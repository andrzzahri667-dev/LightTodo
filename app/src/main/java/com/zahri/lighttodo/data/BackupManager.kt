package com.zahri.lighttodo.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.room.withTransaction
import com.zahri.lighttodo.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 自动备份 & 恢复管理器。
 *
 * 职责：
 *  - 监听数据变化，3s 防抖后自动写备份到 app-external + Documents/LightTodo 迁移目录
 *  - 启动时若数据库为空，尝试从备份恢复
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val repository: Repository,
    private val prefs: UserPrefs,
    private val scope: CoroutineScope
) {
    private val fileName = "lighttodo-auto-backup.json"
    private val portableBackupStore = PortableBackupStore(context)

    suspend fun buildBackupBundle(): BackupBundle {
        val tags = db.tagDao().listAll()
        val todos = db.todoDao().listAll()
        val notes = db.noteDao().listAll()
        return BackupDtoMapper.buildBundle(tags, todos, notes)
    }

    suspend fun restoreFromBundle(bundle: BackupBundle) {
        val entities = BackupDtoMapper.toEntities(bundle)
        val oldTodoIds = db.withTransaction {
            val oldTodoIds = db.todoDao().listAll().map { it.id }
            db.noteDao().deleteAll()
            db.todoDao().deleteAll()
            db.tagDao().deleteAll()
            db.tagDao().upsertAll(entities.tags)
            db.todoDao().upsertAll(entities.todos)
            db.noteDao().upsertAll(entities.notes)
            oldTodoIds
        }
        oldTodoIds.forEach { id -> ReminderScheduler.cancel(context, id) }
        repository.rescheduleAllAlarms()
    }

    fun startAutoBackup() {
        scope.launch(Dispatchers.IO) {
            combine(
                db.todoDao().observeAll(),
                db.tagDao().observeAll(),
                db.noteDao().observeAll(),
                prefs.flow
            ) { todos, tags, notes, settings ->
                AutoBackupSnapshot(todos = todos, tags = tags, notes = notes, settings = settings)
            }
                .collectLatest { snapshot ->
                    if (
                        snapshot.todos.isEmpty() &&
                        snapshot.tags.isEmpty() &&
                        snapshot.notes.isEmpty()
                    ) {
                        return@collectLatest
                    }
                    delay(3000)
                    runCatching {
                        val bundle = BackupDtoMapper.buildBundle(
                            tags = snapshot.tags,
                            todos = snapshot.todos,
                            notes = snapshot.notes
                        )
                        writeBackup(bundle, snapshot.settings)
                    }
                }
        }
    }

    fun restoreIfEmpty() {
        scope.launch(Dispatchers.IO) {
            // 只有 todos 和 notes 都为空时才认为"数据库为空，需要恢复"。
            // 这样删除单一类型（比如清空所有笔记）不会触发备份恢复把它们再写回来。
            val todosEmpty = db.todoDao().listAll().isEmpty()
            val notesEmpty = db.noteDao().listAll().isEmpty()
            if (!todosEmpty || !notesEmpty) return@launch

            readFromAppExternal()?.let { text ->
                if (restoreJsonBackup(text)) return@launch
            }

            // Best-effort Documents/LightTodo restore: Android 10+ only exposes files still
            // readable to this installed app instance, so failure must fall back to legacy JSON.
            val portableBackup = portableBackupStore.read()
            if (portableBackup != null && restoreBestEffortPortableBackup(portableBackup)) {
                return@launch
            }

            readFromPublicDownloads()?.let { text ->
                restoreJsonBackup(text)
            }
        }
    }

    suspend fun restorePortableFromTree(uri: Uri): Int? {
        val portableBackup = portableBackupStore.readFromTree(uri) ?: return null
        restorePortableBackup(portableBackup)
        return portableBackup.bundle.todos.size + portableBackup.bundle.notes.size
    }

    private suspend fun writeBackup(bundle: BackupBundle, settings: UserPrefs.Snapshot) {
        val json = Json { prettyPrint = true; encodeDefaults = true }
        val bytes = json.encodeToString(bundle).toByteArray()
        writeToAppExternal(bytes)
        portableBackupStore.write(bundle, settings)
    }

    private fun writeToAppExternal(bytes: ByteArray) {
        runCatching {
            val dir = context.getExternalFilesDir(null) ?: return
            File(dir, fileName).writeBytes(bytes)
        }
    }

    private fun readFromAppExternal(): String? = runCatching {
        val dir = context.getExternalFilesDir(null) ?: return null
        val f = File(dir, fileName)
        if (f.exists() && BackupReadPolicy.canReadBackupSize(f.length())) f.readText() else null
    }.getOrNull()

    private fun readFromPublicDownloads(): String? =
        when (BackupStoragePolicy.publicDownloadsMode()) {
            BackupStoragePolicy.PublicDownloadsMode.MediaStore -> readFromDownloadsMediaStore()
            BackupStoragePolicy.PublicDownloadsMode.LegacyDirectPath -> readFromDirectPath()
        }

    private fun readFromDownloadsMediaStore(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val entry = findDownloadsMediaStoreEntry() ?: return null
            if (!BackupReadPolicy.canReadBackupSize(entry.sizeBytes)) return null
            context.contentResolver.openInputStream(entry.uri)?.use {
                it.readUtf8WithLimit()
            }
        }.getOrNull()
    }

    private fun findDownloadsMediaStoreEntry(): BackupMediaStoreEntry? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.MediaColumns.SIZE)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val args = arrayOf(fileName)
        val sort = "${MediaStore.Downloads.DATE_MODIFIED} DESC"
        return context.contentResolver.query(collection, projection, selection, args, sort)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val id = cursor.getLong(0)
            BackupMediaStoreEntry(
                uri = Uri.withAppendedPath(collection, id.toString()),
                sizeBytes = if (cursor.isNull(1)) 0L else cursor.getLong(1)
            )
        }
    }

    private fun readFromDirectPath(): String? = runCatching {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (
            file.exists() &&
            file.canRead() &&
            BackupReadPolicy.canReadBackupSize(file.length())
        ) {
            file.readText()
        } else {
            null
        }
    }.getOrNull()

    private fun java.io.InputStream.readUtf8WithLimit(): String? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            total += read
            if (!BackupReadPolicy.canReadBackupSize(total)) return null
            out.write(buffer, 0, read)
        }
        return out.toString(Charsets.UTF_8.name())
    }

    private data class BackupMediaStoreEntry(
        val uri: Uri,
        val sizeBytes: Long
    )

    private data class AutoBackupSnapshot(
        val todos: List<TodoEntity>,
        val tags: List<TagEntity>,
        val notes: List<NoteEntity>,
        val settings: UserPrefs.Snapshot
    )

    private suspend fun restoreJsonBackup(text: String): Boolean =
        runCatching {
            val bundle = Json { ignoreUnknownKeys = true }
                .decodeFromString(BackupBundle.serializer(), text)
            restoreFromBundle(bundle)
        }.onFailure {
            Log.w("BackupManager", "Auto restore failed", it)
        }.isSuccess

    private suspend fun restoreBestEffortPortableBackup(
        portableBackup: PortableBackupStore.PortableRestore
    ): Boolean =
        runCatching {
            restorePortableBackup(portableBackup)
        }.onFailure {
            Log.w("BackupManager", "Best-effort Documents/LightTodo restore failed", it)
        }.isSuccess

    private suspend fun restorePortableBackup(portableBackup: PortableBackupStore.PortableRestore) {
        restoreFromBundle(portableBackup.bundle)
        portableBackup.settings?.let { prefs.restore(it) }
    }
}

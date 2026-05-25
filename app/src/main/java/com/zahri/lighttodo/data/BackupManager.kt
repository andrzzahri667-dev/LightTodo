package com.zahri.lighttodo.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.room.withTransaction
import com.zahri.lighttodo.BuildConfig
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
 *  - 监听数据变化，3s 防抖后自动写备份到 app-external + Downloads
 *  - 启动时若数据库为空，尝试从备份恢复
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val repository: Repository,
    private val scope: CoroutineScope
) {
    private val fileName = BuildConfig.BACKUP_FILE_NAME

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
                db.noteDao().observeAll()
            ) { todos, tags, notes -> Triple(todos, tags, notes) }
                .collectLatest { (todos, tags, notes) ->
                    if (todos.isEmpty() && tags.isEmpty() && notes.isEmpty()) return@collectLatest
                    delay(3000)
                    runCatching {
                        val bundle = BackupDtoMapper.buildBundle(tags, todos, notes)
                        val json = Json { prettyPrint = true; encodeDefaults = true }
                        writeBackupFile(json.encodeToString(bundle))
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
            val text = readBackupFile() ?: return@launch
            runCatching {
                val bundle = Json { ignoreUnknownKeys = true }
                    .decodeFromString(BackupBundle.serializer(), text)
                restoreFromBundle(bundle)
            }.onFailure {
                Log.w("BackupManager", "Auto restore failed", it)
            }
        }
    }

    private fun writeBackupFile(content: String) {
        val bytes = content.toByteArray()
        writeToAppExternal(bytes)
        writeToPublicDownloads(bytes)
    }

    private fun writeToAppExternal(bytes: ByteArray) {
        runCatching {
            val dir = context.getExternalFilesDir(null) ?: return
            File(dir, fileName).writeBytes(bytes)
        }
    }

    private fun writeToPublicDownloads(bytes: ByteArray) {
        when (BackupStoragePolicy.publicDownloadsMode()) {
            BackupStoragePolicy.PublicDownloadsMode.MediaStore -> writeToDownloadsMediaStore(bytes)
            BackupStoragePolicy.PublicDownloadsMode.LegacyDirectPath -> writeToDownloadsDirect(bytes)
        }
    }

    private fun writeToDownloadsMediaStore(bytes: ByteArray) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        runCatching {
            val resolver = context.contentResolver
            val uri = findDownloadsMediaStoreUri() ?: resolver.insert(
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            ) ?: return

            resolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }.also { resolver.update(uri, it, null, null) }
        }
    }

    private fun writeToDownloadsDirect(bytes: ByteArray) {
        runCatching {
            val dir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            dir.mkdirs()
            File(dir, fileName).writeBytes(bytes)
        }
    }

    private fun readBackupFile(): String? {
        readFromAppExternal()?.let { return it }
        readFromPublicDownloads()?.let { return it }
        return null
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

    private fun findDownloadsMediaStoreUri(): Uri? {
        return findDownloadsMediaStoreEntry()?.uri
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
}

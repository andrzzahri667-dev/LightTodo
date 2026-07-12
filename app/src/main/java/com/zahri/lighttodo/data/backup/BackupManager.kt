package com.zahri.lighttodo.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.zahri.lighttodo.data.local.AppDatabase
import com.zahri.lighttodo.data.local.NoteEntity
import com.zahri.lighttodo.data.local.TagEntity
import com.zahri.lighttodo.data.local.TodoEntity
import com.zahri.lighttodo.data.prefs.UserPrefs
import com.zahri.lighttodo.domain.backup.BackupBundle
import com.zahri.lighttodo.usecase.file.FileGateway
import com.zahri.lighttodo.usecase.note.NoteAttachmentGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    private val prefs: UserPrefs,
    private val scope: CoroutineScope,
    private val fileGateway: FileGateway,
    private val noteAttachmentGateway: NoteAttachmentGateway,
    private val cancelTodoReminder: (Long) -> Unit,
    private val rescheduleTodoReminders: suspend () -> Unit
) {
    private val fileName = "lighttodo-auto-backup.json"
    private val portableBackupStore = PortableBackupStore(fileGateway, noteAttachmentGateway)

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
        oldTodoIds.forEach(cancelTodoReminder)
        rescheduleTodoReminders()
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
        if (fileGateway.hasPersistedDocumentTreeWritePermission(uri)) {
            prefs.setPortableBackupTreeUri(uri.toString())
        }
        return portableBackup.bundle.todos.size + portableBackup.bundle.notes.size
    }

    private suspend fun writeBackup(bundle: BackupBundle, settings: UserPrefs.Snapshot) {
        val json = Json { prettyPrint = true; encodeDefaults = true }
        val bytes = json.encodeToString(bundle).toByteArray()
        writeToAppExternal(bytes)
        val treeUri = prefs.portableBackupTreeUri()
        val parsedTreeUri = treeUri?.let(Uri::parse)
        val treeUriHasWritePermission =
            parsedTreeUri?.let(fileGateway::hasPersistedDocumentTreeWritePermission) ?: false
        if (treeUri != null && !treeUriHasWritePermission) {
            prefs.clearPortableBackupTreeUri()
        }
        when (
            PortableBackupRoutingPolicy.destination(
                treeUri = treeUri,
                treeUriHasWritePermission = treeUriHasWritePermission,
                publicDocumentsMode = BackupStoragePolicy.publicDocumentsMode()
            )
        ) {
            PortableBackupRoutingPolicy.Destination.DocumentTree ->
                portableBackupStore.writeToTree(requireNotNull(parsedTreeUri), bundle, settings)
            PortableBackupRoutingPolicy.Destination.LegacyPublicDocuments ->
                portableBackupStore.write(bundle, settings)
            PortableBackupRoutingPolicy.Destination.SkipPublicDocuments -> Unit
        }
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
            BackupStoragePolicy.PublicDownloadsMode.MediaStore -> readFromPublicDownloadsGateway()
            BackupStoragePolicy.PublicDownloadsMode.LegacyDirectPath -> readFromDirectPath()
        }

    private fun readFromPublicDownloadsGateway(): String? {
        return runCatching {
            val entry = fileGateway.findPublicDownloadFile(fileName) ?: return null
            if (!BackupReadPolicy.canReadBackupSize(entry.sizeBytes ?: 0L)) return null
            val bytes = fileGateway.readPublicDownloadFile(fileName) ?: return null
            if (!BackupReadPolicy.canReadBackupSize(bytes.size.toLong())) return null
            bytes.toString(Charsets.UTF_8)
        }.getOrNull()
    }

    private fun readFromDirectPath(): String? = runCatching {
        val file = fileGateway.publicDownloadFile(fileName)
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

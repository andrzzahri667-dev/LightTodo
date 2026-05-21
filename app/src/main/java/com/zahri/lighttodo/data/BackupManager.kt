package com.zahri.lighttodo.data

import android.content.Context
import androidx.room.withTransaction
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
 *  - 监听数据变化，3s 防抖后自动写备份到 app-external + Downloads
 *  - 启动时若数据库为空，尝试从备份恢复
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val repository: Repository,
    private val scope: CoroutineScope
) {
    private val fileName = "lighttodo-auto-backup.json"

    suspend fun buildBackupBundle(): BackupBundle {
        val tags = db.tagDao().listAll()
        val todos = db.todoDao().listAll()
        val notes = db.noteDao().listAll()
        return BackupDtoMapper.buildBundle(tags, todos, notes)
    }

    suspend fun restoreFromBundle(bundle: BackupBundle) {
        val entities = BackupDtoMapper.toEntities(bundle)
        db.withTransaction {
            db.noteDao().deleteAll()
            db.todoDao().deleteAll()
            db.tagDao().deleteAll()
            db.tagDao().upsertAll(entities.tags)
            db.todoDao().upsertAll(entities.todos)
            db.noteDao().upsertAll(entities.notes)
        }
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
            }
        }
    }

    private fun writeBackupFile(content: String) {
        val bytes = content.toByteArray()
        writeToAppExternal(bytes)
        writeToDownloadsDirect(bytes)
    }

    private fun writeToAppExternal(bytes: ByteArray) {
        runCatching {
            val dir = context.getExternalFilesDir(null) ?: return
            File(dir, fileName).writeBytes(bytes)
        }
    }

    private fun writeToDownloadsDirect(bytes: ByteArray) {
        runCatching {
            val dir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            dir.mkdirs()
            File(dir, fileName).writeBytes(bytes)
        }
    }

    private fun readBackupFile(): String? {
        readFromAppExternal()?.let { return it }
        readFromDirectPath()?.let { return it }
        return null
    }

    private fun readFromAppExternal(): String? = runCatching {
        val dir = context.getExternalFilesDir(null) ?: return null
        val f = File(dir, fileName)
        if (f.exists()) f.readText() else null
    }.getOrNull()

    private fun readFromDirectPath(): String? = runCatching {
        val file = File(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (file.exists() && file.canRead()) file.readText() else null
    }.getOrNull()
}

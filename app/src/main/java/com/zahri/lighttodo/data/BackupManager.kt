package com.zahri.lighttodo.data

import android.content.Context
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

    fun startAutoBackup() {
        scope.launch(Dispatchers.IO) {
            combine(db.todoDao().observeAll(), db.tagDao().observeAll()) { todos, tags -> todos to tags }
                .collectLatest { (todos, tags) ->
                    if (todos.isEmpty() && tags.isEmpty()) return@collectLatest
                    delay(3000)
                    runCatching {
                        val bundle = BackupBundle(
                            version = 1,
                            tags = tags.map { BackupTag(it.id, it.name, it.sortOrder) },
                            todos = todos.map { it.toBackupTodo() }
                        )
                        val json = Json { prettyPrint = true; encodeDefaults = true }
                        writeBackupFile(json.encodeToString(bundle))
                    }
                }
        }
    }

    fun restoreIfEmpty() {
        scope.launch(Dispatchers.IO) {
            if (db.todoDao().listAll().isNotEmpty()) return@launch
            val text = readBackupFile() ?: return@launch
            runCatching {
                val bundle = Json { ignoreUnknownKeys = true }
                    .decodeFromString(BackupBundle.serializer(), text)
                db.tagDao().upsertAll(bundle.tags.map { TagEntity(it.id, it.name, it.sortOrder) })
                db.todoDao().upsertAll(bundle.todos.map { it.toEntity() })
                repository.rescheduleAllAlarms()
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

private fun TodoEntity.toBackupTodo() = BackupTodo(
    id = id, title = title, note = note, date = date, dateMillis = dateMillis,
    startHour = startHour, startMinute = startMinute,
    deadlineHour = deadlineHour, deadlineMinute = deadlineMinute,
    remindStartAtMillis = remindStartAtMillis, remindAtMillis = remindAtMillis,
    customRemindHoursBefore = customRemindHoursBefore,
    tagId = tagId, done = done, doneAtMillis = doneAtMillis, createdAtMillis = createdAtMillis
)

private fun BackupTodo.toEntity() = TodoEntity(
    id = id, title = title, note = note, date = date, dateMillis = dateMillis,
    startHour = startHour, startMinute = startMinute,
    deadlineHour = deadlineHour, deadlineMinute = deadlineMinute,
    remindStartAtMillis = remindStartAtMillis, remindAtMillis = remindAtMillis,
    customRemindHoursBefore = customRemindHoursBefore,
    tagId = tagId, done = done, doneAtMillis = doneAtMillis, createdAtMillis = createdAtMillis,
    calendarEventId = null
)

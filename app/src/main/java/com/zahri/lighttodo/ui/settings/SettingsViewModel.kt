package com.zahri.lighttodo.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahri.lighttodo.App
import com.zahri.lighttodo.calendar.CalendarSync
import com.zahri.lighttodo.data.BackupBundle
import com.zahri.lighttodo.data.BackupTag
import com.zahri.lighttodo.data.BackupTodo
import com.zahri.lighttodo.data.TagEntity
import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.data.UserPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsViewModel : ViewModel() {

    private val app = App.instance
    private val prefs = app.prefs
    private val db = app.db
    private val repo = app.repository

    val state: StateFlow<UserPrefs.Snapshot> = prefs.flow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPrefs.Snapshot()
    )

    fun setDefaultRemind(hour: Int, minute: Int) =
        viewModelScope.launch { prefs.setDefaultRemind(hour, minute) }

    fun setDefaultHoursBefore(hours: Int) =
        viewModelScope.launch { prefs.setDefaultHoursBefore(hours) }

    fun setCalendarSyncEnabled(enabled: Boolean) =
        viewModelScope.launch { prefs.setCalendarSyncEnabled(enabled) }

    fun setCalendarAccount(name: String) =
        viewModelScope.launch { prefs.setCalendarAccountName(name) }

    fun setQuickAddNotif(enabled: Boolean) =
        viewModelScope.launch { prefs.setQuickAddNotifEnabled(enabled) }

    fun clearDone(onDone: (String) -> Unit) = viewModelScope.launch {
        repo.clearDone()
        onDone("已清空")
    }

    fun syncCalendarNow(context: Context, onDone: (String) -> Unit) = viewModelScope.launch {
        val n = CalendarSync.runOnce(context)
        onDone(if (n >= 0) "已同步 $n 条日历事件" else "同步失败：缺少权限或未启用")
    }

    fun exportTo(context: Context, uri: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        runCatching {
            val tags = db.tagDao().listAll()
            val todos = db.todoDao().listAll()
            val bundle = BackupBundle(
                version = 1,
                tags = tags.map { BackupTag(it.id, it.name, it.sortOrder) },
                todos = todos.map { it.toBackup() }
            )
            val json = Json { prettyPrint = true; encodeDefaults = true }
            val text = json.encodeToString(bundle)
            context.contentResolver.openOutputStream(uri, "wt")?.use { os ->
                os.write(text.toByteArray(Charsets.UTF_8))
            }
            onDone("导出成功，共 ${todos.size} 条")
        }.onFailure { onDone("导出失败：${it.message}") }
    }

    fun importFrom(context: Context, uri: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: error("无法读取")
            val bundle = Json { ignoreUnknownKeys = true }.decodeFromString(BackupBundle.serializer(), text)
            db.tagDao().deleteAll()
            db.todoDao().deleteAll()
            db.tagDao().upsertAll(bundle.tags.map { TagEntity(it.id, it.name, it.sortOrder) })
            db.todoDao().upsertAll(bundle.todos.map { it.toEntity() })
            repo.rescheduleAllAlarms()
            onDone("导入成功，共 ${bundle.todos.size} 条")
        }.onFailure { onDone("导入失败：${it.message}") }
    }
}

private fun TodoEntity.toBackup() = BackupTodo(
    id = id, title = title, note = note, date = date, dateMillis = dateMillis,
    deadlineHour = deadlineHour, deadlineMinute = deadlineMinute,
    remindAtMillis = remindAtMillis, customRemindHoursBefore = customRemindHoursBefore,
    tagId = tagId, done = done, doneAtMillis = doneAtMillis, createdAtMillis = createdAtMillis
)

private fun BackupTodo.toEntity() = TodoEntity(
    id = id, title = title, note = note, date = date, dateMillis = dateMillis,
    deadlineHour = deadlineHour, deadlineMinute = deadlineMinute,
    remindAtMillis = remindAtMillis, customRemindHoursBefore = customRemindHoursBefore,
    tagId = tagId, done = done, doneAtMillis = doneAtMillis, createdAtMillis = createdAtMillis,
    calendarEventId = null
)

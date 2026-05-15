package com.zahri.lighttodo.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahri.lighttodo.App
import com.zahri.lighttodo.R
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
        onDone(app.getString(R.string.settings_cleared))
    }

    fun syncCalendarNow(context: Context, onDone: (String) -> Unit) = viewModelScope.launch {
        val n = CalendarSync.runOnce(context, force = true)
        onDone(if (n >= 0) context.getString(R.string.settings_sync_success, n) else context.getString(R.string.settings_sync_failed))
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
            onDone(context.getString(R.string.settings_export_success, todos.size))
        }.onFailure { onDone(context.getString(R.string.settings_export_failed, it.message)) }
    }

    fun importFrom(context: Context, uri: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: error(context.getString(R.string.settings_cannot_read))
            val bundle = Json { ignoreUnknownKeys = true }.decodeFromString(BackupBundle.serializer(), text)
            db.tagDao().deleteAll()
            db.todoDao().deleteAll()
            db.tagDao().upsertAll(bundle.tags.map { TagEntity(it.id, it.name, it.sortOrder) })
            db.todoDao().upsertAll(bundle.todos.map { it.toEntity() })
            repo.rescheduleAllAlarms()
            onDone(context.getString(R.string.settings_import_success, bundle.todos.size))
        }.onFailure { onDone(context.getString(R.string.settings_import_failed, it.message)) }
    }
}

private fun TodoEntity.toBackup() = BackupTodo(
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

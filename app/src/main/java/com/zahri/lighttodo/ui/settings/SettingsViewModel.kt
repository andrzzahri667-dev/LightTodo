package com.zahri.lighttodo.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahri.lighttodo.App
import com.zahri.lighttodo.R
import com.zahri.lighttodo.calendar.CalendarSync
import com.zahri.lighttodo.data.BackupBundle
import com.zahri.lighttodo.data.BackupManager
import com.zahri.lighttodo.data.DatabaseSnapshotExporter
import com.zahri.lighttodo.data.Repository
import com.zahri.lighttodo.data.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsViewModel(
    private val app: App = App.instance,
    private val prefs: UserPrefs = app.prefs,
    private val repo: Repository = app.repository,
    private val backupManager: BackupManager = app.backupManager
) : ViewModel() {

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
        val message = withContext(Dispatchers.IO) {
            runCatching {
                val n = CalendarSync.runOnce(context, force = true)
                if (n >= 0) {
                    context.getString(R.string.settings_sync_success, n)
                } else {
                    context.getString(R.string.settings_sync_failed)
                }
            }.getOrElse {
                context.getString(R.string.settings_sync_failed)
            }
        }
        onDone(message)
    }

    fun exportTo(context: Context, uri: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        val message = withContext(Dispatchers.IO) {
            runCatching {
                val bundle = backupManager.buildBackupBundle()
                val json = Json { prettyPrint = true; encodeDefaults = true }
                val text = json.encodeToString(bundle)
                val output = context.contentResolver.openOutputStream(uri, "wt")
                    ?: error(context.getString(R.string.settings_cannot_write))
                output.use { os ->
                    os.write(text.toByteArray(Charsets.UTF_8))
                }
                context.getString(R.string.settings_export_success, bundle.todos.size + bundle.notes.size)
            }.getOrElse { context.getString(R.string.settings_export_failed, it.message) }
        }
        onDone(message)
    }

    fun importFrom(context: Context, uri: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        val message = withContext(Dispatchers.IO) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: error(context.getString(R.string.settings_cannot_read))
                val bundle = Json { ignoreUnknownKeys = true }.decodeFromString(BackupBundle.serializer(), text)
                backupManager.restoreFromBundle(bundle)
                context.getString(R.string.settings_import_success, bundle.todos.size + bundle.notes.size)
            }.getOrElse { context.getString(R.string.settings_import_failed, it.message) }
        }
        onDone(message)
    }

    fun exportDatabaseSnapshot(context: Context, onDone: (String) -> Unit) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                DatabaseSnapshotExporter.export(context, app.db)
            }
        }.onSuccess { dir ->
            onDone(context.getString(R.string.settings_db_snapshot_success, dir.absolutePath))
        }.onFailure {
            onDone(context.getString(R.string.settings_db_snapshot_failed, it.message))
        }
    }
}

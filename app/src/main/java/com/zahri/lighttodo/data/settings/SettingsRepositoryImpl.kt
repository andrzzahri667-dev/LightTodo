package com.zahri.lighttodo.data.settings

import android.content.Context
import android.net.Uri
import com.zahri.lighttodo.data.backup.BackupManager
import com.zahri.lighttodo.data.local.AppDatabase
import com.zahri.lighttodo.data.local.DatabaseSnapshotExporter
import com.zahri.lighttodo.data.prefs.UserPrefs
import com.zahri.lighttodo.domain.backup.BackupBundle
import com.zahri.lighttodo.usecase.settings.CannotReadSettingsFileException
import com.zahri.lighttodo.usecase.settings.CannotWriteSettingsFileException
import com.zahri.lighttodo.usecase.settings.SettingsRepository
import com.zahri.lighttodo.usecase.settings.SettingsSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class SettingsRepositoryImpl(
    context: Context,
    private val prefs: UserPrefs,
    private val backupManager: BackupManager,
    private val db: AppDatabase
) : SettingsRepository {
    private val appContext: Context = context.applicationContext ?: context
    private val prettyJson = Json { prettyPrint = true; encodeDefaults = true }
    private val lenientJson = Json { ignoreUnknownKeys = true }

    override val settingsFlow: Flow<SettingsSnapshot> =
        prefs.flow.map { it.toSettingsSnapshot() }

    override suspend fun setDefaultRemind(hour: Int, minute: Int) {
        prefs.setDefaultRemind(hour, minute)
    }

    override suspend fun setDefaultHoursBefore(hours: Int) {
        prefs.setDefaultHoursBefore(hours)
    }

    override suspend fun setCalendarSyncEnabled(enabled: Boolean) {
        prefs.setCalendarSyncEnabled(enabled)
    }

    override suspend fun setCalendarAccountName(name: String) {
        prefs.setCalendarAccountName(name)
    }

    override suspend fun setQuickAddNotifEnabled(enabled: Boolean) {
        prefs.setQuickAddNotifEnabled(enabled)
    }

    override suspend fun exportBackupTo(uri: Uri): Int {
        val bundle = backupManager.buildBackupBundle()
        val text = prettyJson.encodeToString(bundle)
        val output = appContext.contentResolver.openOutputStream(uri, "wt")
            ?: throw CannotWriteSettingsFileException()
        output.use { os ->
            os.write(text.toByteArray(Charsets.UTF_8))
        }
        return bundle.todos.size + bundle.notes.size
    }

    override suspend fun importBackupFrom(uri: Uri): Int {
        val text = appContext.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: throw CannotReadSettingsFileException()
        val bundle = lenientJson.decodeFromString(BackupBundle.serializer(), text)
        backupManager.restoreFromBundle(bundle)
        return bundle.todos.size + bundle.notes.size
    }

    override suspend fun importPortableFrom(uri: Uri): Int? =
        backupManager.restorePortableFromTree(uri)

    override suspend fun exportDatabaseSnapshot(): File =
        DatabaseSnapshotExporter.export(appContext, db)

    private fun UserPrefs.Snapshot.toSettingsSnapshot(): SettingsSnapshot =
        SettingsSnapshot(
            defaultRemindHour = defaultRemindHour,
            defaultRemindMinute = defaultRemindMinute,
            defaultHoursBefore = defaultHoursBefore,
            calendarSyncEnabled = calendarSyncEnabled,
            calendarAccountName = calendarAccountName,
            quickAddNotifEnabled = quickAddNotifEnabled
        )
}

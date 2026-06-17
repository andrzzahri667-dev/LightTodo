package com.zahri.lighttodo.usecase.settings

import android.net.Uri
import com.zahri.lighttodo.usecase.calendar.SyncCalendarUseCase
import kotlinx.coroutines.flow.Flow
import java.io.File

data class SettingsSnapshot(
    val defaultRemindHour: Int = 9,
    val defaultRemindMinute: Int = 0,
    val defaultHoursBefore: Int = 2,
    val calendarSyncEnabled: Boolean = false,
    val calendarAccountName: String = "",
    val quickAddNotifEnabled: Boolean = false
)

interface SettingsRepository {
    val settingsFlow: Flow<SettingsSnapshot>
    suspend fun setDefaultRemind(hour: Int, minute: Int)
    suspend fun setDefaultHoursBefore(hours: Int)
    suspend fun setCalendarSyncEnabled(enabled: Boolean)
    suspend fun setCalendarAccountName(name: String)
    suspend fun setQuickAddNotifEnabled(enabled: Boolean)
    suspend fun exportBackupTo(uri: Uri): Int
    suspend fun importBackupFrom(uri: Uri): Int
    suspend fun importPortableFrom(uri: Uri): Int?
    suspend fun exportDatabaseSnapshot(): File
}

class CannotReadSettingsFileException : IllegalStateException()

class CannotWriteSettingsFileException : IllegalStateException()

class ObserveSettingsUseCase(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<SettingsSnapshot> = repository.settingsFlow

    fun defaultSnapshot(): SettingsSnapshot = SettingsSnapshot()
}

class UpdateSettingsUseCase(
    private val repository: SettingsRepository
) {
    suspend fun setDefaultRemind(hour: Int, minute: Int) {
        repository.setDefaultRemind(hour, minute)
    }

    suspend fun setDefaultHoursBefore(hours: Int) {
        repository.setDefaultHoursBefore(hours)
    }

    suspend fun setCalendarSyncEnabled(enabled: Boolean) {
        repository.setCalendarSyncEnabled(enabled)
    }

    suspend fun setCalendarAccount(name: String) {
        repository.setCalendarAccountName(name)
    }

    suspend fun setQuickAddNotif(enabled: Boolean) {
        repository.setQuickAddNotifEnabled(enabled)
    }
}

class SyncSettingsCalendarUseCase(
    private val syncCalendar: SyncCalendarUseCase
) {
    suspend operator fun invoke(noTitleFallback: String): Int =
        syncCalendar(force = true, noTitleFallback = noTitleFallback)
}

class ExportSettingsBackupUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(uri: Uri): Int =
        repository.exportBackupTo(uri)
}

class ImportSettingsBackupUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(uri: Uri): Int =
        repository.importBackupFrom(uri)
}

class ImportPortableSettingsBackupUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(uri: Uri): Int? =
        repository.importPortableFrom(uri)
}

class ExportSettingsDatabaseSnapshotUseCase(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(): File =
        repository.exportDatabaseSnapshot()
}

data class SettingsUseCases(
    val observeSettings: ObserveSettingsUseCase,
    val updateSettings: UpdateSettingsUseCase,
    val syncCalendar: SyncSettingsCalendarUseCase,
    val exportBackup: ExportSettingsBackupUseCase,
    val importBackup: ImportSettingsBackupUseCase,
    val importPortableBackup: ImportPortableSettingsBackupUseCase,
    val exportDatabaseSnapshot: ExportSettingsDatabaseSnapshotUseCase
)

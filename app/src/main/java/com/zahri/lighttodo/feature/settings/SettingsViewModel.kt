package com.zahri.lighttodo.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahri.lighttodo.R
import com.zahri.lighttodo.usecase.settings.CannotReadSettingsFileException
import com.zahri.lighttodo.usecase.settings.CannotWriteSettingsFileException
import com.zahri.lighttodo.usecase.settings.SettingsUseCases
import com.zahri.lighttodo.usecase.todo.DeleteTodoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val appContext: Context,
    private val settingsUseCases: SettingsUseCases,
    private val deleteTodo: DeleteTodoUseCase
) : ViewModel() {

    val state = settingsUseCases.observeSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = settingsUseCases.observeSettings.defaultSnapshot()
    )

    fun setDefaultRemind(hour: Int, minute: Int) =
        viewModelScope.launch { settingsUseCases.updateSettings.setDefaultRemind(hour, minute) }

    fun setDefaultHoursBefore(hours: Int) =
        viewModelScope.launch { settingsUseCases.updateSettings.setDefaultHoursBefore(hours) }

    fun setCalendarSyncEnabled(enabled: Boolean) =
        viewModelScope.launch { settingsUseCases.updateSettings.setCalendarSyncEnabled(enabled) }

    fun setCalendarAccount(name: String) =
        viewModelScope.launch { settingsUseCases.updateSettings.setCalendarAccount(name) }

    fun setQuickAddNotif(enabled: Boolean) =
        viewModelScope.launch { settingsUseCases.updateSettings.setQuickAddNotif(enabled) }

    fun clearDone(onDone: (String) -> Unit) = viewModelScope.launch {
        deleteTodo.clearDone()
        onDone(appContext.getString(R.string.settings_cleared))
    }

    fun syncCalendarNow(context: Context, onDone: (String) -> Unit) = viewModelScope.launch {
        val message = withContext(Dispatchers.IO) {
            runCatching {
                val n = settingsUseCases.syncCalendar(
                    noTitleFallback = context.getString(R.string.calendar_no_title)
                )
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
                val count = settingsUseCases.exportBackup(uri)
                context.getString(R.string.settings_export_success, count)
            }.getOrElse {
                context.getString(R.string.settings_export_failed, settingsErrorMessage(context, it))
            }
        }
        onDone(message)
    }

    fun importFrom(context: Context, uri: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        val message = withContext(Dispatchers.IO) {
            runCatching {
                val count = settingsUseCases.importBackup(uri)
                context.getString(R.string.settings_import_success, count)
            }.getOrElse {
                context.getString(R.string.settings_import_failed, settingsErrorMessage(context, it))
            }
        }
        onDone(message)
    }

    fun importPortableFrom(context: Context, uri: Uri, onDone: (String) -> Unit) = viewModelScope.launch {
        val message = withContext(Dispatchers.IO) {
            runCatching {
                val count = settingsUseCases.importPortableBackup(uri)
                    ?: error(context.getString(R.string.settings_cannot_read))
                context.getString(R.string.settings_portable_import_success, count)
            }.getOrElse {
                context.getString(R.string.settings_portable_import_failed, settingsErrorMessage(context, it))
            }
        }
        onDone(message)
    }

    fun exportDatabaseSnapshot(context: Context, onDone: (String) -> Unit) = viewModelScope.launch {
        runCatching {
            withContext(Dispatchers.IO) {
                settingsUseCases.exportDatabaseSnapshot()
            }
        }.onSuccess { dir ->
            onDone(context.getString(R.string.settings_db_snapshot_success, dir.absolutePath))
        }.onFailure {
            onDone(context.getString(R.string.settings_db_snapshot_failed, settingsErrorMessage(context, it)))
        }
    }

    private fun settingsErrorMessage(context: Context, error: Throwable): String =
        when (error) {
            is CannotReadSettingsFileException -> context.getString(R.string.settings_cannot_read)
            is CannotWriteSettingsFileException -> context.getString(R.string.settings_cannot_write)
            else -> error.message.orEmpty()
        }
}

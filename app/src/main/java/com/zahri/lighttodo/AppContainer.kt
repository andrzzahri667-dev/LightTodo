package com.zahri.lighttodo

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zahri.lighttodo.integration.calendar.AndroidCalendarGateway
import com.zahri.lighttodo.integration.calendar.AndroidCalendarSyncGateway
import com.zahri.lighttodo.data.prefs.UserPrefs
import com.zahri.lighttodo.data.backup.BackupManager
import com.zahri.lighttodo.data.calendar.CalendarSyncRepositoryImpl
import com.zahri.lighttodo.data.local.AppDatabase
import com.zahri.lighttodo.data.note.NoteRepositoryImpl
import com.zahri.lighttodo.data.note.NoteAttachmentStore
import com.zahri.lighttodo.data.settings.SettingsRepositoryImpl
import com.zahri.lighttodo.data.todo.TodoRepositoryImpl
import com.zahri.lighttodo.integration.file.AndroidFileGateway
import com.zahri.lighttodo.integration.reminder.AndroidReminderGateway
import com.zahri.lighttodo.feature.quickadd.QuickAddViewModel
import com.zahri.lighttodo.feature.reminder.ReminderViewModel
import com.zahri.lighttodo.feature.todoedit.EditViewModel
import com.zahri.lighttodo.feature.home.HomeViewModel
import com.zahri.lighttodo.feature.noteeditor.NoteEditViewModel
import com.zahri.lighttodo.feature.settings.SettingsViewModel
import com.zahri.lighttodo.usecase.boot.HandleBootCompletedUseCase
import com.zahri.lighttodo.usecase.calendar.SyncCalendarUseCase
import com.zahri.lighttodo.usecase.note.CopyNoteImageFromUriUseCase
import com.zahri.lighttodo.usecase.note.CreateNoteAudioFileUseCase
import com.zahri.lighttodo.usecase.note.CreateNoteImageFileUseCase
import com.zahri.lighttodo.usecase.note.DeleteNoteUseCase
import com.zahri.lighttodo.usecase.note.LoadNoteUseCase
import com.zahri.lighttodo.usecase.note.NoteAudioRefUseCase
import com.zahri.lighttodo.usecase.note.NoteFileProviderUriUseCase
import com.zahri.lighttodo.usecase.note.NoteImageRefUseCase
import com.zahri.lighttodo.usecase.note.NoteUseCases
import com.zahri.lighttodo.usecase.note.ObserveNotesUseCase
import com.zahri.lighttodo.usecase.note.ResolveNoteAttachmentUseCase
import com.zahri.lighttodo.usecase.note.SaveNoteUseCase
import com.zahri.lighttodo.usecase.settings.ExportSettingsBackupUseCase
import com.zahri.lighttodo.usecase.settings.ExportSettingsDatabaseSnapshotUseCase
import com.zahri.lighttodo.usecase.settings.ImportPortableSettingsBackupUseCase
import com.zahri.lighttodo.usecase.settings.ImportSettingsBackupUseCase
import com.zahri.lighttodo.usecase.settings.ObserveSettingsUseCase
import com.zahri.lighttodo.usecase.settings.SettingsUseCases
import com.zahri.lighttodo.usecase.settings.SyncSettingsCalendarUseCase
import com.zahri.lighttodo.usecase.settings.UpdateSettingsUseCase
import com.zahri.lighttodo.usecase.todo.CompleteTodoUseCase
import com.zahri.lighttodo.usecase.todo.DeleteTodoUseCase
import com.zahri.lighttodo.usecase.todo.LoadReminderDialogUseCase
import com.zahri.lighttodo.usecase.todo.LoadReminderNotificationUseCase
import com.zahri.lighttodo.usecase.todo.LoadTodoEditUseCase
import com.zahri.lighttodo.usecase.todo.LoadWidgetCompletionAnimationUseCase
import com.zahri.lighttodo.usecase.todo.LoadWidgetTodosUseCase
import com.zahri.lighttodo.usecase.todo.ObserveHomeUseCase
import com.zahri.lighttodo.usecase.todo.RescheduleRemindersUseCase
import com.zahri.lighttodo.usecase.todo.SaveTodoUseCase
import com.zahri.lighttodo.usecase.todo.TodoUseCases
import com.zahri.lighttodo.usecase.todo.UpdateHomePreferencesUseCase
import com.zahri.lighttodo.integration.widget.AndroidWidgetUpdater
import kotlinx.coroutines.CoroutineScope

class AppContainer(
    private val app: App,
    private val appScope: CoroutineScope
) {
    val appContext: Context = app.applicationContext ?: app
    val db by lazy { AppDatabase.get(appContext) }
    val prefs by lazy { UserPrefs(appContext) }
    private val todoRepository by lazy {
        TodoRepositoryImpl(
            todoDao = db.todoDao(),
            tagDao = db.tagDao(),
            prefs = prefs
        )
    }
    private val calendarSyncRepository by lazy {
        CalendarSyncRepositoryImpl(todoDao = db.todoDao())
    }
    private val fileGateway by lazy { AndroidFileGateway(appContext) }
    private val noteAttachmentGateway by lazy {
        NoteAttachmentStore(
            filesDir = appContext.filesDir,
            fileGateway = fileGateway
        )
    }
    private val noteRepository by lazy {
        NoteRepositoryImpl(
            noteDao = db.noteDao(),
            attachmentGateway = noteAttachmentGateway
        )
    }
    private val reminderGateway by lazy { AndroidReminderGateway(appContext) }
    private val calendarGateway by lazy { AndroidCalendarGateway(appContext) }
    private val calendarSyncGateway by lazy { AndroidCalendarSyncGateway(appContext) }
    private val widgetUpdater by lazy { AndroidWidgetUpdater(appContext) }
    val backupManager by lazy {
        BackupManager(
            context = appContext,
            db = db,
            prefs = prefs,
            scope = appScope,
            fileGateway = fileGateway,
            noteAttachmentGateway = noteAttachmentGateway,
            cancelTodoReminder = reminderGateway::cancel,
            rescheduleTodoReminders = { todoUseCases.rescheduleReminders() }
        )
    }
    private val settingsRepository by lazy {
        SettingsRepositoryImpl(
            context = appContext,
            prefs = prefs,
            backupManager = backupManager,
            db = db
        )
    }
    val todoUseCases by lazy {
        TodoUseCases(
            observeHome = ObserveHomeUseCase(todoRepository),
            loadTodoEdit = LoadTodoEditUseCase(todoRepository),
            updateHomePreferences = UpdateHomePreferencesUseCase(prefs),
            saveTodo = SaveTodoUseCase(todoRepository, reminderGateway, calendarGateway, widgetUpdater),
            completeTodo = CompleteTodoUseCase(todoRepository, reminderGateway, calendarGateway, widgetUpdater),
            deleteTodo = DeleteTodoUseCase(todoRepository, reminderGateway, calendarGateway, widgetUpdater),
            rescheduleReminders = RescheduleRemindersUseCase(todoRepository, reminderGateway),
            loadReminderDialog = LoadReminderDialogUseCase(todoRepository),
            loadReminderNotification = LoadReminderNotificationUseCase(todoRepository),
            loadWidgetCompletionAnimation = LoadWidgetCompletionAnimationUseCase(todoRepository),
            loadWidgetTodos = LoadWidgetTodosUseCase(todoRepository)
        )
    }
    val noteUseCases by lazy {
        NoteUseCases(
            observeNotes = ObserveNotesUseCase(noteRepository),
            loadNote = LoadNoteUseCase(noteRepository),
            saveNote = SaveNoteUseCase(noteRepository),
            deleteNote = DeleteNoteUseCase(noteRepository),
            createImageFile = CreateNoteImageFileUseCase(noteAttachmentGateway),
            createAudioFile = CreateNoteAudioFileUseCase(noteAttachmentGateway),
            fileProviderUri = NoteFileProviderUriUseCase(noteAttachmentGateway),
            copyImageFromUri = CopyNoteImageFromUriUseCase(noteAttachmentGateway),
            imageRef = NoteImageRefUseCase(noteAttachmentGateway),
            audioRef = NoteAudioRefUseCase(noteAttachmentGateway),
            resolveAttachment = ResolveNoteAttachmentUseCase(noteAttachmentGateway)
        )
    }
    val handleBootCompleted by lazy {
        HandleBootCompletedUseCase(
            prefs = prefs,
            rescheduleReminders = todoUseCases.rescheduleReminders
        )
    }
    val syncCalendar by lazy {
        SyncCalendarUseCase(
            prefs = prefs,
            repository = calendarSyncRepository,
            calendarSyncGateway = calendarSyncGateway,
            widgetUpdater = widgetUpdater,
            reminderGateway = reminderGateway
        )
    }
    val settingsUseCases by lazy {
        SettingsUseCases(
            observeSettings = ObserveSettingsUseCase(settingsRepository),
            updateSettings = UpdateSettingsUseCase(settingsRepository),
            syncCalendar = SyncSettingsCalendarUseCase(syncCalendar),
            exportBackup = ExportSettingsBackupUseCase(settingsRepository),
            importBackup = ImportSettingsBackupUseCase(settingsRepository),
            importPortableBackup = ImportPortableSettingsBackupUseCase(settingsRepository),
            exportDatabaseSnapshot = ExportSettingsDatabaseSnapshotUseCase(settingsRepository)
        )
    }
    val viewModelFactory: ViewModelProvider.Factory by lazy { LightTodoViewModelFactory(this) }
}

class LightTodoViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(
                    context = container.appContext,
                    observeHome = container.todoUseCases.observeHome,
                    completeTodo = container.todoUseCases.completeTodo,
                    deleteTodo = container.todoUseCases.deleteTodo,
                    updateHomePreferences = container.todoUseCases.updateHomePreferences,
                    observeNotes = container.noteUseCases.observeNotes,
                    deleteNote = container.noteUseCases.deleteNote
                )
            modelClass.isAssignableFrom(EditViewModel::class.java) ->
                EditViewModel(
                    loadTodoEdit = container.todoUseCases.loadTodoEdit,
                    saveTodo = container.todoUseCases.saveTodo,
                    deleteTodo = container.todoUseCases.deleteTodo
                )
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(
                    appContext = container.appContext,
                    settingsUseCases = container.settingsUseCases,
                    deleteTodo = container.todoUseCases.deleteTodo
                )
            modelClass.isAssignableFrom(NoteEditViewModel::class.java) ->
                NoteEditViewModel(
                    noteUseCases = container.noteUseCases
                )
            modelClass.isAssignableFrom(QuickAddViewModel::class.java) ->
                QuickAddViewModel(
                    saveTodo = container.todoUseCases.saveTodo
                )
            modelClass.isAssignableFrom(ReminderViewModel::class.java) ->
                ReminderViewModel(
                    loadReminderDialog = container.todoUseCases.loadReminderDialog,
                    completeTodo = container.todoUseCases.completeTodo
                )
            else -> error("Unknown ViewModel class ${modelClass.name}")
        }
        return viewModel as T
    }
}

fun lightTodoViewModelFactory(context: Context): ViewModelProvider.Factory {
    val app = context.applicationContext as App
    return app.viewModelFactory
}

@Composable
fun lightTodoViewModelFactory(): ViewModelProvider.Factory {
    return lightTodoViewModelFactory(LocalContext.current)
}

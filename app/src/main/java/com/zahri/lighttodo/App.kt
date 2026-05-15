package com.zahri.lighttodo

import android.app.Application
import com.zahri.lighttodo.calendar.CalendarObserver
import com.zahri.lighttodo.calendar.CalendarSync
import com.zahri.lighttodo.data.AppDatabase
import com.zahri.lighttodo.data.BackupBundle
import com.zahri.lighttodo.data.BackupTag
import com.zahri.lighttodo.data.BackupTodo
import com.zahri.lighttodo.data.Repository
import com.zahri.lighttodo.data.TagEntity
import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.data.UserPrefs
import com.zahri.lighttodo.notify.NotificationChannels
import com.zahri.lighttodo.notify.QuickAddService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class App : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val db by lazy { AppDatabase.get(this) }
    val prefs by lazy { UserPrefs(this) }
    val repository by lazy { Repository(this, db.todoDao(), db.tagDao(), prefs) }

    // Guarded by Main dispatcher confinement (only accessed from Dispatchers.Main)
    private var calendarObserver: CalendarObserver? = null
    private var calendarSyncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationChannels.ensure(this)

        // Watch quick-add notif preference and start/stop accordingly.
        appScope.launch {
            prefs.flow.collectLatest { snap ->
                if (snap.quickAddNotifEnabled) QuickAddService.start(this@App)
                else QuickAddService.stop(this@App)
            }
        }

        // Calendar ContentObserver lifecycle, tied to prefs.calendarSyncEnabled.
        // Confined to Main dispatcher so calendarObserver field access is thread-safe.
        // - enabled  -> register observer + run an initial pull
        // - disabled -> unregister observer + cancel in-flight sync
        appScope.launch(Dispatchers.Main.immediate) {
            prefs.flow
                .map { it.calendarSyncEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) {
                        if (calendarObserver == null) {
                            calendarObserver = CalendarObserver.register(this@App)
                            // Pull existing events once so we don't have to wait
                            // for the next user-visible calendar change.
                            calendarSyncJob = appScope.launch(Dispatchers.IO) {
                                try {
                                    CalendarSync.runOnce(this@App)
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                }
                            }
                        }
                    } else {
                        // Cancel any in-flight sync before unregistering
                        calendarSyncJob?.cancel()
                        calendarSyncJob = null
                        calendarObserver?.let { CalendarObserver.unregister(this@App, it) }
                        calendarObserver = null
                    }
                }
        }

        autoRestoreIfEmpty()
        startAutoBackup()
    }

    companion object {
        @Volatile lateinit var instance: App
            private set
    }

    private fun autoRestoreIfEmpty() { retryRestore() }

    fun retryRestore() {
        appScope.launch(Dispatchers.IO) {
            if (db.todoDao().listAll().isNotEmpty()) return@launch
            val text = readBackupFile() ?: return@launch
            runCatching {
                val bundle = Json { ignoreUnknownKeys = true }
                    .decodeFromString(BackupBundle.serializer(), text)
                db.tagDao().upsertAll(bundle.tags.map { TagEntity(it.id, it.name, it.sortOrder) })
                db.todoDao().upsertAll(bundle.todos.map { t ->
                    TodoEntity(
                        id = t.id, title = t.title, note = t.note, date = t.date, dateMillis = t.dateMillis,
                        startHour = t.startHour, startMinute = t.startMinute,
                        deadlineHour = t.deadlineHour, deadlineMinute = t.deadlineMinute,
                        remindStartAtMillis = t.remindStartAtMillis, remindAtMillis = t.remindAtMillis,
                        customRemindHoursBefore = t.customRemindHoursBefore,
                        tagId = t.tagId, done = t.done, doneAtMillis = t.doneAtMillis,
                        createdAtMillis = t.createdAtMillis, calendarEventId = null
                    )
                })
                repository.rescheduleAllAlarms()
            }
        }
    }

    private fun startAutoBackup() {
        appScope.launch(Dispatchers.IO) {
            combine(db.todoDao().observeAll(), db.tagDao().observeAll()) { todos, tags -> todos to tags }
                .collectLatest { (todos, tags) ->
                    // Don't overwrite a valid backup with empty data (race with restore)
                    if (todos.isEmpty() && tags.isEmpty()) return@collectLatest
                    delay(3000)
                    runCatching {
                        val bundle = BackupBundle(
                            version = 1,
                            tags = tags.map { BackupTag(it.id, it.name, it.sortOrder) },
                            todos = todos.map { t ->
                                BackupTodo(
                                    id = t.id, title = t.title, note = t.note, date = t.date, dateMillis = t.dateMillis,
                                    startHour = t.startHour, startMinute = t.startMinute,
                                    deadlineHour = t.deadlineHour, deadlineMinute = t.deadlineMinute,
                                    remindStartAtMillis = t.remindStartAtMillis, remindAtMillis = t.remindAtMillis,
                                    customRemindHoursBefore = t.customRemindHoursBefore,
                                    tagId = t.tagId, done = t.done, doneAtMillis = t.doneAtMillis, createdAtMillis = t.createdAtMillis
                                )
                            }
                        )
                        val json = Json { prettyPrint = true; encodeDefaults = true }
                        writeBackupFile(json.encodeToString(bundle))
                    }
                }
        }
    }

    // ---- Backup: write to app-specific external dir (always writable, no permission) ----
    // This dir is deleted on uninstall, so we ALSO write to MediaStore Downloads (survives uninstall).

    private val backupFileName = "lighttodo-auto-backup.json"

    private fun writeBackupFile(content: String) {
        val bytes = content.toByteArray()
        // 1) App-specific external storage (reliable, no permission needed)
        writeToAppExternal(bytes)
        // 2) Direct file in Downloads (readable after reinstall with MANAGE_EXTERNAL_STORAGE)
        writeToDownloadsDirect(bytes)
    }

    private fun writeToAppExternal(bytes: ByteArray) {
        runCatching {
            val dir = getExternalFilesDir(null) ?: return
            java.io.File(dir, backupFileName).writeBytes(bytes)
        }
    }

    private fun writeToDownloadsDirect(bytes: ByteArray) {
        runCatching {
            val dir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            dir.mkdirs()
            java.io.File(dir, backupFileName).writeBytes(bytes)
        }
    }

    /** Read backup - try app-external first, then Downloads direct path */
    private fun readBackupFile(): String? {
        // 1) App-specific external (fast, works during normal operation)
        readFromAppExternal()?.let { return it }
        // 2) Direct file path (works after reinstall with MANAGE_EXTERNAL_STORAGE)
        readFromDirectPath()?.let { return it }
        return null
    }

    private fun readFromAppExternal(): String? = runCatching {
        val dir = getExternalFilesDir(null) ?: return null
        val f = java.io.File(dir, backupFileName)
        if (f.exists()) f.readText() else null
    }.getOrNull()

    private fun readFromDirectPath(): String? = runCatching {
        val file = java.io.File(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            backupFileName
        )
        if (file.exists() && file.canRead()) file.readText() else null
    }.getOrNull()
}

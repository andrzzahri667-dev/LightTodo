package com.zahri.lighttodo

import android.app.Application
import com.zahri.lighttodo.calendar.CalendarObserver
import com.zahri.lighttodo.calendar.CalendarSync
import com.zahri.lighttodo.data.AppDatabase
import com.zahri.lighttodo.data.BackupManager
import com.zahri.lighttodo.data.Repository
import com.zahri.lighttodo.data.UserPrefs
import com.zahri.lighttodo.notify.NotificationChannels
import com.zahri.lighttodo.notify.QuickAddService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class App : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val db by lazy { AppDatabase.get(this) }
    val prefs by lazy { UserPrefs(this) }
    val repository by lazy { Repository(this, db.todoDao(), db.tagDao(), prefs) }
    private val backupManager by lazy { BackupManager(this, db, repository, appScope) }

    private var calendarObserver: CalendarObserver? = null
    private var calendarSyncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationChannels.ensure(this)
        watchQuickAddPref()
        watchCalendarSync()
        backupManager.restoreIfEmpty()
        backupManager.startAutoBackup()
    }

    fun retryRestore() { backupManager.restoreIfEmpty() }

    companion object {
        @Volatile lateinit var instance: App
            private set
    }

    private fun watchQuickAddPref() {
        appScope.launch {
            prefs.flow.collectLatest { snap ->
                if (snap.quickAddNotifEnabled) QuickAddService.start(this@App)
                else QuickAddService.stop(this@App)
            }
        }
    }

    private fun watchCalendarSync() {
        appScope.launch(Dispatchers.Main.immediate) {
            prefs.flow
                .map { it.calendarSyncEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) {
                        if (calendarObserver == null) {
                            calendarObserver = CalendarObserver.register(this@App)
                            calendarSyncJob = appScope.launch(Dispatchers.IO) {
                                try { CalendarSync.runOnce(this@App) }
                                catch (e: Exception) { if (e is CancellationException) throw e }
                            }
                        }
                    } else {
                        calendarSyncJob?.cancel()
                        calendarSyncJob = null
                        calendarObserver?.let { CalendarObserver.unregister(this@App, it) }
                        calendarObserver = null
                    }
                }
        }
    }
}

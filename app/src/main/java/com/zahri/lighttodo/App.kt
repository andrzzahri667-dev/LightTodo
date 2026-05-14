package com.zahri.lighttodo

import android.app.Application
import com.zahri.lighttodo.calendar.CalendarObserver
import com.zahri.lighttodo.calendar.CalendarSync
import com.zahri.lighttodo.data.AppDatabase
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
    }

    companion object {
        @Volatile lateinit var instance: App
            private set
    }
}

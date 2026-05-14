package com.zahri.lighttodo

import android.app.Application
import com.zahri.lighttodo.calendar.CalendarObserver
import com.zahri.lighttodo.calendar.CalendarSync
import com.zahri.lighttodo.data.AppDatabase
import com.zahri.lighttodo.data.Repository
import com.zahri.lighttodo.data.UserPrefs
import com.zahri.lighttodo.notify.NotificationChannels
import com.zahri.lighttodo.notify.QuickAddService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    @Volatile
    private var calendarObserver: CalendarObserver? = null

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
        // - enabled  -> register observer + run an initial pull
        // - disabled -> unregister observer
        appScope.launch {
            prefs.flow
                .map { it.calendarSyncEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) {
                        if (calendarObserver == null) {
                            calendarObserver = CalendarObserver.register(this@App)
                            // Pull existing events once so we don't have to wait
                            // for the next user-visible calendar change.
                            try {
                                CalendarSync.runOnce(this@App)
                            } catch (_: Throwable) {
                            }
                        }
                    } else {
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

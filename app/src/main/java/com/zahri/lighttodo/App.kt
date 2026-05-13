package com.zahri.lighttodo

import android.app.Application
import com.zahri.lighttodo.data.AppDatabase
import com.zahri.lighttodo.data.Repository
import com.zahri.lighttodo.data.UserPrefs
import com.zahri.lighttodo.notify.NotificationChannels
import com.zahri.lighttodo.notify.QuickAddService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class App : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val db by lazy { AppDatabase.get(this) }
    val prefs by lazy { UserPrefs(this) }
    val repository by lazy { Repository(this, db.todoDao(), db.tagDao(), prefs) }

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
    }

    companion object {
        @Volatile lateinit var instance: App
            private set
    }
}

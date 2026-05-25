package com.zahri.lighttodo.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zahri.lighttodo.App
import com.zahri.lighttodo.calendar.CalendarSync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 重启 / 升级后重新安排所有未来提醒。
 * 同时若用户启用了"通知栏常驻添加"，则启动前台服务；
 * 启用了日历同步则跑一次拉取（observer 注册由 App.onCreate 负责）。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as App
        val pending = goAsync()
        app.appScope.launch(Dispatchers.IO) {
            try {
                app.repository.rescheduleAllAlarms()
                val snap = app.prefs.snapshot()
                if (snap.quickAddNotifEnabled) {
                    QuickAddService.start(context)
                }
                if (snap.calendarSyncEnabled) {
                    // observer registration happens in App.onCreate (process start);
                    // here we just make sure existing events are pulled in promptly.
                    try {
                        CalendarSync.runOnce(context)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        // Best effort at boot; normal app startup will retry via the observer path.
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                pending.finish()
            }
        }
    }
}

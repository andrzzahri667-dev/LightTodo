package com.zahri.lighttodo.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zahri.lighttodo.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 重启 / 升级后重新安排所有未来提醒。
 * 同时若用户启用了"通知栏常驻添加"，则启动前台服务。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as App
        val pending = goAsync()
        runBlocking(Dispatchers.IO) {
            try {
                app.repository.rescheduleAllAlarms()
                val snap = app.prefs.snapshot()
                if (snap.quickAddNotifEnabled) {
                    QuickAddService.start(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

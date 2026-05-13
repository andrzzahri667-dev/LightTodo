package com.zahri.lighttodo.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val REMINDER_ID = "reminder"
    const val QUICK_ADD_ID = "quick_add"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val reminder = NotificationChannel(REMINDER_ID, "任务提醒", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "到点提醒"
            enableVibration(true)
        }
        val quickAdd = NotificationChannel(QUICK_ADD_ID, "快速添加", NotificationManager.IMPORTANCE_LOW).apply {
            description = "通知栏常驻入口"
            setShowBadge(false)
        }
        nm.createNotificationChannel(reminder)
        nm.createNotificationChannel(quickAdd)
    }
}

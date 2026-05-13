package com.zahri.lighttodo.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

object NotificationChannels {
    const val REMINDER_ID = "reminder_v2"
    const val QUICK_ADD_ID = "quick_add"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        val reminder = NotificationChannel(
            REMINDER_ID, "任务提醒", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "到点弹出主界面"
            enableVibration(true)
            enableLights(true)
            setBypassDnd(false)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        val quickAdd = NotificationChannel(
            QUICK_ADD_ID, "快速添加", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "通知栏常驻入口"
            setShowBadge(false)
        }
        nm.createNotificationChannel(reminder)
        nm.createNotificationChannel(quickAdd)
    }
}

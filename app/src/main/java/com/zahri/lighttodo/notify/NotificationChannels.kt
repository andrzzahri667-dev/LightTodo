package com.zahri.lighttodo.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.zahri.lighttodo.R

object NotificationChannels {
    const val REMINDER_ID = "reminder"
    const val QUICK_ADD_ID = "quick_add"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val reminder = NotificationChannel(REMINDER_ID, context.getString(R.string.notif_channel_reminder), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.notif_channel_reminder_desc)
            enableVibration(true)
        }
        val quickAdd = NotificationChannel(QUICK_ADD_ID, context.getString(R.string.notif_channel_quick_add), NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.notif_channel_quick_add_desc)
            setShowBadge(false)
        }
        nm.createNotificationChannel(reminder)
        nm.createNotificationChannel(quickAdd)
    }
}

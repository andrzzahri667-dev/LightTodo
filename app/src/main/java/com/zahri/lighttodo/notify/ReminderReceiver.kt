package com.zahri.lighttodo.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.zahri.lighttodo.App
import com.zahri.lighttodo.MainActivity
import com.zahri.lighttodo.ui.home.displayTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 提醒到点广播接收器。在 BG 拉起一个高优先级通知。
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = ReminderScheduler.extractTodoId(intent) ?: return
        val pending = goAsync()
        runBlocking(Dispatchers.IO) {
            try {
                val app = context.applicationContext as App
                val todo = app.db.todoDao().findById(id) ?: return@runBlocking
                if (todo.done) return@runBlocking

                val fullScreenIntent = Intent(context, ReminderActivity::class.java).apply {
                    putExtra(ReminderActivity.EXTRA_TODO_ID, id)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val fullScreenPi = PendingIntent.getActivity(
                    context, id.toInt(), fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val contentPi = PendingIntent.getActivity(
                    context, id.toInt(),
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notif = NotificationCompat.Builder(context, NotificationChannels.REMINDER_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(todo.displayTitle())
                    .setContentText(buildSubtitle(todo))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setContentIntent(contentPi)
                    .setFullScreenIntent(fullScreenPi, true)
                    .build()
                val nm = context.getSystemService(NotificationManager::class.java)
                nm?.notify(id.toInt(), notif)
            } finally {
                pending.finish()
            }
        }
    }

    private fun buildSubtitle(t: com.zahri.lighttodo.data.TodoEntity): String {
        val parts = mutableListOf<String>()
        if (t.deadlineHour != null && t.deadlineMinute != null) {
            parts += "截止 %02d:%02d".format(t.deadlineHour, t.deadlineMinute)
        }
        if (!t.note.isNullOrBlank()) {
            parts += t.note.lineSequence().firstOrNull().orEmpty()
        }
        return parts.joinToString(" · ")
    }
}

package com.zahri.lighttodo.integration.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zahri.lighttodo.App
import com.zahri.lighttodo.MainActivity
import com.zahri.lighttodo.R
import com.zahri.lighttodo.integration.notification.NotificationChannels
import com.zahri.lighttodo.feature.reminder.ReminderActivity
import com.zahri.lighttodo.feature.home.displayTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 提醒到点广播接收器。在 BG 拉起一个高优先级通知。
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = ReminderScheduler.extractTodoId(intent) ?: return
        val isStart = ReminderScheduler.isStartReminder(intent)
        val app = context.applicationContext as App
        val pending = goAsync()
        app.appScope.launch(Dispatchers.IO) {
            try {
                val todo = app.db.todoDao().findById(id) ?: return@launch
                if (todo.done) return@launch

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

                val nm = context.getSystemService(NotificationManager::class.java)
                val canUseFullScreenIntent =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                        nm?.canUseFullScreenIntent() == true
                val attachFullScreenIntent = ReminderFullScreenPolicy.shouldAttachFullScreenIntent(
                    canUseFullScreenIntent = canUseFullScreenIntent
                )

                val builder = NotificationCompat.Builder(context, NotificationChannels.REMINDER_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(todo.displayTitle(context))
                    .setContentText(buildSubtitle(context, todo, isStart))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setContentIntent(contentPi)
                if (attachFullScreenIntent) {
                    builder.setFullScreenIntent(fullScreenPi, true)
                }
                val notif = builder.build()
                val notifId = ReminderRequestCodePolicy.notificationIdFor(id, isStart)
                nm?.notify(notifId, notif)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                pending.finish()
            }
        }
    }

    private fun buildSubtitle(context: Context, t: com.zahri.lighttodo.data.TodoEntity, isStart: Boolean): String {
        val parts = mutableListOf<String>()
        if (isStart && t.startHour != null && t.startMinute != null) {
            parts += context.getString(R.string.notif_start_time, t.startHour, t.startMinute)
        } else if (!isStart && t.deadlineHour != null && t.deadlineMinute != null) {
            parts += context.getString(R.string.notif_end_time, t.deadlineHour, t.deadlineMinute)
        }
        if (!t.note.isNullOrBlank()) {
            parts += t.note.lineSequence().firstOrNull().orEmpty()
        }
        return parts.joinToString(" · ")
    }
}

package com.zahri.lighttodo.integration.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zahri.lighttodo.App
import com.zahri.lighttodo.AppLaunchIntents
import com.zahri.lighttodo.R
import com.zahri.lighttodo.domain.reminder.ReminderFullScreenPolicy
import com.zahri.lighttodo.domain.reminder.ReminderRequestCodePolicy
import com.zahri.lighttodo.integration.notification.NotificationChannels
import com.zahri.lighttodo.usecase.todo.TodoReminderNotification
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
                val reminder = app.container.todoUseCases.loadReminderNotification(
                    todoId = id,
                    fallbackTitle = context.getString(R.string.home_no_title)
                ) ?: return@launch

                val fullScreenPi = PendingIntent.getActivity(
                    context, id.toInt(), AppLaunchIntents.reminder(context, id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val contentPi = PendingIntent.getActivity(
                    context, id.toInt(),
                    AppLaunchIntents.main(context),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val nm = context.getSystemService(NotificationManager::class.java)
                val canUseFullScreenIntent =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                        nm?.canUseFullScreenIntent() == true
                val attachFullScreenIntent = ReminderFullScreenPolicy.shouldAttachFullScreenIntent(
                    sdkInt = Build.VERSION.SDK_INT,
                    canUseFullScreenIntent = canUseFullScreenIntent
                )

                val builder = NotificationCompat.Builder(context, NotificationChannels.REMINDER_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(reminder.title)
                    .setContentText(buildSubtitle(context, reminder, isStart))
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

    private fun buildSubtitle(context: Context, reminder: TodoReminderNotification, isStart: Boolean): String {
        val parts = mutableListOf<String>()
        if (isStart && reminder.startHour != null && reminder.startMinute != null) {
            parts += context.getString(R.string.notif_start_time, reminder.startHour, reminder.startMinute)
        } else if (!isStart && reminder.deadlineHour != null && reminder.deadlineMinute != null) {
            parts += context.getString(R.string.notif_end_time, reminder.deadlineHour, reminder.deadlineMinute)
        }
        if (reminder.notePreview.isNotBlank()) {
            parts += reminder.notePreview
        }
        return parts.joinToString(" · ")
    }
}

package com.zahri.lighttodo.integration.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zahri.lighttodo.domain.reminder.ReminderRequestCodePolicy
import com.zahri.lighttodo.usecase.todo.TodoRecord

object ReminderScheduler {

    private const val EXTRA_TODO_ID = "todo_id"
    const val EXTRA_IS_START = "is_start"

    fun schedule(context: Context, todo: TodoRecord, isStart: Boolean) {
        val triggerAt = if (isStart) todo.remindStartAtMillis ?: return else todo.remindAtMillis ?: return
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntentFor(context, todo.id, isStart)

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pendingIntentFor(context, id, isStart = true))
        am.cancel(pendingIntentFor(context, id, isStart = false))
    }

    fun extractTodoId(intent: Intent): Long? =
        intent.getLongExtra(EXTRA_TODO_ID, -1L).takeIf { it > 0 }

    fun isStartReminder(intent: Intent): Boolean =
        intent.getBooleanExtra(EXTRA_IS_START, false)

    private fun pendingIntentFor(context: Context, id: Long, isStart: Boolean): PendingIntent {
        val requestCode = ReminderRequestCodePolicy.requestCodeFor(id, isStart)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TODO_ID, id)
            putExtra(EXTRA_IS_START, isStart)
            action = "com.zahri.lighttodo.REMIND_${id}_${if (isStart) "start" else "end"}"
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

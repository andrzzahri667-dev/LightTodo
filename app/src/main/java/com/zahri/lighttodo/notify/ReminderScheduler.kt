package com.zahri.lighttodo.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zahri.lighttodo.data.TodoEntity

object ReminderScheduler {

    private const val EXTRA_TODO_ID = "todo_id"

    fun schedule(context: Context, todo: TodoEntity) {
        val triggerAt = todo.remindAtMillis ?: return
        scheduleAt(context, todo.id, triggerAt)
    }

    fun scheduleAt(context: Context, todoId: Long, triggerAtMillis: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntentFor(context, todoId)

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pendingIntentFor(context, id))
    }

    fun extractTodoId(intent: Intent): Long? =
        intent.getLongExtra(EXTRA_TODO_ID, -1L).takeIf { it > 0 }

    private fun pendingIntentFor(context: Context, id: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TODO_ID, id)
            action = "com.zahri.lighttodo.REMIND_$id"
        }
        return PendingIntent.getBroadcast(
            context, id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

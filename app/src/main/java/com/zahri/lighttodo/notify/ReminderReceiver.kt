package com.zahri.lighttodo.notify

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.zahri.lighttodo.App
import com.zahri.lighttodo.ui.home.displayTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 到点 → 启动 ReminderActivity 全屏弹出 + 同时挂一条 heads-up 通知（兜底，
 * 系统不允许全屏时也能看到）。
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

                // 直接尝试启动全屏 Activity（部分 ROM 后台启动 Activity 受限，
                // 但 Android 允许在 broadcast 触发的提醒场景下短时启动）
                val fullIntent = Intent(context, ReminderActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NO_HISTORY
                    )
                    putExtra(ReminderActivity.EXTRA_TODO_ID, id)
                }

                val fullPi = PendingIntent.getActivity(
                    context, id.toInt(), fullIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 通知作为兜底 + heads-up
                val notif = NotificationCompat.Builder(context, NotificationChannels.REMINDER_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(todo.displayTitle())
                    .setContentText(buildSubtitle(todo))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setOngoing(false)
                    .setContentIntent(fullPi)
                    // 关键：fullScreenIntent 让系统在屏幕亮 / 锁屏时直接弹 Activity
                    .setFullScreenIntent(fullPi, true)
                    .build()

                val nm = context.getSystemService(NotificationManager::class.java)
                nm?.notify(id.toInt(), notif)

                // 主动也尝试启动（部分 MIUI 设备需要）
                runCatching { context.startActivity(fullIntent) }
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
        return parts.joinToString(" · ").ifEmpty { "时间到了" }
    }
}

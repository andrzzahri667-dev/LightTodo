package com.zahri.lighttodo.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zahri.lighttodo.App
import com.zahri.lighttodo.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 在主进程运行（不带 android:process），处理小部件的 [ ] 勾选 / 行点击。
 * 这样写数据库的逻辑不会出现在 :widgetProvider 进程里，避免 Room 多进程并发问题
 * （也符合 MIUI"widget 进程不能拉起其他进程"的限制——本 receiver 是被广播唤醒的，
 *  并不算 widget 进程主动启动）。
 */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TodoWidgetProvider.ACTION_TOGGLE_DONE) return

        val openApp = intent.getBooleanExtra(TodoWidgetProvider.EXTRA_OPEN_APP, false)
        if (openApp) {
            val open = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(open)
            return
        }

        val todoId = intent.getLongExtra(TodoWidgetProvider.EXTRA_TODO_ID, -1L)
        if (todoId <= 0) return

        val pending = goAsync()
        runBlocking(Dispatchers.IO) {
            try {
                (context.applicationContext as App).repository.setDone(todoId, true)
            } finally {
                pending.finish()
            }
        }
        // Repository.setDone 会自动触发 widget 刷新
    }
}

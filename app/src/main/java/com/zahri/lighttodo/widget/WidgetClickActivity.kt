package com.zahri.lighttodo.widget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.zahri.lighttodo.App
import com.zahri.lighttodo.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 小部件点击中转 Activity（无 UI）。
 *
 * 为什么用 Activity 而不是 BroadcastReceiver：
 *  - MIUI 小部件规范 6.1: 不建议 PendingIntent → Receiver → startActivity 链路
 *  - 规范允许并推荐"业务有分发逻辑时使用 Activity 中转"
 *  - 这个 Activity 用 Theme.NoDisplay，立刻 finish()，用户不会看到闪屏
 *
 * 它在主进程运行，所以可以自由访问数据库 / 拉起其他 Activity。
 */
class WidgetClickActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val openApp = intent.getBooleanExtra(TodoWidgetProvider.EXTRA_OPEN_APP, false)
        val todoId = intent.getLongExtra(TodoWidgetProvider.EXTRA_TODO_ID, -1L)

        when {
            openApp -> {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            todoId > 0 -> {
                // 后台写库；不阻塞 Activity finish
                val app = applicationContext as App
                app.appScope.launch(Dispatchers.IO) {
                    app.repository.setDone(todoId, true)
                }
            }
        }

        finish()
        // 关闭 Activity 出场动画，让用户感觉不到我们存在过
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}

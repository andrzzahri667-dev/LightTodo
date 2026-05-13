package com.zahri.lighttodo.notify

import android.app.KeyguardManager
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.zahri.lighttodo.App
import com.zahri.lighttodo.MainActivity
import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.ui.theme.AppColors
import com.zahri.lighttodo.ui.theme.LightTodoTheme
import com.zahri.lighttodo.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 提醒到点的全屏 Activity。
 * - showWhenLocked + turnScreenOn → 锁屏直接亮屏弹出
 * - 由 ReminderReceiver 在到点时通过 fullScreenIntent 拉起
 */
class ReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)

        // 干掉对应的状态栏通知（已经全屏弹了，没必要再留通知）
        if (todoId > 0) {
            getSystemService(NotificationManager::class.java)?.cancel(todoId.toInt())
        }

        setContent {
            LightTodoTheme {
                ReminderUi(
                    todoId = todoId,
                    onMarkDone = {
                        lifecycleScope.launch {
                            App.instance.repository.setDone(todoId, true)
                            finish()
                        }
                    },
                    onSnooze10Min = {
                        // 简单实现：再过 10 分钟重新发同一条提醒
                        val newAt = System.currentTimeMillis() + 10 * 60 * 1000L
                        lifecycleScope.launch {
                            val t = withContext(Dispatchers.IO) {
                                App.instance.db.todoDao().findById(todoId)
                            }
                            if (t != null) {
                                ReminderScheduler.scheduleAt(applicationContext, t.id, newAt)
                            }
                            finish()
                        }
                    },
                    onOpenApp = {
                        startActivity(android.content.Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_TODO_ID = "todo_id"
    }
}

@Composable
private fun ReminderUi(
    todoId: Long,
    onMarkDone: () -> Unit,
    onSnooze10Min: () -> Unit,
    onOpenApp: () -> Unit,
    onDismiss: () -> Unit
) {
    var todo by remember { mutableStateOf<TodoEntity?>(null) }

    LaunchedEffect(todoId) {
        if (todoId > 0) {
            todo = withContext(Dispatchers.IO) {
                App.instance.db.todoDao().findById(todoId)
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "任务提醒",
                color = AppColors.Brand,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                todo?.let { it.title?.takeIf { t -> t.isNotBlank() } ?: it.note?.lineSequence()?.firstOrNull() ?: "无标题" }
                    ?: "（任务已被删除）",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            todo?.let { t ->
                val sub = buildString {
                    append(DateUtils.displayDate(t.date))
                    if (t.deadlineHour != null && t.deadlineMinute != null) {
                        append("  截止 ")
                        append("%02d:%02d".format(t.deadlineHour, t.deadlineMinute))
                    }
                }
                Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                if (!t.note.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        t.note,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onMarkDone,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Brand,
                    contentColor = Color.Black
                )
            ) { Text("标记完成", fontWeight = FontWeight.SemiBold) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onSnooze10Min) { Text("稍后 10 分钟") }
                OutlinedButton(onClick = onOpenApp) { Text("打开应用") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss) { Text("关闭") }
        }
    }
}

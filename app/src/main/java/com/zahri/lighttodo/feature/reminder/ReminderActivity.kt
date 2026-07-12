package com.zahri.lighttodo.feature.reminder

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.zahri.lighttodo.MainActivity
import com.zahri.lighttodo.R
import com.zahri.lighttodo.lightTodoViewModelFactory
import com.zahri.lighttodo.ui.theme.AppColors
import com.zahri.lighttodo.ui.theme.LightTodoTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ReminderActivity : ComponentActivity() {
    private val reminderViewModel: ReminderViewModel by viewModels {
        lightTodoViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        if (todoId <= 0) { finish(); return }

        lifecycleScope.launch {
            val fallbackTitle = getString(R.string.home_no_title)
            val reminder = reminderViewModel.load(todoId, fallbackTitle)
            if (reminder == null) {
                finish()
                return@launch
            }
            setContent {
                var completing by remember { mutableStateOf(false) }
                LightTodoTheme {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0x80000000))
                            .clickable { /* dismiss */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    stringResource(R.string.notif_task_reminder),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    reminder.title,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (reminder.note.isNotBlank()) {
                                    Text(
                                        reminder.note,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        enabled = !completing,
                                        onClick = {
                                            if (!completing) {
                                                completing = true
                                                lifecycleScope.launch {
                                                    try {
                                                        reminderViewModel.complete(todoId)
                                                        finish()
                                                    } catch (e: Exception) {
                                                        if (e is CancellationException) throw e
                                                        completing = false
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AppColors.Brand,
                                            contentColor = Color.Black
                                        )
                                    ) { Text(stringResource(R.string.reminder_complete)) }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            startActivity(
                                                Intent(
                                                    this@ReminderActivity,
                                                    MainActivity::class.java
                                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                            finish()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) { Text(stringResource(R.string.reminder_view)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_TODO_ID = "todo_id"
    }
}

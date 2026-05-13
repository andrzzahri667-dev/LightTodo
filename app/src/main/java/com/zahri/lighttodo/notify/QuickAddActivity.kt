package com.zahri.lighttodo.notify

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.zahri.lighttodo.App
import com.zahri.lighttodo.data.TodoInput
import com.zahri.lighttodo.ui.theme.AppColors
import com.zahri.lighttodo.ui.theme.LightTodoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 半透明对话框 Activity。从通知栏拉起。
 * 一行输入 → 默认日期=今天 → 保存。
 *
 * 关键：
 *  - manifest 里 windowSoftInputMode=stateAlwaysVisible|adjustResize，确保 IME 自动弹
 *  - LaunchedEffect 内 delay 一帧再 requestFocus()，避免 Activity 还没 attach window 时调用失败
 *  - 同时用 keyboardController.show() 双保险
 */
class QuickAddActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 显式让窗口 SOFT_INPUT_STATE_ALWAYS_VISIBLE（双保险）
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        setContent {
            LightTodoTheme {
                QuickAddDialog(
                    onSave = { text ->
                        val trimmed = text.trim()
                        if (trimmed.isNotEmpty()) {
                            val today = LocalDate.now()
                            lifecycleScope.launch {
                                App.instance.repository.saveTodo(
                                    TodoInput(
                                        title = trimmed,
                                        note = null,
                                        year = today.year,
                                        month = today.monthValue,
                                        day = today.dayOfMonth
                                    )
                                )
                                finish()
                            }
                        } else finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@Composable
private fun QuickAddDialog(onSave: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        // 等一帧再 focus，避免 window 还没 attach
        delay(80)
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = false) {},
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("快速添加", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("写点什么…") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { onSave(text) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(text) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Brand,
                            contentColor = Color.Black
                        )
                    ) { Text("保存") }
                }
            }
        }
    }
}

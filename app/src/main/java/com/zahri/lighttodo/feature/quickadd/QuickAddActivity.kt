package com.zahri.lighttodo.feature.quickadd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.zahri.lighttodo.R
import com.zahri.lighttodo.lightTodoViewModelFactory
import com.zahri.lighttodo.ui.theme.AppColors
import com.zahri.lighttodo.ui.theme.LightTodoTheme
import kotlinx.coroutines.delay

/**
 * 半透明对话框风格的 Activity。从通知或小组件 + 按钮拉起。
 * 一行输入 → 默认创建无日期任务（"先记下来，有空再处理"）→ 保存。
 *
 * 键盘自动弹起策略（从弱到强叠加）：
 *  - Window 设 SOFT_INPUT_STATE_ALWAYS_VISIBLE
 *  - WindowInsetsControllerCompat.show(IME)
 *  - LaunchedEffect 中 delay 一帧再 requestFocus + keyboardController.show()
 */
class QuickAddActivity : ComponentActivity() {
    private val quickAddViewModel: QuickAddViewModel by viewModels {
        lightTodoViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1) 提示窗口管理器永远显示软键盘
        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                    or android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        // 2) 显式让 IME 立即出来
        WindowCompat.getInsetsController(window, window.decorView)
            ?.show(WindowInsetsCompat.Type.ime())

        setContent {
            LightTodoTheme {
                QuickAddDialog(
                    onSave = { text ->
                        quickAddViewModel.save(text) { finish() }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun QuickAddDialog(onSave: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // 等 Window 拿到焦点 + 输入框真正附着后再请求焦点，避免冷启动时被吞
    LaunchedEffect(Unit) {
        delay(150)
        focus.requestFocus()
        keyboard?.show()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            // 用 pointerInput 检测点击空白处，避免 clickable 引入 focusable 抢走 TextField 焦点
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onCancel() })
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                // 同样用 pointerInput 拦截穿透，不引入 focusable
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                },
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.quick_add_title), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.quick_add_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.quick_add_cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(text) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brand, contentColor = Color.Black)
                    ) { Text(stringResource(R.string.quick_add_save)) }
                }
            }
        }
    }
}

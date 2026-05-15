package com.zahri.lighttodo.notify

import android.os.Bundle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.zahri.lighttodo.App
import com.zahri.lighttodo.R
import com.zahri.lighttodo.data.TodoInput
import com.zahri.lighttodo.ui.theme.AppColors
import com.zahri.lighttodo.ui.theme.LightTodoTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 半透明对话框风格的 Activity。从通知或小组件 + 按钮拉起。
 * 一行输入 → 默认日期=今天 → 保存。
 */
class QuickAddActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        setContent {
            LightTodoTheme {
                QuickAddDialog(
                    onSave = { text ->
                        if (text.isNotBlank()) {
                            val today = LocalDate.now()
                            lifecycleScope.launch {
                                App.instance.repository.saveTodo(
                                    TodoInput(
                                        title = text,
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

@androidx.compose.runtime.Composable
private fun QuickAddDialog(onSave: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = false) {},
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.quick_add_title), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.quick_add_hint)) },
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

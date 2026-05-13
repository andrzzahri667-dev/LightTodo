package com.zahri.lighttodo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Material3 Compose 版日期选择器对话框。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3DatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit
) {
    val initMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { ms ->
                    val ld = java.time.Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                    onPick(ld)
                }
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    ) {
        DatePicker(state = state)
    }
}

/**
 * Material3 Compose 版时间选择器对话框。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onPick: (Int, Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties()) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("选择时间", style = MaterialTheme.typography.titleMedium)
                Box(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = state)
                }
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = {
                        onPick(state.hour, state.minute)
                        onDismiss()
                    }) { Text("确定") }
                }
            }
        }
    }
}

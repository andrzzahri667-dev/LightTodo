package com.zahri.lighttodo.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zahri.lighttodo.ui.components.M3DatePickerDialog
import com.zahri.lighttodo.ui.components.M3TimePickerDialog
import com.zahri.lighttodo.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    editingId: Long?,
    onBack: () -> Unit,
    initialTitle: String? = null,
    vm: EditViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(editingId, initialTitle) {
        vm.load(editingId, initialTitle)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingId == null) "新建" else "编辑", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (editingId != null && !state.readOnly) {
                        IconButton(onClick = { vm.delete(); onBack() }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::setTitle,
                label = { Text("标题（可空）") },
                singleLine = true,
                enabled = !state.readOnly,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = vm::setNote,
                label = { Text("备注") },
                enabled = !state.readOnly,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            // 日期
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("日期", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
                AssistChip(
                    enabled = !state.readOnly,
                    onClick = { showDatePicker = true },
                    label = {
                        Text(
                            "%d 年 %02d 月 %02d 日".format(
                                state.date.year, state.date.monthValue, state.date.dayOfMonth
                            )
                        )
                    }
                )
            }

            // 截止时间
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("截止时间", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
                AssistChip(
                    enabled = !state.readOnly,
                    onClick = { showTimePicker = true },
                    label = {
                        Text(state.deadline?.let { "%02d:%02d".format(it.first, it.second) } ?: "全天")
                    }
                )
                if (state.deadline != null && !state.readOnly) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = vm::clearDeadline) { Text("清除") }
                }
            }

            // 提醒
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("提醒", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
                if (state.deadline == null) {
                    Text("默认 ${state.defaultRemindLabel}", color = MaterialTheme.colorScheme.onSurface)
                } else {
                    val hours = state.customHoursBefore ?: state.defaultHoursBefore
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            enabled = !state.readOnly,
                            onClick = { vm.adjustHoursBefore(-1) },
                            label = { Text("−") }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("提前 ${hours} 小时", color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(6.dp))
                        AssistChip(
                            enabled = !state.readOnly,
                            onClick = { vm.adjustHoursBefore(+1) },
                            label = { Text("+") }
                        )
                    }
                }
            }

            // 标签
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("标签", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
                OutlinedTextField(
                    value = state.tagName,
                    onValueChange = vm::setTagName,
                    placeholder = { Text("可空") },
                    singleLine = true,
                    enabled = !state.readOnly,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (state.allTags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.allTags) { t ->
                        FilterChip(
                            enabled = !state.readOnly,
                            selected = state.tagName == t.name,
                            onClick = { vm.setTagName(t.name) },
                            label = { Text(t.name) }
                        )
                    }
                }
            }

            if (state.readOnly) {
                Text(
                    "此任务来自系统日历，仅可勾选完成。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.weight(1f))
            if (!state.readOnly) {
                Button(
                    onClick = { vm.save(); onBack() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Brand,
                        contentColor = Color.Black
                    )
                ) { Text("保存", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showDatePicker) {
        M3DatePickerDialog(
            initial = state.date,
            onDismiss = { showDatePicker = false },
            onPick = { vm.setDate(it.year, it.monthValue, it.dayOfMonth) }
        )
    }
    if (showTimePicker) {
        val (h, m) = state.deadline ?: (9 to 0)
        M3TimePickerDialog(
            initialHour = h,
            initialMinute = m,
            onDismiss = { showTimePicker = false },
            onPick = { hh, mm -> vm.setDeadline(hh, mm) }
        )
    }
}

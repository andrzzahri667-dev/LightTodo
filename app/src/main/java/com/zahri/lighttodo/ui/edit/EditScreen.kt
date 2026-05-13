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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
                title = {
                    Text(
                        if (editingId == null) "新建" else "编辑",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (editingId != null) {
                        IconButton(onClick = {
                            vm.delete()
                            onBack()
                        }) {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::setTitle,
                label = { Text("标题（可空）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.readOnly
            )
            OutlinedTextField(
                value = state.note,
                onValueChange = vm::setNote,
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                enabled = !state.readOnly
            )

            // Date row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("日期", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
                AssistChip(
                    enabled = !state.readOnly,
                    onClick = { showDatePicker = true },
                    label = { Text("%d 年 %02d 月 %02d 日".format(state.date.year, state.date.monthValue, state.date.dayOfMonth)) }
                )
            }

            // Deadline row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("截止时间", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
                AssistChip(
                    enabled = !state.readOnly,
                    onClick = { showTimePicker = true },
                    label = { Text(state.deadline?.let { "%02d:%02d".format(it.first, it.second) } ?: "全天") }
                )
                if (state.deadline != null && !state.readOnly) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = vm::clearDeadline) { Text("清除") }
                }
            }

            // Remind row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("提醒", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
                if (state.deadline == null) {
                    Text("默认（${state.defaultRemindLabel}）", color = MaterialTheme.colorScheme.onSurface)
                } else {
                    val hours = state.customHoursBefore ?: state.defaultHoursBefore
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            enabled = !state.readOnly,
                            onClick = { vm.adjustHoursBefore(-1) },
                            label = { Text("-") }
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

            // Tag row
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
                    onClick = {
                        vm.save()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Brand, contentColor = Color.Black)
                ) {
                    Text("保存", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Material3 date picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        vm.setDate(localDate.year, localDate.monthValue, localDate.dayOfMonth)
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Horizontal time slot picker
    if (showTimePicker) {
        TimeSlotPickerDialog(
            currentHour = state.deadline?.first ?: 9,
            currentMinute = state.deadline?.second ?: 0,
            onSelect = { h, m -> vm.setDeadline(h, m); showTimePicker = false },
            onDismiss = { showTimePicker = false }
        )
    }
}

/** 06:00 ~ 23:30，每 30 分钟一档 */
private val TIME_SLOTS: List<Pair<Int, Int>> = buildList {
    for (h in 6..23) {
        add(h to 0)
        if (h < 23) add(h to 30)
    }
}

@Composable
private fun TimeSlotPickerDialog(
    currentHour: Int,
    currentMinute: Int,
    onSelect: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(currentHour to currentMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间", fontWeight = FontWeight.SemiBold) },
        text = {
            val listState = rememberLazyListState()
            val selectedIndex by remember {
                derivedStateOf {
                    TIME_SLOTS.indexOfFirst { it.first == selected.first && it.second == selected.second }.coerceAtLeast(0)
                }
            }
            // auto-scroll to current selection on first composition
            LaunchedEffect(Unit) {
                val idx = TIME_SLOTS.indexOfFirst { it.first == currentHour && it.second == currentMinute }.coerceAtLeast(0)
                if (idx > 2) listState.scrollToItem(idx - 2)
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TIME_SLOTS) { (h, m) ->
                        val isSelected = h == selected.first && m == selected.second
                        FilterChip(
                            selected = isSelected,
                            onClick = { selected = h to m },
                            label = { Text("%02d:%02d".format(h, m)) }
                        )
                    }
                }
                // quick presets
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("上午" to 9, "中午" to 12, "下午" to 15, "晚上" to 20).forEach { (label, hour) ->
                        TextButton(onClick = { selected = hour to 0 }) { Text(label) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected.first, selected.second) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

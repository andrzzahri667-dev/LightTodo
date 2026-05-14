package com.zahri.lighttodo.ui.edit

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zahri.lighttodo.ui.theme.AppColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

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

    // Wheel-style time picker
    if (showTimePicker) {
        WheelTimePickerDialog(
            currentDate = state.date,
            currentHour = state.deadline?.first ?: 9,
            currentMinute = state.deadline?.second ?: 0,
            onConfirm = { date, h, m ->
                vm.setDate(date.year, date.monthValue, date.dayOfMonth)
                vm.setDeadline(h, m)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

// ──────────────────────────────────────────────────────────
// Wheel-style Time Picker Dialog (matches Samsung Calendar style)
// ──────────────────────────────────────────────────────────

/**
 * Generate date list: 15 days before and after the given date.
 */
private fun generateDateList(centerDate: LocalDate): List<LocalDate> {
    return (-15..15).map { centerDate.plusDays(it.toLong()) }
}

/**
 * Format date for wheel display: "周X, M月D日"
 */
private fun formatDateForWheel(date: LocalDate): String {
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE)
    return "$dayOfWeek, ${date.monthValue}月${date.dayOfMonth}日"
}

@Composable
private fun WheelTimePickerDialog(
    currentDate: LocalDate,
    currentHour: Int,
    currentMinute: Int,
    onConfirm: (LocalDate, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val dateList = remember(currentDate) { generateDateList(currentDate) }
    val dateLabels = remember(dateList) { dateList.map { formatDateForWheel(it) } }
    val hourLabels = remember { (0..23).map { "%d".format(it) } }
    val minuteLabels = remember { (0..59).map { "%d".format(it) } }

    val initialDateIndex = remember(currentDate, dateList) {
        dateList.indexOf(currentDate).coerceAtLeast(0)
    }

    var selectedDateIndex by remember { mutableIntStateOf(initialDateIndex) }
    var selectedHour by remember { mutableIntStateOf(currentHour) }
    var selectedMinute by remember { mutableIntStateOf(currentMinute) }

    val selectedDate by remember {
        derivedStateOf { dateList.getOrElse(selectedDateIndex) { currentDate } }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: "From" title
            Text(
                text = "开始时间",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            // Header: current selection summary
            Text(
                text = "${formatDateForWheel(selectedDate)}, %02d:%02d".format(selectedHour, selectedMinute),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            // Three-column wheel picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date column
                WheelPicker(
                    items = dateLabels,
                    selectedIndex = selectedDateIndex,
                    onSelectedChanged = { selectedDateIndex = it },
                    modifier = Modifier.weight(1.4f),
                    selectedColor = MaterialTheme.colorScheme.primary,
                    selectedFontSize = 18.sp,
                    unselectedFontSize = 14.sp
                )

                // Hour column
                WheelPicker(
                    items = hourLabels,
                    selectedIndex = selectedHour,
                    onSelectedChanged = { selectedHour = it },
                    modifier = Modifier.weight(0.8f),
                    selectedColor = MaterialTheme.colorScheme.primary,
                    selectedFontSize = 24.sp,
                    unselectedFontSize = 16.sp,
                    suffix = "H"
                )

                // Minute column
                WheelPicker(
                    items = minuteLabels,
                    selectedIndex = selectedMinute,
                    onSelectedChanged = { selectedMinute = it },
                    modifier = Modifier.weight(0.8f),
                    selectedColor = MaterialTheme.colorScheme.primary,
                    selectedFontSize = 24.sp,
                    unselectedFontSize = 16.sp,
                    suffix = "M"
                )
            }

            Spacer(Modifier.height(24.dp))

            // Bottom buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        "取消",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                // OK button
                Button(
                    onClick = { onConfirm(selectedDate, selectedHour, selectedMinute) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "确定",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

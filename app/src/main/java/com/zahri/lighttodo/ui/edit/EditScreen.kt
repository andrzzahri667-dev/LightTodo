package com.zahri.lighttodo.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val PrimaryBlue = Color(0xFF2196F3)

@Composable
fun EditScreen(
    editingId: Long?,
    onBack: () -> Unit,
    initialTitle: String? = null,
    vm: EditViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    LaunchedEffect(editingId, initialTitle) {
        vm.load(editingId, initialTitle)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar: close (X) on left, confirm (✓) on right, optional delete
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.weight(1f))
            if (editingId != null && !state.readOnly) {
                IconButton(onClick = {
                    vm.delete()
                    onBack()
                }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (!state.readOnly) {
                IconButton(onClick = {
                    vm.save()
                    onBack()
                }) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "保存",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Big page title
            Text(
                text = if (editingId == null) "新建任务" else "编辑任务",
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            )

            // ── Card 1: Title field (single row card) ───────────────
            Card {
                TitleField(
                    value = state.title,
                    onValueChange = vm::setTitle,
                    enabled = !state.readOnly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                )
            }

            // ── Card 2: All-day / From / To ──────────────────────────
            Card {
                Column {
                    SwitchRow(
                        label = "全天",
                        checked = state.deadline == null,
                        onCheckedChange = { allDay ->
                            if (allDay) vm.clearDeadline() else vm.setDeadline(9, 0)
                        },
                        enabled = !state.readOnly
                    )
                    RowDivider()
                    DateTimeRow(
                        label = "开始时间",
                        date = state.date,
                        time = state.deadline,
                        onClick = { if (!state.readOnly) showFromPicker = true }
                    )
                    RowDivider()
                    DateTimeRow(
                        label = "截止时间",
                        date = state.date,
                        time = state.deadline,
                        onClick = { if (!state.readOnly) showToPicker = true }
                    )
                }
            }

            // ── Card 3: Reminder ─────────────────────────────────────
            Card {
                ReminderRow(
                    deadline = state.deadline,
                    customHoursBefore = state.customHoursBefore,
                    defaultHoursBefore = state.defaultHoursBefore,
                    defaultRemindLabel = state.defaultRemindLabel,
                    enabled = !state.readOnly,
                    onDecrease = { vm.adjustHoursBefore(-1) },
                    onIncrease = { vm.adjustHoursBefore(+1) }
                )
            }

            // ── Card 4: Tag ──────────────────────────────────────────
            Card {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "标签",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.weight(1f))
                        BasicTextField(
                            value = state.tagName,
                            onValueChange = vm::setTagName,
                            enabled = !state.readOnly,
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                if (state.tagName.isEmpty()) {
                                    Text(
                                        "可空",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 15.sp
                                    )
                                }
                                inner()
                            },
                            modifier = Modifier.width(140.dp)
                        )
                    }
                    if (state.allTags.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
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
                }
            }

            // ── Card 5: Description (Note) ───────────────────────────
            Card {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    BasicTextField(
                        value = state.note,
                        onValueChange = vm::setNote,
                        enabled = !state.readOnly,
                        textStyle = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            if (state.note.isEmpty()) {
                                Text(
                                    "描述",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 16.sp
                                )
                            }
                            inner()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                    )
                }
            }

            if (state.readOnly) {
                Text(
                    "此任务来自系统日历，仅可勾选完成。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }

    // ── Wheel-style "From" picker (date + time merged) ─────────────
    if (showFromPicker) {
        WheelDateTimePickerDialog(
            title = "开始时间",
            initialDate = state.date,
            initialHour = state.deadline?.first ?: 9,
            initialMinute = state.deadline?.second ?: 0,
            onConfirm = { date, h, m ->
                vm.setDate(date.year, date.monthValue, date.dayOfMonth)
                vm.setDeadline(h, m)
                showFromPicker = false
            },
            onDismiss = { showFromPicker = false }
        )
    }
    if (showToPicker) {
        WheelDateTimePickerDialog(
            title = "截止时间",
            initialDate = state.date,
            initialHour = state.deadline?.first ?: 10,
            initialMinute = state.deadline?.second ?: 0,
            onConfirm = { date, h, m ->
                vm.setDate(date.year, date.monthValue, date.dayOfMonth)
                vm.setDeadline(h, m)
                showToPicker = false
            },
            onDismiss = { showToPicker = false }
        )
    }
}

// ──────────────────────────────────────────────────────────────────
// Reusable card / row components
// ──────────────────────────────────────────────────────────────────

@Composable
private fun Card(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
}

@Composable
private fun TitleField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    "请输入标题",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
            }
            inner()
        },
        modifier = modifier
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun DateTimeRow(
    label: String,
    date: LocalDate,
    time: Pair<Int, Int>?,
    onClick: () -> Unit
) {
    val display = remember(date, time) {
        val datePart = "%s, %d月%d日".format(
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE),
            date.monthValue,
            date.dayOfMonth
        )
        val timePart = time?.let { " %02d:%02d".format(it.first, it.second) } ?: ""
        "$datePart$timePart"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        Text(
            display,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ReminderRow(
    deadline: Pair<Int, Int>?,
    customHoursBefore: Int?,
    defaultHoursBefore: Int,
    defaultRemindLabel: String,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "提醒",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        if (deadline == null) {
            Text(
                "默认（$defaultRemindLabel）",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val hours = customHoursBefore ?: defaultHoursBefore
            StepperButton(text = "−", enabled = enabled, onClick = onDecrease)
            Spacer(Modifier.width(10.dp))
            Text(
                "提前 $hours 小时",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(10.dp))
            StepperButton(text = "+", enabled = enabled, onClick = onIncrease)
        }
    }
}

@Composable
private fun StepperButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ──────────────────────────────────────────────────────────────────
// Wheel-style date+time picker dialog
// ──────────────────────────────────────────────────────────────────

private fun generateDateList(centerDate: LocalDate): List<LocalDate> =
    (-180..180).map { centerDate.plusDays(it.toLong()) }

private fun formatDateForWheel(date: LocalDate): String {
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE)
    return "$dayOfWeek, ${date.monthValue}月${date.dayOfMonth}日"
}

@Composable
private fun WheelDateTimePickerDialog(
    title: String,
    initialDate: LocalDate,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (LocalDate, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val dateList = remember(initialDate) { generateDateList(initialDate) }
    val dateLabels = remember(dateList) { dateList.map { formatDateForWheel(it) } }
    val hourLabels = remember { (0..23).map { "%d".format(it) } }
    val minuteLabels = remember { (0..59).map { "%02d".format(it) } }

    val initialDateIndex = remember(initialDate, dateList) {
        dateList.indexOf(initialDate).coerceAtLeast(0)
    }

    var selectedDateIndex by remember { mutableIntStateOf(initialDateIndex) }
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    val selectedDate by remember {
        derivedStateOf { dateList.getOrElse(selectedDateIndex) { initialDate } }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${formatDateForWheel(selectedDate)}, %02d:%02d".format(selectedHour, selectedMinute),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(
                    items = dateLabels,
                    selectedIndex = selectedDateIndex,
                    onSelectedChanged = { selectedDateIndex = it },
                    modifier = Modifier.weight(1.6f),
                    selectedFontSize = 18.sp,
                    unselectedFontSize = 14.sp
                )
                WheelPicker(
                    items = hourLabels,
                    selectedIndex = selectedHour,
                    onSelectedChanged = { selectedHour = it },
                    modifier = Modifier.weight(0.7f),
                    selectedFontSize = 24.sp,
                    unselectedFontSize = 16.sp,
                    superscript = "H"
                )
                WheelPicker(
                    items = minuteLabels,
                    selectedIndex = selectedMinute,
                    onSelectedChanged = { selectedMinute = it },
                    modifier = Modifier.weight(0.7f),
                    selectedFontSize = 24.sp,
                    unselectedFontSize = 16.sp,
                    superscript = "M"
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        "取消",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                Button(
                    onClick = { onConfirm(selectedDate, selectedHour, selectedMinute) },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("确定", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

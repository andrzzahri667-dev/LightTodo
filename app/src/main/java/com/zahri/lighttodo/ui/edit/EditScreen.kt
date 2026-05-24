package com.zahri.lighttodo.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zahri.lighttodo.R
import com.zahri.lighttodo.ui.theme.AppType

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

    LaunchedEffect(editingId, initialTitle) { vm.load(editingId, initialTitle) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.edit_close), tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.weight(1f))
            if (editingId != null && !state.readOnly) {
                IconButton(onClick = { vm.delete(); onBack() }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.edit_delete), tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (!state.readOnly) {
                IconButton(onClick = { vm.save(); onBack() }) {
                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.edit_save), tint = MaterialTheme.colorScheme.onSurface)
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
            // Page title
            Text(
                text = if (editingId == null) stringResource(R.string.edit_title_new) else stringResource(R.string.edit_title_edit),
                style = AppType.largeTitle,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            )

            // Card 1: Title
            EditCard {
                BasicTextField(
                    value = state.title,
                    onValueChange = vm::setTitle,
                    enabled = !state.readOnly,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (state.title.isEmpty()) {
                            Text(stringResource(R.string.edit_title_hint), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 16.sp)
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp)
                )
            }

            // Card 2: All-day / From / To
            val dateEnabled = state.date != null
            EditCard {
                Column {
                    EditSwitchRow(
                        label = stringResource(R.string.edit_all_day),
                        checked = state.startTime == null && state.endTime == null,
                        onCheckedChange = { allDay ->
                            if (allDay) vm.clearTimes() else {
                                if (state.startTime == null) {
                                    val now = java.time.LocalTime.now()
                                    vm.setStartTime(now.hour, now.minute)
                                }
                            }
                        },
                        enabled = !state.readOnly && dateEnabled
                    )
                    EditRowDivider()
                    EditDateTimeRow(
                        label = stringResource(R.string.edit_start_time),
                        date = state.date,
                        time = state.startTime,
                        enabled = !state.readOnly && dateEnabled,
                        onClick = { if (!state.readOnly && dateEnabled) showFromPicker = true }
                    )
                    EditRowDivider()
                    EditDateTimeRow(
                        label = stringResource(R.string.edit_end_time),
                        date = state.date,
                        time = state.endTime,
                        enabled = !state.readOnly && dateEnabled,
                        onClick = { if (!state.readOnly && dateEnabled) showToPicker = true }
                    )
                }
            }

            // Card 3: Reminder
            EditCard {
                EditReminderRow(
                    hasTime = state.startTime != null || state.endTime != null,
                    customHoursBefore = state.customHoursBefore,
                    defaultHoursBefore = state.defaultHoursBefore,
                    defaultRemindLabel = state.defaultRemindLabel,
                    enabled = !state.readOnly && dateEnabled,
                    onDecrease = { vm.adjustHoursBefore(-1) },
                    onIncrease = { vm.adjustHoursBefore(+1) }
                )
            }

            // Card 4: Set-date toggle (controls cards above)
            EditCard {
                EditSwitchRow(
                    label = stringResource(R.string.edit_set_date),
                    checked = dateEnabled,
                    onCheckedChange = { vm.setDateEnabled(it) },
                    enabled = !state.readOnly
                )
            }

            // Card 4: Tag
            EditCard {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.edit_tag), fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.weight(1f))
                        BasicTextField(
                            value = state.tagName,
                            onValueChange = vm::setTagName,
                            enabled = !state.readOnly,
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                if (state.tagName.isEmpty()) {
                                    Text(stringResource(R.string.edit_tag_hint), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 15.sp)
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

            // Card 5: Note
            EditCard {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    BasicTextField(
                        value = state.note,
                        onValueChange = vm::setNote,
                        enabled = !state.readOnly,
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            if (state.note.isEmpty()) {
                                Text(stringResource(R.string.edit_note_hint), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 16.sp)
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                    )
                }
            }

            if (state.readOnly) {
                Text(stringResource(R.string.edit_read_only), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }

    // Picker dialogs
    if (showFromPicker && state.date != null) {
        val isAllDay = state.startTime == null && state.endTime == null
        if (isAllDay) {
            WheelDatePickerDialog(
                title = stringResource(R.string.edit_start_time),
                initialDate = state.date!!,
                onConfirm = { date ->
                    vm.setDate(date.year, date.monthValue, date.dayOfMonth)
                    showFromPicker = false
                },
                onDismiss = { showFromPicker = false }
            )
        } else {
            WheelDateTimePickerDialog(
                title = stringResource(R.string.edit_start_time),
                initialDate = state.date!!,
                initialHour = state.startTime?.first ?: 9,
                initialMinute = state.startTime?.second ?: 0,
                onConfirm = { date, h, m ->
                    vm.setDate(date.year, date.monthValue, date.dayOfMonth)
                    vm.setStartTime(h, m)
                    showFromPicker = false
                },
                onDismiss = { showFromPicker = false }
            )
        }
    }
    if (showToPicker && state.date != null) {
        val isAllDay = state.startTime == null && state.endTime == null
        if (isAllDay) {
            WheelDatePickerDialog(
                title = stringResource(R.string.edit_end_time),
                initialDate = state.date!!,
                onConfirm = { date ->
                    vm.setDate(date.year, date.monthValue, date.dayOfMonth)
                    showToPicker = false
                },
                onDismiss = { showToPicker = false }
            )
        } else {
            val fromH = state.startTime?.first ?: 9
            val fromM = state.startTime?.second ?: 0
            val totalFromMin = fromH * 60 + fromM
            WheelDateTimePickerDialog(
                title = stringResource(R.string.edit_end_time),
                initialDate = state.date!!,
                initialHour = state.endTime?.first ?: ((totalFromMin + 15) / 60),
                initialMinute = state.endTime?.second ?: ((totalFromMin + 15) % 60),
                onConfirm = { date, h, m ->
                    vm.setDate(date.year, date.monthValue, date.dayOfMonth)
                    vm.setEndTime(h, m)
                    showToPicker = false
                },
                onDismiss = { showToPicker = false }
            )
        }
    }
}

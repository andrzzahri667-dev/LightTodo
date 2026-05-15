package com.zahri.lighttodo.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zahri.lighttodo.R
import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.ui.theme.AppColors
import com.zahri.lighttodo.ui.theme.AppType

@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val selectedIds by vm.selectedIds.collectAsStateWithLifecycle()
    val inSelection = selectedIds.isNotEmpty()

    Scaffold(
        floatingActionButton = {
            if (!inSelection) {
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = AppColors.Brand,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_add), tint = Color.Black)
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = inSelection, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.clearSelection() }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        stringResource(R.string.home_selected_count, selectedIds.size),
                        style = AppType.headline,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { vm.deleteSelected() }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.home_delete_selected), tint = AppColors.Overdue)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                bottom = 96.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Large title header (iOS style) ───────────────────
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = AppType.largeTitle,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.home_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Empty state ──────────────────────────────────────
            if (state.groups.isEmpty() && state.doneItems.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("☀️", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.home_empty),
                                style = AppType.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            // ── Todo groups ──────────────────────────────────────
            for (group in state.groups) {
                val key = HomeViewModel.groupKey(group.tagId)
                val expanded = key !in state.collapsedTagIds

                item(key = "header-$key") {
                    SectionHeader(
                        title = group.name,
                        count = group.items.size,
                        expanded = expanded,
                        onToggle = { vm.setGroupExpanded(key, !expanded) }
                    )
                }

                item(key = "body-$key") {
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = 300f)),
                        exit = shrinkVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            group.items.forEachIndexed { index, todo ->
                                TodoRow(
                                    todo = todo,
                                    selected = todo.id in selectedIds,
                                    inSelectionMode = inSelection,
                                    onToggle = { vm.toggleDone(todo.id, true) },
                                    onClick = {
                                        if (inSelection) vm.toggleSelection(todo.id)
                                        else onEdit(todo.id)
                                    },
                                    onLongClick = { vm.toggleSelection(todo.id) }
                                )
                                if (index < group.items.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 52.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Done section ─────────────────────────────────────
            if (state.doneItems.isNotEmpty()) {
                item(key = "done-header") {
                    SectionHeader(
                        title = stringResource(R.string.home_done_section),
                        count = state.doneItems.size,
                        expanded = state.doneExpanded,
                        onToggle = { vm.setDoneExpanded(!state.doneExpanded) }
                    )
                }

                item(key = "done-body") {
                    AnimatedVisibility(
                        visible = state.doneExpanded,
                        enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = 300f)),
                        exit = shrinkVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            state.doneItems.forEachIndexed { index, todo ->
                                TodoRow(
                                    todo = todo,
                                    selected = todo.id in selectedIds,
                                    inSelectionMode = inSelection,
                                    onToggle = { vm.toggleDone(todo.id, false) },
                                    onClick = {
                                        if (inSelection) vm.toggleSelection(todo.id)
                                        else onEdit(todo.id)
                                    },
                                    onLongClick = { vm.toggleSelection(todo.id) },
                                    strikeThrough = true
                                )
                                if (index < state.doneItems.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 52.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Section Header ──────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = AppType.headline,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$count",
            style = AppType.footnote,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─── Todo Row ────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoRow(
    todo: TodoEntity,
    selected: Boolean = false,
    inSelectionMode: Boolean = false,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    strikeThrough: Boolean = false
) {
    val titleText = todo.displayTitle(androidx.compose.ui.platform.LocalContext.current)
    val isOverdue = todo.isOverdueDate()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AppColors.Brand.copy(alpha = 0.12f) else Color.Transparent)
            .combinedClickable(onLongClick = onLongClick, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox or selection indicator
        if (inSelectionMode) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) AppColors.Brand else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Text("✓", color = Color.Black, fontSize = 13.sp)
            }
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (todo.done) AppColors.DoneGreen
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (todo.done) Text("✓", color = Color.White, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.width(12.dp))

        // Content
        Column(Modifier.weight(1f)) {
            Text(
                text = titleText,
                style = AppType.body,
                color = if (strikeThrough) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (strikeThrough) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Subtitle: time + calendar icon
            val timeSuffix = when {
                todo.startHour != null && todo.deadlineHour != null ->
                    "%02d:%02d – %02d:%02d".format(todo.startHour, todo.startMinute, todo.deadlineHour, todo.deadlineMinute)
                todo.startHour != null ->
                    "%02d:%02d".format(todo.startHour, todo.startMinute)
                todo.deadlineHour != null ->
                    "%02d:%02d".format(todo.deadlineHour, todo.deadlineMinute)
                else -> null
            }
            val subtitle = buildString {
                append(todo.dateLabel())
                if (timeSuffix != null) { append("  "); append(timeSuffix) }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subtitle,
                    style = AppType.caption1,
                    color = if (isOverdue) AppColors.Overdue else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (todo.calendarEventId != null) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar_sync),
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = Color.Unspecified
                    )
                }
            }
        }
    }
}

package com.zahri.lighttodo.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zahri.lighttodo.R
import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("今日安排", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = AppColors.Brand) {
                Icon(Icons.Default.Add, contentDescription = "新建", tint = Color.Black)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.groups.isEmpty() && state.doneItems.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无待办", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding() + 4.dp, bottom = 96.dp)
        ) {
            for (group in state.groups) {
                val key = HomeViewModel.groupKey(group.tagId)
                val expanded = key !in state.collapsedTagIds
                item(key = "header-$key") {
                    GroupHeader(
                        title = group.name,
                        count = group.items.size,
                        expanded = expanded,
                        onToggle = { vm.setGroupExpanded(key, !expanded) }
                    )
                }
                item(key = "body-$key") {
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            for (todo in group.items) {
                                TodoRow(
                                    todo = todo,
                                    onToggle = { vm.toggleDone(todo.id, true) },
                                    onClick = { onEdit(todo.id) }
                                )
                            }
                        }
                    }
                }
            }

            if (state.doneItems.isNotEmpty()) {
                item(key = "done-header") {
                    GroupHeader(
                        title = "已完成",
                        count = state.doneItems.size,
                        expanded = state.doneExpanded,
                        onToggle = { vm.setDoneExpanded(!state.doneExpanded) }
                    )
                }
                item(key = "done-body") {
                    AnimatedVisibility(
                        visible = state.doneExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            for (todo in state.doneItems) {
                                TodoRow(
                                    todo = todo,
                                    onToggle = { vm.toggleDone(todo.id, false) },
                                    onClick = { onEdit(todo.id) },
                                    strikeThrough = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(title: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$count",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun TodoRow(
    todo: TodoEntity,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    strikeThrough: Boolean = false
) {
    val titleText = todo.displayTitle()
    val isOverdue = todo.isOverdueDate()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Orange dot
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AppColors.Brand)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = titleText,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (strikeThrough || todo.done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val deadlineSuffix = when {
                    todo.startHour != null && todo.startMinute != null && todo.deadlineHour != null && todo.deadlineMinute != null ->
                        " %02d:%02d-%02d:%02d".format(todo.startHour, todo.startMinute, todo.deadlineHour, todo.deadlineMinute)
                    todo.startHour != null && todo.startMinute != null ->
                        " %02d:%02d".format(todo.startHour, todo.startMinute)
                    todo.deadlineHour != null && todo.deadlineMinute != null ->
                        " %02d:%02d".format(todo.deadlineHour, todo.deadlineMinute)
                    else -> ""
                }
                Text(
                    text = todo.dateLabel() + deadlineSuffix,
                    color = if (isOverdue) AppColors.Overdue else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                if (todo.calendarEventId != null) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar_sync),
                        contentDescription = "calendar",
                        modifier = Modifier.size(11.dp),
                        tint = Color.Unspecified
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        // Checkbox
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { onToggle() }
                .background(if (todo.done) AppColors.Brand else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (todo.done) {
                Text("✓", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            } else {
                androidx.compose.foundation.Canvas(Modifier.size(20.dp)) {
                    drawRoundRect(
                        color = Color(0x80FFFFFF),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )
                }
            }
        }
    }
}

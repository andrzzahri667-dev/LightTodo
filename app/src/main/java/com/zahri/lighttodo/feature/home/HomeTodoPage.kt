package com.zahri.lighttodo.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zahri.lighttodo.R
import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.ui.motion.components.MotionSectionVisibility
import com.zahri.lighttodo.ui.motion.components.TodoCompletionIndicator
import com.zahri.lighttodo.ui.motion.components.motionCompletionSettleLayer
import com.zahri.lighttodo.ui.motion.components.motionExpansionRotationLayer
import com.zahri.lighttodo.ui.motion.components.motionSectionItemPlacement
import com.zahri.lighttodo.ui.motion.components.rememberMotionCompletionSettle
import com.zahri.lighttodo.ui.motion.components.rememberMotionExpansionRotation
import com.zahri.lighttodo.ui.motion.components.rememberMotionSelectionColor
import com.zahri.lighttodo.ui.theme.AppColors
import com.zahri.lighttodo.ui.theme.AppType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoPage(
    state: HomeUiState,
    selectedIds: Set<Long>,
    pendingCompleteIds: Set<Long>,
    inSelection: Boolean,
    vm: HomeViewModel,
    onEdit: (Long) -> Unit
) {
    val listItems = remember(state) { buildHomeTodoListItems(state) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 96.dp,
            start = 16.dp,
            end = 16.dp
        )
    ) {
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

        items(
            items = listItems,
            key = { it.key }
        ) { item ->
            when (item) {
                is HomeTodoListItem.Header -> {
                    SectionHeader(
                        title = if (item.doneSection) stringResource(R.string.home_done_section) else item.title,
                        count = item.count,
                        expanded = item.expanded,
                        onToggle = {
                            if (item.doneSection) {
                                vm.setDoneExpanded(!item.expanded)
                            } else {
                                vm.setGroupExpanded(item.sectionKey, !item.expanded)
                            }
                        },
                        modifier = motionSectionItemPlacement().padding(bottom = 8.dp)
                    )
                }
                is HomeTodoListItem.TodoRow -> {
                    val todo = item.todo
                    MotionSectionVisibility(
                        visible = item.visible,
                        modifier = motionSectionItemPlacement()
                    ) {
                        TodoRow(
                            todo = todo,
                            selected = todo.id in selectedIds,
                            inSelectionMode = inSelection,
                            onToggle = { vm.toggleDone(todo.id, !item.strikeThrough) },
                            onClick = {
                                if (inSelection) vm.toggleSelection(todo.id)
                                else onEdit(todo.id)
                            },
                            onLongClick = { vm.toggleSelection(todo.id) },
                            strikeThrough = item.strikeThrough,
                            animating = todo.id in pendingCompleteIds,
                            showDivider = item.showDivider,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arrowRotation by rememberMotionExpansionRotation(expanded)

    Row(
        modifier = modifier
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
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .motionExpansionRotationLayer(arrowRotation)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoRow(
    todo: TodoEntity,
    selected: Boolean = false,
    inSelectionMode: Boolean = false,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    strikeThrough: Boolean = false,
    animating: Boolean = false,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val titleText = todo.displayTitle(context)
    val isOverdue = todo.isOverdueDate()
    val selectedBackground by rememberMotionSelectionColor(
        selected = selected,
        selectedColor = AppColors.Brand.copy(alpha = 0.12f),
        unselectedColor = Color.Transparent,
        label = "todo-selection-bg"
    )

    val displayDone = todo.done || animating
    val displayStrike = strikeThrough || animating
    val completionSettle = rememberMotionCompletionSettle(animating)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(selectedBackground)
                .combinedClickable(onLongClick = onLongClick, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (inSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (selected) AppColors.Brand else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        .semantics {
                            contentDescription = context.getString(
                                if (selected) {
                                    R.string.home_selected_indicator
                                } else {
                                    R.string.home_not_selected_indicator
                                },
                                titleText
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Text("✓", color = Color.Black, fontSize = 13.sp)
                }
            } else {
                TodoCompletionIndicator(
                    displayDone = displayDone,
                    animating = animating,
                    enabled = !animating,
                    contentDescription = context.getString(
                        if (displayDone) R.string.home_mark_active else R.string.home_mark_done,
                        titleText
                    ),
                    onToggle = onToggle
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                Modifier
                    .weight(1f)
                    .motionCompletionSettleLayer(completionSettle)
            ) {
                Text(
                    text = titleText,
                    style = AppType.body,
                    color = if (displayStrike) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (displayStrike) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                    if (timeSuffix != null) {
                        append("  ")
                        append(timeSuffix)
                    }
                }
                if (subtitle.isNotEmpty() || todo.calendarEventId != null) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = AppType.caption1,
                                color = if (isOverdue) AppColors.Overdue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (todo.calendarEventId != null) {
                            if (subtitle.isNotEmpty()) Spacer(Modifier.width(6.dp))
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
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}

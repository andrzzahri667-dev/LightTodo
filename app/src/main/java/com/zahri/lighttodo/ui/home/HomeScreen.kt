package com.zahri.lighttodo.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zahri.lighttodo.R
import com.zahri.lighttodo.data.NoteEntity
import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.ui.home.note.NoteGridPage
import com.zahri.lighttodo.ui.theme.AppColors
import com.zahri.lighttodo.ui.theme.AppType
import kotlinx.coroutines.launch

/** Page indices for the home pager. */
private object HomePagerPages {
    const val NOTE = 0
    const val TODO = 1
    const val COUNT = 2
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onNoteEdit: (Long?) -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val selectedIds by vm.selectedIds.collectAsStateWithLifecycle()
    val pendingCompleteIds by vm.pendingCompleteIds.collectAsStateWithLifecycle()
    val inSelection = selectedIds.isNotEmpty()

    val notes by vm.notes.collectAsStateWithLifecycle()
    val noteSelectedIds by vm.noteSelectedIds.collectAsStateWithLifecycle()
    val noteInSelection = noteSelectedIds.isNotEmpty()

    val pagerState = rememberPagerState(initialPage = HomePagerPages.NOTE) { HomePagerPages.COUNT }
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage

    Scaffold(
        floatingActionButton = {
            val anySelection = inSelection || noteInSelection
            if (!anySelection) {
                val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.92f else 1f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 800f),
                    label = "fab-scale"
                )
                FloatingActionButton(
                    onClick = {
                        when (currentPage) {
                            HomePagerPages.NOTE -> onNoteEdit(null)
                            HomePagerPages.TODO -> onAdd()
                        }
                    },
                    containerColor = AppColors.Brand,
                    shape = CircleShape,
                    interactionSource = interaction,
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_add), tint = Color.Black)
                }
            }
        },
        bottomBar = {
            // Todo selection bar
            AnimatedVisibility(visible = inSelection && currentPage == HomePagerPages.TODO, enter = fadeIn(), exit = fadeOut()) {
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
            // Note selection bar
            AnimatedVisibility(visible = noteInSelection && currentPage == HomePagerPages.NOTE, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.clearNoteSelection() }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        stringResource(R.string.note_selected_count, noteSelectedIds.size),
                        style = AppType.headline,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { vm.deleteSelectedNotes() }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.note_delete_selected), tint = AppColors.Overdue)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            // ── Header with indicator ────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page indicator dots
                val tabLabels = listOf(
                    stringResource(R.string.tab_notes),
                    stringResource(R.string.tab_todos)
                )
                tabLabels.forEachIndexed { index, label ->
                    val tabInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Text(
                        text = label,
                        style = AppType.title2.copy(
                            fontWeight = if (currentPage == index) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (currentPage == index) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = tabInteraction,
                                indication = null
                            ) {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .padding(horizontal = 4.dp)
                    )
                    if (index < tabLabels.lastIndex) Spacer(Modifier.width(12.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.home_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Pager ────────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    HomePagerPages.NOTE -> NoteGridPage(
                        notes = notes,
                        selectedIds = noteSelectedIds,
                        inSelectionMode = noteInSelection,
                        onNoteClick = { id ->
                            if (noteInSelection) vm.toggleNoteSelection(id)
                            else onNoteEdit(id)
                        },
                        onNoteLongClick = { id -> vm.toggleNoteSelection(id) }
                    )
                    HomePagerPages.TODO -> TodoPage(
                        state = state,
                        selectedIds = selectedIds,
                        pendingCompleteIds = pendingCompleteIds,
                        inSelection = inSelection,
                        vm = vm,
                        onEdit = onEdit
                    )
                }
            }
        }
    }
}

// ─── Todo Page (extracted from original HomeScreen) ──────────────
@Composable
private fun TodoPage(
    state: HomeUiState,
    selectedIds: Set<Long>,
    pendingCompleteIds: Set<Long>,
    inSelection: Boolean,
    vm: HomeViewModel,
    onEdit: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 96.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                                onLongClick = { vm.toggleSelection(todo.id) },
                                animating = todo.id in pendingCompleteIds
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
    strikeThrough: Boolean = false,
    animating: Boolean = false
) {
    val titleText = todo.displayTitle(androidx.compose.ui.platform.LocalContext.current)
    val isOverdue = todo.isOverdueDate()

    // ── Multi-stage complete animation (anime.js-style stagger + spring) ──
    // 时间轴:
    //  t=0       checkbox: scale 0.6 → 1.1 (spring overshoot) → 1.0,✓ alpha 0→1
    //  t=80ms    content : alpha 1→0.45,translationX 0→+6dp(被推开的感觉)
    //  t=220ms   commit: VM 真正 setDone
    val checkScale = remember { androidx.compose.animation.core.Animatable(1f) }
    val checkmarkAlpha = remember { androidx.compose.animation.core.Animatable(if (todo.done) 1f else 0f) }
    val contentAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
    val contentTranslateX = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(animating) {
        if (animating) {
            // 并行编排:checkbox 立即弹,内容延迟 110ms 后渐隐右移
            kotlinx.coroutines.coroutineScope {
                launch {
                    checkScale.snapTo(0.6f)
                    checkmarkAlpha.snapTo(0f)
                    launch { checkmarkAlpha.animateTo(1f, tween(180)) }
                    checkScale.animateTo(
                        1.15f,
                        spring(dampingRatio = 0.4f, stiffness = 480f)
                    )
                    checkScale.animateTo(
                        1f,
                        spring(dampingRatio = 0.7f, stiffness = 360f)
                    )
                }
                launch {
                    kotlinx.coroutines.delay(110)
                    launch { contentAlpha.animateTo(0.45f, tween(240)) }
                    contentTranslateX.animateTo(6f, tween(240))
                }
            }
        } else {
            // 重置（动画结束后 todo.done 已为 true,VM 会把它移到已完成区,
            // 但若是从已完成区取消勾选,需要把内容透明度/位移复位）
            checkScale.snapTo(1f)
            checkmarkAlpha.snapTo(if (todo.done) 1f else 0f)
            contentAlpha.snapTo(1f)
            contentTranslateX.snapTo(0f)
        }
    }

    val displayDone = todo.done || animating
    val displayStrike = strikeThrough || animating

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
                    .graphicsLayer {
                        scaleX = checkScale.value
                        scaleY = checkScale.value
                    }
                    .background(
                        if (displayDone) AppColors.DoneGreen
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                    .clickable(enabled = !animating) { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (displayDone) {
                    Text(
                        "✓",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.graphicsLayer { alpha = checkmarkAlpha.value }
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // Content
        Column(
            Modifier
                .weight(1f)
                .graphicsLayer {
                    alpha = contentAlpha.value
                    translationX = contentTranslateX.value * density
                }
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
            // 无日期任务且来源不是日历 → subtitle 为空，整行不渲染，避免空白
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
}

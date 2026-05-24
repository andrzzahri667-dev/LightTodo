package com.zahri.lighttodo.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zahri.lighttodo.R
import com.zahri.lighttodo.ui.home.note.NoteGridPage
import com.zahri.lighttodo.ui.motion.AppMotion
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
    onNoteEdit: (Long?, Rect?) -> Unit,
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
                val noteSourceBounds = remember { HomeNoteSourceBounds() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.92f else 1f,
                    animationSpec = AppMotion.pressSpring(),
                    label = "fab-scale"
                )
                FloatingActionButton(
                    onClick = {
                        when (currentPage) {
                            HomePagerPages.NOTE -> onNoteEdit(null, noteSourceBounds.bounds)
                            HomePagerPages.TODO -> onAdd()
                        }
                    },
                    containerColor = AppColors.Brand,
                    shape = CircleShape,
                    interactionSource = interaction,
                    modifier = Modifier
                        .size(56.dp)
                        .onGloballyPositioned { noteSourceBounds.bounds = it.boundsInRoot() }
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_add), tint = Color.Black)
                }
            }
        },
        bottomBar = {
            // Todo selection bar
            AnimatedVisibility(
                visible = inSelection && currentPage == HomePagerPages.TODO,
                enter = AppMotion.transientSurfaceEnter(),
                exit = AppMotion.transientSurfaceExit()
            ) {
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
            AnimatedVisibility(
                visible = noteInSelection && currentPage == HomePagerPages.NOTE,
                enter = AppMotion.transientSurfaceEnter(),
                exit = AppMotion.transientSurfaceExit()
            ) {
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
                        onNoteClick = { id, sourceBounds ->
                            if (noteInSelection) vm.toggleNoteSelection(id)
                            else onNoteEdit(id, sourceBounds)
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

private class HomeNoteSourceBounds {
    var bounds: Rect? = null
}

package com.zahri.lighttodo.ui.home.note

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zahri.lighttodo.R
import com.zahri.lighttodo.data.NoteEntity
import com.zahri.lighttodo.ui.motion.AppMotion
import com.zahri.lighttodo.ui.note.NoteSourceAnimationKey
import com.zahri.lighttodo.ui.theme.AppColors
import com.zahri.lighttodo.ui.theme.AppType

private val CARD_HEIGHT = 160.dp
private const val GRID_COLUMNS = 2

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteGridPage(
    notes: List<NoteEntity>,
    selectedIds: Set<Long>,
    hiddenNoteSource: NoteSourceAnimationKey?,
    backgroundColor: Color,
    onNoteClick: (Long, Rect?) -> Unit,
    onNoteLongClick: (Long) -> Unit
) {
    val noteItems = remember(notes) { buildNoteGridItems(notes) }

    if (notes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📝", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.note_empty),
                    style = AppType.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp
    ) {
        items(noteItems, key = { it.id }) { note ->
            NoteCard(
                note = note,
                selected = note.id in selectedIds,
                hidden = hiddenNoteSource?.matches(note.id) == true,
                onClick = { sourceBounds -> onNoteClick(note.id, sourceBounds) },
                onLongClick = { onNoteLongClick(note.id) },
                modifier = Modifier.animateItemPlacement(
                    animationSpec = AppMotion.noteGridPlacementSpring()
                )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: NoteGridItem,
    selected: Boolean,
    hidden: Boolean,
    onClick: (Rect?) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sourceBounds = remember { NoteSourceBounds() }
    val cardBackground by animateColorAsState(
        targetValue = if (selected) AppColors.Brand.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(AppMotion.NoteCardSelectionColorMillis),
        label = "note-card-selection-bg"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(CARD_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackground)
            .onGloballyPositioned { sourceBounds.bounds = it.boundsInRoot() }
            .graphicsLayer { alpha = if (hidden) 0f else 1f }
            .combinedClickable(onLongClick = onLongClick, onClick = { onClick(sourceBounds.bounds) })
            .padding(12.dp)
    ) {
        if (note.title != null) {
            Text(
                text = note.title,
                style = AppType.headline,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
        }
        if (note.preview != null) {
            Text(
                text = note.preview,
                style = AppType.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = note.previewMaxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        } else if (note.showEmptyPlaceholder) {
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.note_no_title),
                style = AppType.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

private class NoteSourceBounds {
    var bounds: Rect? = null
}

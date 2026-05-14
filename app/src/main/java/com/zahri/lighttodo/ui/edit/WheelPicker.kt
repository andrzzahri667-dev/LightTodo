package com.zahri.lighttodo.ui.edit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * A generic vertical scroll-wheel picker.
 *
 * @param items list of display strings
 * @param selectedIndex the currently selected index
 * @param onSelectedChanged called when user scrolls to a new item
 * @param itemHeight height of each item row
 * @param visibleCount number of visible rows (should be odd, e.g. 3 or 5)
 * @param selectedColor text color for the selected item
 * @param unselectedColor text color for non-selected items
 * @param selectedFontSize font size for selected item
 * @param unselectedFontSize font size for non-selected items
 * @param suffix optional suffix displayed next to the selected item (e.g. "H", "M")
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 48.dp,
    visibleCount: Int = 3,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    selectedFontSize: TextUnit = 22.sp,
    unselectedFontSize: TextUnit = 16.sp,
    suffix: String = ""
) {
    val halfVisible = visibleCount / 2
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex
    )

    val totalHeight = itemHeight * visibleCount

    // Detect which item is centered
    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset +
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
            val closestItem = layoutInfo.visibleItemsInfo.minByOrNull {
                kotlin.math.abs((it.offset + it.size / 2) - viewportCenter)
            }
            // Subtract padding items
            (closestItem?.index ?: (selectedIndex + halfVisible)) - halfVisible
        }
    }

    // Notify parent when centered index changes
    LaunchedEffect(Unit) {
        snapshotFlow { centeredIndex }
            .distinctUntilChanged()
            .collect { idx ->
                if (idx in items.indices) {
                    onSelectedChanged(idx)
                }
            }
    }

    // Scroll to selected index when it changes externally
    LaunchedEffect(selectedIndex) {
        if (centeredIndex != selectedIndex) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Box(
        modifier = modifier.height(totalHeight),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .height(totalHeight)
                .fillMaxWidth(),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            // Top padding items
            items(halfVisible) {
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                )
            }

            // Actual items
            items(items.size) { index ->
                val isSelected = index == centeredIndex
                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.5f,
                    label = "alpha"
                )
                val fontSize = if (isSelected) selectedFontSize else unselectedFontSize
                val color = if (isSelected) selectedColor else unselectedColor
                val weight = if (isSelected) FontWeight.Bold else FontWeight.Normal

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .alpha(alpha),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSelected && suffix.isNotEmpty()) "${items[index]}$suffix" else items[index],
                        color = color,
                        fontSize = fontSize,
                        fontWeight = weight,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom padding items
            items(halfVisible) {
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                )
            }
        }
    }
}

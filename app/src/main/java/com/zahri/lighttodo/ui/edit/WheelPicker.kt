package com.zahri.lighttodo.ui.edit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zahri.lighttodo.ui.motion.AppMotion
import com.zahri.lighttodo.ui.motion.WheelPickerMotionPolicy
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * A vertical scroll-wheel picker with circular/infinite scrolling.
 *
 * Returns the current logical index synchronously so callers can read it in click handlers.
 *
 * @param items list of display strings
 * @param selectedIndex initial / externally-controlled index
 * @param onSelectedChanged called when the centered item changes
 * @param loopThreshold items.size <= this → enable circular looping
 * @param loopRepetitions how many times to repeat the list (odd, large)
 */
@OptIn(ExperimentalFoundationApi::class)
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
    superscript: String = "",
    loopThreshold: Int = 60,
    loopRepetitions: Int = 501
): Int {
    val halfVisible = visibleCount / 2
    val looping = items.size in 2..loopThreshold
    val totalItems = if (looping) items.size * loopRepetitions else items.size

    val mappedInitial = if (looping) {
        selectedIndex + items.size * (loopRepetitions / 2)
    } else {
        selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = mappedInitial
    )

    val totalHeight = itemHeight * visibleCount
    val density = LocalDensity.current
    val proximityRadiusPx = with(density) {
        itemHeight.toPx() * AppMotion.PickerItemProximityRadiusItems
    }

    // Absolute centered index in the expanded list
    val centeredAbsIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset +
                    (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
            val closestItem = layoutInfo.visibleItemsInfo.minByOrNull {
                kotlin.math.abs((it.offset + it.size / 2) - viewportCenter)
            }
            ((closestItem?.index ?: (mappedInitial + halfVisible)) - halfVisible)
                .let { if (looping) it.coerceIn(0, totalItems - 1) else it.coerceIn(0, (items.size - 1).coerceAtLeast(0)) }
        }
    }

    // Map absolute → logical index — MUST be derivedStateOf so snapshotFlow can observe it
    val logicalIndex by remember {
        derivedStateOf {
            val abs = centeredAbsIndex
            if (looping) ((abs % items.size) + items.size) % items.size else abs
        }
    }

    // Notify parent
    LaunchedEffect(Unit) {
        snapshotFlow { logicalIndex }
            .distinctUntilChanged()
            .collect { idx ->
                if (idx in items.indices) {
                    onSelectedChanged(idx)
                }
            }
    }

    // External scroll with spring animation
    LaunchedEffect(selectedIndex) {
        if (logicalIndex != selectedIndex && selectedIndex in items.indices) {
            val target = if (looping) {
                val currentAbs = centeredAbsIndex
                val currentLoop = currentAbs / items.size
                val targetBase = selectedIndex + items.size * currentLoop
                listOf(targetBase, targetBase - items.size, targetBase + items.size)
                    .minByOrNull { kotlin.math.abs(it - currentAbs) }
                    ?: targetBase
            } else selectedIndex
            listState.animateScrollToItem(target)
        }
    }

    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    Box(
        modifier = modifier.height(totalHeight),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .height(totalHeight)
                .fillMaxWidth(),
            flingBehavior = flingBehavior
        ) {
            items(halfVisible) {
                Box(Modifier.height(itemHeight).fillMaxWidth())
            }

            items(totalItems) { absIndex ->
                val logicalIdx = if (looping) {
                    ((absIndex % items.size) + items.size) % items.size
                } else absIndex

                val isSelected = absIndex == centeredAbsIndex
                val distanceFromCenter by remember(absIndex) {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val viewportCenter = layoutInfo.viewportStartOffset +
                            (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f
                        val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == absIndex + halfVisible }
                        if (itemInfo == null) {
                            Float.POSITIVE_INFINITY
                        } else {
                            kotlin.math.abs((itemInfo.offset + itemInfo.size / 2f) - viewportCenter)
                        }
                    }
                }
                val proximity = WheelPickerMotionPolicy.proximityForDistance(distanceFromCenter, proximityRadiusPx)
                val targetAlpha = WheelPickerMotionPolicy.alphaForProximity(proximity)
                val targetScale = WheelPickerMotionPolicy.scaleForProximity(proximity)
                val alpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(AppMotion.PickerItemAlphaMillis),
                    label = "wheel-picker-item-alpha"
                )
                val scale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = tween(AppMotion.PickerItemScaleMillis),
                    label = "wheel-picker-item-scale"
                )
                val fontSize = if (isSelected) selectedFontSize else unselectedFontSize
                val color = if (isSelected) selectedColor else unselectedColor
                val weight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .graphicsLayer {
                            this.alpha = alpha
                            scaleX = scale
                            scaleY = scale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected && superscript.isNotEmpty()) {
                        Text(
                            text = buildAnnotatedString {
                                append(items[logicalIdx])
                                withStyle(
                                    SpanStyle(
                                        fontSize = fontSize.value.times(0.45f).sp,
                                        baselineShift = BaselineShift.Superscript
                                    )
                                ) { append(superscript) }
                            },
                            color = color,
                            fontSize = fontSize,
                            fontWeight = weight,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = items[logicalIdx],
                            color = color,
                            fontSize = fontSize,
                            fontWeight = weight,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(halfVisible) {
                Box(Modifier.height(itemHeight).fillMaxWidth())
            }
        }
    }

    // Return current logical index for synchronous reads
    return logicalIndex
}

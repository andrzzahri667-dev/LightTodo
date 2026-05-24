package com.zahri.lighttodo.ui.note

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class NoteRouteTransitionSpec(
    val transformOrigin: TransformOrigin,
    val sourceCenterOffset: IntOffset,
    val sourceScale: Float
)

object NoteRouteTransitionPolicy {
    private const val FALLBACK_SCALE = 0.985f
    private const val MIN_SOURCE_SCALE = 0.16f
    private const val MAX_SOURCE_SCALE = 0.56f

    fun specFor(sourceBounds: Rect?, rootSize: IntSize): NoteRouteTransitionSpec {
        val rootWidth = rootSize.width.toFloat()
        val rootHeight = rootSize.height.toFloat()
        if (sourceBounds == null ||
            rootWidth <= 0f ||
            rootHeight <= 0f ||
            sourceBounds.width <= 0f ||
            sourceBounds.height <= 0f
        ) {
            return NoteRouteTransitionSpec(
                transformOrigin = TransformOrigin.Center,
                sourceCenterOffset = IntOffset.Zero,
                sourceScale = FALLBACK_SCALE
            )
        }

        val sourceCenterOffset = IntOffset(
            x = (sourceBounds.center.x - rootWidth / 2f).roundToInt(),
            y = (sourceBounds.center.y - rootHeight / 2f).roundToInt()
        )
        val sourceScale = sqrt(
            (sourceBounds.width / rootWidth) * (sourceBounds.height / rootHeight)
        ).coerceIn(MIN_SOURCE_SCALE, MAX_SOURCE_SCALE)

        return NoteRouteTransitionSpec(
            transformOrigin = TransformOrigin.Center,
            sourceCenterOffset = sourceCenterOffset,
            sourceScale = sourceScale
        )
    }
}

package com.zahri.lighttodo.ui.note

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

enum class NoteContainerTransformDirection {
    Enter,
    Exit
}

object NoteContainerTransformPolicy {
    private const val SourceCornerRadius = 12f
    private const val ContentFadeStart = 0.7f

    fun boundsAt(progress: Float, sourceBounds: Rect, rootSize: IntSize): Rect {
        val p = progress.coerceIn(0f, 1f)
        return Rect(
            left = lerp(sourceBounds.left, 0f, p),
            top = lerp(sourceBounds.top, 0f, p),
            right = lerp(sourceBounds.right, rootSize.width.toFloat(), p),
            bottom = lerp(sourceBounds.bottom, rootSize.height.toFloat(), p)
        )
    }

    fun cornerRadiusAt(progress: Float): Float =
        lerp(SourceCornerRadius, 0f, progress.coerceIn(0f, 1f))

    fun overlayAlphaAt(direction: NoteContainerTransformDirection, progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return when (direction) {
            NoteContainerTransformDirection.Enter -> 1f - ((p - ContentFadeStart) / (1f - ContentFadeStart)).coerceIn(0f, 1f)
            NoteContainerTransformDirection.Exit -> ((1f - p) / (1f - ContentFadeStart)).coerceIn(0f, 1f)
        }
    }

    private fun lerp(start: Float, end: Float, progress: Float): Float =
        start + (end - start) * progress
}

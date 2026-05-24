package com.zahri.lighttodo.ui.note

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

enum class NoteContainerTransformDirection {
    Enter,
    Exit
}

object NoteContainerTransformPolicy {
    const val DefaultSourceCornerRadius = 12f

    private const val ExitSurfaceFadeEnd = 0.2f
    private const val SourceSnapshotFadeStart = 0.24f
    private const val SourceSnapshotFadeEnd = 0.56f

    fun boundsAt(progress: Float, sourceBounds: Rect, rootSize: IntSize): Rect {
        val p = progress.coerceIn(0f, 1f)
        return Rect(
            left = lerp(sourceBounds.left, 0f, p),
            top = lerp(sourceBounds.top, 0f, p),
            right = lerp(sourceBounds.right, rootSize.width.toFloat(), p),
            bottom = lerp(sourceBounds.bottom, rootSize.height.toFloat(), p)
        )
    }

    fun cornerRadiusAt(progress: Float, sourceRadius: Float = DefaultSourceCornerRadius): Float =
        lerp(sourceRadius, 0f, progress.coerceIn(0f, 1f))

    fun overlayAlphaAt(direction: NoteContainerTransformDirection, progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return when (direction) {
            NoteContainerTransformDirection.Enter -> 1f
            NoteContainerTransformDirection.Exit -> (p / ExitSurfaceFadeEnd).coerceIn(0f, 1f)
        }
    }

    fun sourceSnapshotAlphaAt(direction: NoteContainerTransformDirection, progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return when (direction) {
            NoteContainerTransformDirection.Enter -> {
                1f - ((p - SourceSnapshotFadeStart) / (SourceSnapshotFadeEnd - SourceSnapshotFadeStart))
                    .coerceIn(0f, 1f)
            }
            NoteContainerTransformDirection.Exit -> overlayAlphaAt(direction, p)
        }
    }

    private fun lerp(start: Float, end: Float, progress: Float): Float =
        start + (end - start) * progress
}

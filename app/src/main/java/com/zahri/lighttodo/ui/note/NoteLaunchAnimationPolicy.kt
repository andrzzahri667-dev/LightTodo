package com.zahri.lighttodo.ui.note

import kotlin.math.roundToInt

enum class NoteLaunchSnapshotMode {
    CaptureSource,
    SolidColor
}

data class NoteLaunchAnimationSpec(
    val snapshotMode: NoteLaunchSnapshotMode,
    val miuiAnimationType: Int,
    val sourceCornerRadiusPx: Int,
    val sourceScale: Float
)

object NoteLaunchAnimationPolicy {
    const val AnimLaunchActivityFromRoundedView = 102
    const val AnimLaunchActivityWithScaledThumb = 103

    private const val CardCornerRadiusDp = 12f

    fun specFor(
        noteId: Long?,
        sourceWidthPx: Float,
        density: Float,
        sourceScale: Float = 1f
    ): NoteLaunchAnimationSpec {
        val safeDensity = density.takeIf { it > 0f } ?: 1f
        val safeSourceScale = sourceScale.takeIf { it > 0f }?.coerceIn(0.1f, 2f) ?: 1f
        return if (noteId == null) {
            NoteLaunchAnimationSpec(
                snapshotMode = NoteLaunchSnapshotMode.SolidColor,
                miuiAnimationType = AnimLaunchActivityFromRoundedView,
                sourceCornerRadiusPx = (sourceWidthPx / 2f).roundToInt().coerceAtLeast(1),
                sourceScale = safeSourceScale
            )
        } else {
            NoteLaunchAnimationSpec(
                snapshotMode = NoteLaunchSnapshotMode.CaptureSource,
                miuiAnimationType = AnimLaunchActivityWithScaledThumb,
                sourceCornerRadiusPx = (CardCornerRadiusDp * safeDensity).roundToInt().coerceAtLeast(1),
                sourceScale = safeSourceScale
            )
        }
    }
}

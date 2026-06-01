package com.zahri.lighttodo.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import kotlin.math.max

data class NoteEditorContainerTransformFrame(
    val scaleX: Float,
    val scaleY: Float,
    val translationX: Float,
    val translationY: Float,
    val cornerRadiusPx: Float
)

data class NoteEditorTransformContentAlpha(
    val sourcePreviewAlpha: Float,
    val editorAlpha: Float
)

data class NoteEditorTransitionBounds(
    val screenLeft: Int,
    val screenTop: Int,
    val width: Int,
    val height: Int,
    val cornerRadiusPx: Int
)

data class NoteEditorContainerTransformEasing(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
)

enum class NoteEditorTransformContentPhase {
    SourcePreview,
    Editor
}

object NoteEditorContainerTransformPolicy {
    const val EntryDurationMillis = 720
    const val ExitDurationMillis = 540
    const val SourceRevealAfterEntryDelayMillis = 40
    const val ContentSwitchProgress = 0.5f
    const val ContentCrossfadeStartProgress = 0.4f
    const val ContentCrossfadeEndProgress = 0.6f

    val EntryEasing = NoteEditorContainerTransformEasing(0.4f, 0f, 0.2f, 1f)
    val ExitEasing = NoteEditorContainerTransformEasing(0.4f, 0f, 0.2f, 1f)

    fun shouldRender(rootWidth: Int, rootHeight: Int): Boolean =
        rootWidth > 0 && rootHeight > 0

    fun entryTween(): TweenSpec<Float> =
        tween(
            durationMillis = EntryDurationMillis,
            easing = EntryEasing.toComposeEasing()
        )

    fun exitTween(): TweenSpec<Float> =
        tween(
            durationMillis = ExitDurationMillis,
            easing = ExitEasing.toComposeEasing()
        )

    fun contentPhaseFor(progress: Float): NoteEditorTransformContentPhase {
        val safeProgress = safeProgress(progress)
        return if (safeProgress < ContentSwitchProgress) {
            NoteEditorTransformContentPhase.SourcePreview
        } else {
            NoteEditorTransformContentPhase.Editor
        }
    }

    fun contentAlphaFor(progress: Float): NoteEditorTransformContentAlpha {
        val safeProgress = safeProgress(progress)
        val rawFadeProgress = (
            (safeProgress - ContentCrossfadeStartProgress) /
                (ContentCrossfadeEndProgress - ContentCrossfadeStartProgress)
            ).coerceIn(0f, 1f)
        val fadeProgress = smoothStep(rawFadeProgress)
        return NoteEditorTransformContentAlpha(
            sourcePreviewAlpha = 1f - fadeProgress,
            editorAlpha = fadeProgress
        )
    }

    fun geometryProgressFor(progress: Float): Float {
        val t = safeProgress(progress)
        return t * t * t * (t * (t * 6f - 15f) + 10f)
    }

    fun frameFor(
        rootWidth: Int,
        rootHeight: Int,
        sourceLeft: Int,
        sourceTop: Int,
        sourceWidth: Int,
        sourceHeight: Int,
        sourceCornerRadiusPx: Int,
        progress: Float
    ): NoteEditorContainerTransformFrame {
        val safeProgress = geometryProgressFor(progress)
        val safeRootWidth = max(rootWidth, 1).toFloat()
        val safeRootHeight = max(rootHeight, 1).toFloat()
        val startScaleX = max(sourceWidth, 1) / safeRootWidth
        val startScaleY = max(sourceHeight, 1) / safeRootHeight
        return NoteEditorContainerTransformFrame(
            scaleX = lerp(startScaleX, 1f, safeProgress),
            scaleY = lerp(startScaleY, 1f, safeProgress),
            translationX = lerp(sourceLeft.toFloat(), 0f, safeProgress),
            translationY = lerp(sourceTop.toFloat(), 0f, safeProgress),
            cornerRadiusPx = lerp(sourceCornerRadiusPx.toFloat(), 0f, safeProgress).coerceAtLeast(0f)
        )
    }

    fun sourcePreviewFrameFor(
        rootWidth: Int,
        rootHeight: Int,
        sourceLeft: Int,
        sourceTop: Int,
        sourceWidth: Int,
        sourceHeight: Int,
        sourceCornerRadiusPx: Int,
        progress: Float
    ): NoteEditorContainerTransformFrame {
        val containerFrame = frameFor(
            rootWidth = rootWidth,
            rootHeight = rootHeight,
            sourceLeft = sourceLeft,
            sourceTop = sourceTop,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            sourceCornerRadiusPx = sourceCornerRadiusPx,
            progress = progress
        )
        return sourcePreviewFrameFor(
            containerFrame = containerFrame,
            rootWidth = rootWidth,
            rootHeight = rootHeight,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight
        )
    }

    fun sourcePreviewFrameFor(
        containerFrame: NoteEditorContainerTransformFrame,
        rootWidth: Int,
        rootHeight: Int,
        sourceWidth: Int,
        sourceHeight: Int
    ): NoteEditorContainerTransformFrame {
        val safeRootWidth = max(rootWidth, 1).toFloat()
        val safeRootHeight = max(rootHeight, 1).toFloat()
        val startScaleX = max(sourceWidth, 1) / safeRootWidth
        val startScaleY = max(sourceHeight, 1) / safeRootHeight
        val previewScaleX = containerFrame.scaleX / startScaleX
        val previewScaleY = containerFrame.scaleY / startScaleY
        val cornerScale = max(previewScaleX, previewScaleY).coerceAtLeast(0.0001f)

        return NoteEditorContainerTransformFrame(
            scaleX = previewScaleX,
            scaleY = previewScaleY,
            translationX = containerFrame.translationX,
            translationY = containerFrame.translationY,
            cornerRadiusPx = (containerFrame.cornerRadiusPx / cornerScale).coerceAtLeast(0f)
        )
    }

    private fun safeProgress(progress: Float): Float =
        if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)

    private fun smoothStep(progress: Float): Float =
        progress * progress * (3f - 2f * progress)

    private fun lerp(start: Float, end: Float, progress: Float): Float =
        start + (end - start) * progress

    private fun NoteEditorContainerTransformEasing.toComposeEasing(): Easing =
        CubicBezierEasing(x1, y1, x2, y2)
}

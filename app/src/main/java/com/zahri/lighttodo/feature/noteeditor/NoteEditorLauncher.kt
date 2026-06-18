package com.zahri.lighttodo.feature.noteeditor

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.geometry.Rect
import com.zahri.lighttodo.ui.motion.NoteEditorContainerTransformPolicy
import com.zahri.lighttodo.ui.motion.NoteEditorTransitionBounds
import kotlin.math.roundToInt

object NoteEditorLauncher {
    private const val MaxSourceSnapshotPixels = 700_000

    fun launch(
        activity: Activity,
        rootView: View,
        noteId: Long?,
        launchSeed: NoteEditLaunchSeed? = null,
        sourceBounds: Rect?,
        density: Float,
        sourceScale: Float,
        targetBackgroundColor: Int,
        createSourceColor: Int,
        onSourceHiddenChange: (Boolean) -> Unit
    ): NoteEditorLaunchResult {
        val intent = NoteEditActivity.intent(activity, noteId, launchSeed)
        val launchBounds = sourceBounds?.toLaunchBounds(rootView)
        if (launchBounds == null) {
            activity.startActivity(intent)
            return NoteEditorLaunchResult(miuiReturnAnimationPrepared = false)
        }

        val spec = NoteLaunchAnimationPolicy.specFor(
            noteId = noteId,
            sourceWidthPx = launchBounds.width.toFloat(),
            density = density,
            sourceScale = sourceScale
        )
        val miuiScaleUpDownSupported = MiuiScaleUpDownOptions.isSupported()
        val snapshot = if (miuiScaleUpDownSupported) {
            when (spec.snapshotMode) {
                NoteLaunchSnapshotMode.CaptureSource ->
                    rootView.captureSourceSnapshot(launchBounds) ?: solidBitmap(launchBounds, targetBackgroundColor)
                NoteLaunchSnapshotMode.SolidColor ->
                    solidBitmap(launchBounds, createSourceColor)
            }
        } else {
            null
        }
        val transitionColor = if (noteId == null) createSourceColor else targetBackgroundColor

        val miuiReturnAnimationPrepared: Boolean
        var sourceHiddenBeforeLaunch = false
        var snapshotHandedToSystem = false
        try {
            val miuiOptions = if (miuiScaleUpDownSupported) {
                MiuiScaleUpDownOptions.makeBundle(
                    anchor = rootView,
                    snapshot = snapshot,
                    screenX = launchBounds.screenX(rootView),
                    screenY = launchBounds.screenY(rootView),
                    radiusPx = spec.sourceCornerRadiusPx,
                    targetColor = transitionColor,
                    sourceScale = spec.sourceScale,
                    animationType = spec.miuiAnimationType,
                    onSourceHiddenChange = onSourceHiddenChange
                )
            } else {
                null
            }
            val launchMode = NoteEditorLaunchAnimationModePolicy.modeFor(
                miuiOptionsAvailable = miuiOptions != null,
                sourceBoundsAvailable = true
            )
            NoteEditActivity.setLaunchAnimationMode(intent, launchMode)
            NoteEditActivity.setTransitionBounds(
                intent,
                launchBounds.toTransitionBounds(rootView, spec.sourceCornerRadiusPx)
            )
            if (launchMode == NoteEditorLaunchAnimationMode.CustomContainerTransform) {
                sourceHiddenBeforeLaunch = true
                onSourceHiddenChange(true)
            }
            snapshotHandedToSystem = miuiOptions != null
            miuiReturnAnimationPrepared = miuiOptions != null

            when (launchMode) {
                NoteEditorLaunchAnimationMode.MiuiSystemScaleUpDown -> {
                    activity.startActivity(intent, miuiOptions)
                }
                NoteEditorLaunchAnimationMode.CustomContainerTransform -> {
                    activity.startActivity(intent)
                    activity.disablePendingTransition()
                    rootView.postDelayed(
                        { onSourceHiddenChange(false) },
                        (
                            NoteEditorContainerTransformPolicy.EntryDurationMillis +
                                NoteEditorContainerTransformPolicy.SourceRevealAfterEntryDelayMillis
                            ).toLong()
                    )
                }
                NoteEditorLaunchAnimationMode.Plain -> {
                    activity.startActivity(intent)
                }
            }
        } catch (throwable: RuntimeException) {
            if (sourceHiddenBeforeLaunch) {
                onSourceHiddenChange(false)
            }
            throw throwable
        } finally {
            if (!snapshotHandedToSystem) {
                snapshot?.recycle()
            }
        }
        return NoteEditorLaunchResult(miuiReturnAnimationPrepared = miuiReturnAnimationPrepared)
    }

    fun disableScaleDownAnimation(activity: Activity): Boolean =
        MiuiScaleUpDownOptions.disableScaleDownAnimation(activity)

    data class NoteEditorLaunchResult(
        val miuiReturnAnimationPrepared: Boolean
    )

    private fun View.captureSourceSnapshot(bounds: LaunchBounds): Bitmap? {
        if (bounds.width.toLong() * bounds.height.toLong() > MaxSourceSnapshotPixels) return null
        return runCatching {
            val bitmap = Bitmap.createBitmap(
                resources.displayMetrics,
                bounds.width,
                bounds.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.translate(-bounds.left.toFloat(), -bounds.top.toFloat())
            draw(canvas)
            canvas.setBitmap(null)
            bitmap
        }.getOrNull()
    }

    private fun solidBitmap(bounds: LaunchBounds, color: Int): Bitmap? =
        runCatching {
            Bitmap.createBitmap(bounds.width, bounds.height, Bitmap.Config.ARGB_8888).apply {
                eraseColor(color)
            }
        }.getOrNull()

    private fun Rect.toLaunchBounds(rootView: View): LaunchBounds? {
        if (rootView.width <= 0 || rootView.height <= 0) return null
        val left = left.roundToInt().coerceIn(0, rootView.width - 1)
        val top = top.roundToInt().coerceIn(0, rootView.height - 1)
        val right = right.roundToInt().coerceIn(left + 1, rootView.width)
        val bottom = bottom.roundToInt().coerceIn(top + 1, rootView.height)
        val width = right - left
        val height = bottom - top
        return if (width > 0 && height > 0) {
            LaunchBounds(left = left, top = top, width = width, height = height)
        } else {
            null
        }
    }

    private data class LaunchBounds(
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int
    ) {
        fun screenX(rootView: View): Int = rootView.screenLocation()[0] + left

        fun screenY(rootView: View): Int = rootView.screenLocation()[1] + top

        fun toTransitionBounds(rootView: View, cornerRadiusPx: Int): NoteEditorTransitionBounds =
            NoteEditorTransitionBounds(
                screenLeft = screenX(rootView),
                screenTop = screenY(rootView),
                width = width,
                height = height,
                cornerRadiusPx = cornerRadiusPx
            )
    }

    private fun View.screenLocation(): IntArray =
        IntArray(2).also { getLocationOnScreen(it) }

    @Suppress("DEPRECATION")
    private fun Activity.disablePendingTransition() {
        overridePendingTransition(0, 0)
    }

}

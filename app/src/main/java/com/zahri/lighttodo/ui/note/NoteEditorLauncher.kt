package com.zahri.lighttodo.ui.note

import android.app.Activity
import android.app.ActivityOptions
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.compose.ui.geometry.Rect
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
                    boundsInRoot = launchBounds,
                    spec = spec,
                    targetColor = transitionColor,
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

    private object MiuiScaleUpDownOptions {
        private val mainHandler by lazy(LazyThreadSafetyMode.NONE) { Handler(Looper.getMainLooper()) }
        private val updateScaleUpDownDataMethod by lazy(LazyThreadSafetyMode.NONE) {
            runCatching {
                Activity::class.java.getDeclaredMethod(
                    "updateScaleUpDownData",
                    Bundle::class.java
                ).apply { isAccessible = true }
            }.getOrNull()
        }
        private val scaleUpDownMethod by lazy(LazyThreadSafetyMode.NONE) {
            runCatching {
                ActivityOptions::class.java.getMethod(
                    "makeScaleUpDown",
                    View::class.java,
                    Bitmap::class.java,
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE,
                    java.lang.Float.TYPE,
                    Handler::class.java,
                    Runnable::class.java,
                    Runnable::class.java,
                    Runnable::class.java,
                    Runnable::class.java,
                    Integer.TYPE
                ).apply { isAccessible = true }
            }.getOrNull()
        }
        private val roundedViewMethod by lazy(LazyThreadSafetyMode.NONE) {
            runCatching {
                ActivityOptions::class.java.getMethod(
                    "makeScaleUpAnimationFromRoundedView",
                    View::class.java,
                    Bitmap::class.java,
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE,
                    java.lang.Float.TYPE,
                    Handler::class.java,
                    Runnable::class.java,
                    Runnable::class.java,
                    Runnable::class.java,
                    Runnable::class.java
                ).apply { isAccessible = true }
            }.getOrNull()
        }

        fun isSupported(): Boolean =
            scaleUpDownMethod != null || roundedViewMethod != null

        fun makeBundle(
            anchor: View,
            snapshot: Bitmap?,
            boundsInRoot: LaunchBounds,
            spec: NoteLaunchAnimationSpec,
            targetColor: Int,
            onSourceHiddenChange: (Boolean) -> Unit
        ): Bundle? {
            if (snapshot == null) return null
            val screenX = boundsInRoot.screenX(anchor)
            val screenY = boundsInRoot.screenY(anchor)
            val onSourceExitStart = sourceVisibilityCallback(
                phase = NoteSourceVisibilityCallbackPhase.SourceExitStarted,
                onSourceHiddenChange = onSourceHiddenChange
            )
            val onSourceExitEnd = sourceVisibilityCallback(
                phase = NoteSourceVisibilityCallbackPhase.SourceExitFinished,
                onSourceHiddenChange = onSourceHiddenChange
            )
            val onSourceReenterStart = sourceVisibilityCallback(
                phase = NoteSourceVisibilityCallbackPhase.SourceReenterStarted,
                onSourceHiddenChange = onSourceHiddenChange
            )
            val onSourceReenterEnd = sourceVisibilityCallback(
                phase = NoteSourceVisibilityCallbackPhase.SourceReenterFinished,
                onSourceHiddenChange = onSourceHiddenChange
            )

            return makeScaleUpDown(
                anchor = anchor,
                snapshot = snapshot,
                screenX = screenX,
                screenY = screenY,
                radiusPx = spec.sourceCornerRadiusPx,
                targetColor = targetColor,
                sourceScale = spec.sourceScale,
                animationType = spec.miuiAnimationType,
                onLaunchStart = onSourceExitStart,
                onLaunchEnd = onSourceExitEnd,
                onReturnStart = onSourceReenterStart,
                onReturnEnd = onSourceReenterEnd
            ) ?: makeScaleUpAnimationFromRoundedView(
                anchor = anchor,
                snapshot = snapshot,
                screenX = screenX,
                screenY = screenY,
                radiusPx = spec.sourceCornerRadiusPx,
                targetColor = targetColor,
                sourceScale = spec.sourceScale,
                onLaunchStart = onSourceExitStart,
                onLaunchEnd = onSourceExitEnd,
                onReturnStart = onSourceReenterStart,
                onReturnEnd = onSourceReenterEnd
            )
        }

        fun disableScaleDownAnimation(activity: Activity): Boolean =
            runCatching {
                val method = updateScaleUpDownDataMethod ?: return false
                method.invoke(
                    activity,
                    Bundle().apply {
                        putBoolean("disableBackAnimation", true)
                    }
                )
                true
            }.getOrDefault(false)

        private fun makeScaleUpDown(
            anchor: View,
            snapshot: Bitmap,
            screenX: Int,
            screenY: Int,
            radiusPx: Int,
            targetColor: Int,
            sourceScale: Float,
            animationType: Int,
            onLaunchStart: Runnable,
            onLaunchEnd: Runnable,
            onReturnStart: Runnable,
            onReturnEnd: Runnable
        ): Bundle? =
            runCatching {
                val method = scaleUpDownMethod ?: return null
                val options = method.invoke(
                    null,
                    anchor,
                    snapshot,
                    screenX,
                    screenY,
                    radiusPx,
                    targetColor,
                    sourceScale,
                    mainHandler,
                    onLaunchStart,
                    onLaunchEnd,
                    onReturnStart,
                    onReturnEnd,
                    animationType
                ) as? ActivityOptions
                options?.toBundle()
            }.getOrNull()

        private fun makeScaleUpAnimationFromRoundedView(
            anchor: View,
            snapshot: Bitmap,
            screenX: Int,
            screenY: Int,
            radiusPx: Int,
            targetColor: Int,
            sourceScale: Float,
            onLaunchStart: Runnable,
            onLaunchEnd: Runnable,
            onReturnStart: Runnable,
            onReturnEnd: Runnable
        ): Bundle? =
            runCatching {
                val method = roundedViewMethod ?: return null
                val options = method.invoke(
                    null,
                    anchor,
                    snapshot,
                    screenX,
                    screenY,
                    radiusPx,
                    targetColor,
                    sourceScale,
                    mainHandler,
                    onLaunchStart,
                    onLaunchEnd,
                    onReturnStart,
                    onReturnEnd
                ) as? ActivityOptions
                options?.toBundle()
            }.getOrNull()

        private fun sourceVisibilityCallback(
            phase: NoteSourceVisibilityCallbackPhase,
            onSourceHiddenChange: (Boolean) -> Unit
        ): Runnable =
            Runnable {
                when (NoteSourceVisibilityCallbackPolicy.actionFor(phase)) {
                    NoteSourceVisibilityAction.Hide -> onSourceHiddenChange(true)
                    NoteSourceVisibilityAction.Show -> onSourceHiddenChange(false)
                    NoteSourceVisibilityAction.Keep -> Unit
                }
            }
    }
}

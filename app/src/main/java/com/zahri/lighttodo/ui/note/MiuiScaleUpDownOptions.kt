package com.zahri.lighttodo.ui.note

import android.app.Activity
import android.app.ActivityOptions
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View

object MiuiScaleUpDownOptions {
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
        screenX: Int,
        screenY: Int,
        radiusPx: Int,
        targetColor: Int,
        sourceScale: Float,
        animationType: Int,
        onSourceHiddenChange: (Boolean) -> Unit
    ): Bundle? {
        if (snapshot == null) return null
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
            radiusPx = radiusPx,
            targetColor = targetColor,
            sourceScale = sourceScale,
            animationType = animationType,
            onLaunchStart = onSourceExitStart,
            onLaunchEnd = onSourceExitEnd,
            onReturnStart = onSourceReenterStart,
            onReturnEnd = onSourceReenterEnd
        ) ?: makeScaleUpAnimationFromRoundedView(
            anchor = anchor,
            snapshot = snapshot,
            screenX = screenX,
            screenY = screenY,
            radiusPx = radiusPx,
            targetColor = targetColor,
            sourceScale = sourceScale,
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

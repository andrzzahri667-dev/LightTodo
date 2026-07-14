package com.zahri.lighttodo.ui.motion.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.zahri.lighttodo.ui.motion.WheelPickerMotionPolicy

@Immutable
data class WheelPickerItemMotion(
    val alpha: Float,
    val scale: Float
)

fun wheelPickerItemMotion(proximity: Float): WheelPickerItemMotion =
    WheelPickerItemMotion(
        alpha = WheelPickerMotionPolicy.alphaForProximity(proximity),
        scale = WheelPickerMotionPolicy.scaleForProximity(proximity)
    )

fun Modifier.motionWheelPickerItemLayer(itemMotion: WheelPickerItemMotion): Modifier =
    graphicsLayer {
        alpha = itemMotion.alpha
        scaleX = itemMotion.scale
        scaleY = itemMotion.scale
    }

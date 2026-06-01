package com.zahri.lighttodo.ui.motion.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.zahri.lighttodo.ui.motion.AppMotion
import com.zahri.lighttodo.ui.motion.WheelPickerMotionPolicy

@Immutable
data class WheelPickerItemMotion(
    val alpha: Float,
    val scale: Float
)

@Composable
fun rememberWheelPickerItemMotion(proximity: Float): WheelPickerItemMotion {
    val alpha by animateFloatAsState(
        targetValue = WheelPickerMotionPolicy.alphaForProximity(proximity),
        animationSpec = tween(AppMotion.PickerItemAlphaMillis),
        label = "wheel-picker-item-alpha"
    )
    val scale by animateFloatAsState(
        targetValue = WheelPickerMotionPolicy.scaleForProximity(proximity),
        animationSpec = tween(AppMotion.PickerItemScaleMillis),
        label = "wheel-picker-item-scale"
    )
    return WheelPickerItemMotion(alpha = alpha, scale = scale)
}

fun Modifier.motionWheelPickerItemLayer(itemMotion: WheelPickerItemMotion): Modifier =
    graphicsLayer {
        alpha = itemMotion.alpha
        scaleX = itemMotion.scale
        scaleY = itemMotion.scale
    }

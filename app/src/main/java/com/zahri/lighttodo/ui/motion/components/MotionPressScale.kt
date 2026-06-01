package com.zahri.lighttodo.ui.motion.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.zahri.lighttodo.ui.motion.AppMotion

@Composable
fun rememberMotionPressScale(
    interactionSource: InteractionSource,
    label: String = "motion-press-scale"
): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) AppMotion.PressScale else 1f,
        animationSpec = AppMotion.pressSpring(),
        label = label
    )
}

fun Modifier.motionPressScaleLayer(scale: Float): Modifier =
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }

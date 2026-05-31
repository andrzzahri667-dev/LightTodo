package com.zahri.lighttodo.ui.motion.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zahri.lighttodo.ui.motion.AppMotion

@Composable
fun MotionTransientVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = AppMotion.transientSurfaceEnter(),
        exit = AppMotion.transientSurfaceExit(),
        modifier = modifier,
        content = { content() }
    )
}

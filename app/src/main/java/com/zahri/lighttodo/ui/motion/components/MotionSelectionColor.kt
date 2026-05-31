package com.zahri.lighttodo.ui.motion.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import com.zahri.lighttodo.ui.motion.AppMotion

@Composable
fun rememberMotionSelectionColor(
    selected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    durationMillis: Int = AppMotion.SelectionColorMillis,
    label: String = "motion-selection-color"
): State<Color> =
    animateColorAsState(
        targetValue = if (selected) selectedColor else unselectedColor,
        animationSpec = tween(durationMillis),
        label = label
    )

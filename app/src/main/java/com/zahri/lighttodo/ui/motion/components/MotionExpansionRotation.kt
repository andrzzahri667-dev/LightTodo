package com.zahri.lighttodo.ui.motion.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.zahri.lighttodo.ui.motion.AppMotion

@Composable
fun rememberMotionExpansionRotation(
    expanded: Boolean,
    label: String = "motion-expansion-rotation"
): State<Float> =
    animateFloatAsState(
        targetValue = if (expanded) AppMotion.SectionExpandedRotationDegrees else 0f,
        animationSpec = AppMotion.sectionArrowSpring(),
        label = label
    )

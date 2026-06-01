package com.zahri.lighttodo.ui.motion.components

import androidx.compose.animation.animateContentSize
import androidx.compose.ui.Modifier
import com.zahri.lighttodo.ui.motion.AppMotion

fun Modifier.motionNoteContentSize(): Modifier =
    animateContentSize(animationSpec = AppMotion.noteContentSizeSpring())

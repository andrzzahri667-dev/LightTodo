package com.zahri.lighttodo.ui.motion.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.zahri.lighttodo.ui.motion.AppMotion

object NoteSourceVisibilityMotion {
    const val ResetDelayMillis = AppMotion.NoteSourceAnimationResetDelayMillis
}

fun Modifier.motionNoteSourceVisibilityLayer(hidden: Boolean): Modifier =
    graphicsLayer { alpha = if (hidden) 0f else 1f }

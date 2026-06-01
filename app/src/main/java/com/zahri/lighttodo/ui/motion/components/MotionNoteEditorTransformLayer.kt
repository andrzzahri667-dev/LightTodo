package com.zahri.lighttodo.ui.motion.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.motionNoteEditorTransformLayer(
    alpha: Float,
    cornerRadius: Dp
): Modifier =
    graphicsLayer {
        transformOrigin = TransformOrigin(0f, 0f)
        this.alpha = alpha
        clip = cornerRadius > 0.dp
        shape = RoundedCornerShape(cornerRadius)
    }

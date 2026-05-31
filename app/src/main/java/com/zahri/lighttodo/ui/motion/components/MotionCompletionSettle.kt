package com.zahri.lighttodo.ui.motion.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Immutable
import com.zahri.lighttodo.ui.motion.AppMotion

@Immutable
data class MotionCompletionSettle(
    val alpha: Float,
    val translationX: Float
)

@Composable
fun rememberMotionCompletionSettle(active: Boolean): MotionCompletionSettle {
    val alpha by animateFloatAsState(
        targetValue = if (active) AppMotion.TodoCompletedContentAlpha else 1f,
        animationSpec = tween(AppMotion.TodoContentSettleMillis),
        label = "motion-completion-content-alpha"
    )
    val translationX by animateFloatAsState(
        targetValue = if (active) AppMotion.TodoCompletedContentTranslationX else 0f,
        animationSpec = tween(AppMotion.TodoContentSettleMillis),
        label = "motion-completion-content-offset"
    )
    return MotionCompletionSettle(alpha = alpha, translationX = translationX)
}

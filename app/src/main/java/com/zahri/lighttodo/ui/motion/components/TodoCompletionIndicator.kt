package com.zahri.lighttodo.ui.motion.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zahri.lighttodo.ui.motion.AppMotion
import com.zahri.lighttodo.ui.theme.AppColors

@Composable
fun TodoCompletionIndicator(
    displayDone: Boolean,
    contentDescription: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val checkAlpha by animateFloatAsState(
        targetValue = if (displayDone) 1f else 0f,
        animationSpec = tween(
            durationMillis = AppMotion.TodoCompletionCheckMillis,
            easing = AppMotion.EmphasizedEasing
        ),
        label = "todo-completion-check-alpha"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (displayDone) 1f else AppMotion.TodoCompletionCheckStartScale,
        animationSpec = tween(
            durationMillis = AppMotion.TodoCompletionCheckMillis,
            easing = AppMotion.EmphasizedEasing
        ),
        label = "todo-completion-check-scale"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription }
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (displayDone) AppColors.DoneGreen
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        alpha = checkAlpha
                        scaleX = checkScale
                        scaleY = checkScale
                    }
            )
        }
    }
}

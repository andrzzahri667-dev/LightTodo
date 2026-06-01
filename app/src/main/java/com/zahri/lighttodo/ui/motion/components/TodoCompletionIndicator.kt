package com.zahri.lighttodo.ui.motion.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.zahri.lighttodo.R
import com.zahri.lighttodo.ui.theme.AppColors

@Composable
fun TodoCompletionIndicator(
    displayDone: Boolean,
    animating: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (displayDone) AppColors.DoneGreen
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
            .semantics { this.contentDescription = contentDescription }
            .clickable(enabled = enabled) { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        if (displayDone) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.todo_check_success))
            val animatedProgress by animateLottieCompositionAsState(
                composition = composition,
                isPlaying = animating,
                restartOnPlay = true
            )
            val lottieProgress = if (animating) animatedProgress else 1f

            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { lottieProgress },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                )
            }
        }
    }
}

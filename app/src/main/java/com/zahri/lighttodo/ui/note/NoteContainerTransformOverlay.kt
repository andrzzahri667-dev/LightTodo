package com.zahri.lighttodo.ui.note

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.zahri.lighttodo.ui.motion.AppMotion
import kotlin.math.roundToInt

data class NoteContainerTransformRequest(
    val key: Long,
    val sourceBounds: Rect,
    val rootSize: IntSize,
    val sourceCornerRadius: Float,
    val direction: NoteContainerTransformDirection,
    val snapshot: Bitmap?,
    val onCovered: () -> Unit,
    val onFinished: () -> Unit
)

@Composable
fun NoteContainerTransformOverlay(request: NoteContainerTransformRequest?) {
    if (request == null) return

    val density = LocalDensity.current
    val progress = remember(request.key) {
        Animatable(
            if (request.direction == NoteContainerTransformDirection.Enter) 0f else 1f
        )
    }
    val revealAlpha = remember(request.key) { Animatable(1f) }
    val imageBitmap = remember(request.key, request.snapshot) { request.snapshot?.asImageBitmap() }

    LaunchedEffect(request.key) {
        when (request.direction) {
            NoteContainerTransformDirection.Enter -> {
                progress.animateTo(1f, AppMotion.noteEnterTween())
                request.onCovered()
                withFrameNanos { }
                revealAlpha.animateTo(
                    0f,
                    tween(AppMotion.NoteCoverRevealFadeMillis, easing = AppMotion.EmphasizedEasing)
                )
            }
            NoteContainerTransformDirection.Exit -> {
                progress.animateTo(0f, AppMotion.noteExitTween())
            }
        }
        request.onFinished()
    }

    val bounds = NoteContainerTransformPolicy.boundsAt(
        progress = progress.value,
        sourceBounds = request.sourceBounds,
        rootSize = request.rootSize
    )
    val containerAlpha =
        NoteContainerTransformPolicy.overlayAlphaAt(request.direction, progress.value) * revealAlpha.value
    val snapshotAlpha = when (request.direction) {
        NoteContainerTransformDirection.Enter ->
            NoteContainerTransformPolicy.sourceSnapshotAlphaAt(request.direction, progress.value)
        NoteContainerTransformDirection.Exit -> 1f
    }
    val cornerRadius = NoteContainerTransformPolicy.cornerRadiusAt(
        progress = progress.value,
        sourceRadius = request.sourceCornerRadius
    ).dp
    val background = if (isSystemInDarkTheme()) Color.Black else Color(0xFFFFFCF6)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(4f)
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = bounds.left.roundToInt(),
                        y = bounds.top.roundToInt()
                    )
                }
                .size(
                    width = with(density) { bounds.width.toDp() },
                    height = with(density) { bounds.height.toDp() }
                )
                .alpha(containerAlpha)
                .clip(RoundedCornerShape(cornerRadius))
                .background(background)
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(snapshotAlpha)
                )
            }
        }
    }
}

package com.zahri.lighttodo.ui.motion.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import com.zahri.lighttodo.ui.note.NoteEditorContainerTransformPolicy
import com.zahri.lighttodo.ui.note.NoteEditorTransitionBounds

@OptIn(ExperimentalMotionApi::class)
@Composable
fun NoteEditorMotionLayoutTransformHost(
    transitionBounds: NoteEditorTransitionBounds?,
    exitRequested: Boolean,
    onExitFinished: () -> Unit,
    sourcePreview: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    if (transitionBounds == null) {
        content()
        return
    }

    val view = LocalView.current
    val density = LocalDensity.current
    var entryPlayed by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (entryPlayed) 1f else 0f) }
    var rootSize by remember {
        mutableStateOf(
            if (view.width > 0 && view.height > 0) {
                IntSize(view.width, view.height)
            } else {
                IntSize.Zero
            }
        )
    }
    var rootScreenOffset by remember { mutableStateOf(IntOffset.Zero) }
    val canRender = NoteEditorContainerTransformPolicy.shouldRender(
        rootWidth = rootSize.width,
        rootHeight = rootSize.height
    )

    LaunchedEffect(canRender, exitRequested) {
        if (canRender && !entryPlayed && !exitRequested) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = NoteEditorContainerTransformPolicy.entryTween()
            )
            entryPlayed = true
        } else if (canRender && entryPlayed && !exitRequested && progress.value < 1f) {
            progress.snapTo(1f)
        }
    }

    LaunchedEffect(exitRequested, canRender) {
        if (exitRequested && canRender) {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = NoteEditorContainerTransformPolicy.exitTween()
            )
            onExitFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { rootSize = it }
            .onGloballyPositioned {
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                rootScreenOffset = IntOffset(location[0], location[1])
            }
    ) {
        if (canRender) {
            val sourceLeft = transitionBounds.screenLeft - rootScreenOffset.x
            val sourceTop = transitionBounds.screenTop - rootScreenOffset.y
            val frame = NoteEditorContainerTransformPolicy.frameFor(
                rootWidth = rootSize.width,
                rootHeight = rootSize.height,
                sourceLeft = sourceLeft,
                sourceTop = sourceTop,
                sourceWidth = transitionBounds.width,
                sourceHeight = transitionBounds.height,
                sourceCornerRadiusPx = transitionBounds.cornerRadiusPx,
                progress = progress.value
            )
            val cornerRadius = with(density) { frame.cornerRadiusPx.toDp() }
            val contentAlpha = NoteEditorContainerTransformPolicy.contentAlphaFor(progress.value)
            val geometryProgress = NoteEditorContainerTransformPolicy.geometryProgressFor(progress.value)
            val startSet = remember(rootSize, transitionBounds, sourceLeft, sourceTop, density) {
                noteEditorMotionConstraintSet(
                    rootSize = rootSize,
                    sourceLeft = sourceLeft,
                    sourceTop = sourceTop,
                    sourceWidth = transitionBounds.width,
                    sourceHeight = transitionBounds.height,
                    density = density.density,
                    atEnd = false
                )
            }
            val endSet = remember(rootSize, transitionBounds, density) {
                noteEditorMotionConstraintSet(
                    rootSize = rootSize,
                    sourceLeft = sourceLeft,
                    sourceTop = sourceTop,
                    sourceWidth = transitionBounds.width,
                    sourceHeight = transitionBounds.height,
                    density = density.density,
                    atEnd = true
                )
            }

            MotionLayout(
                start = startSet,
                end = endSet,
                progress = geometryProgress,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .layoutId(NoteEditorMotionIds.Editor)
                        .motionNoteEditorTransformLayer(
                            alpha = contentAlpha.editorAlpha,
                            cornerRadius = cornerRadius
                        )
                ) {
                    content()
                }

                val previewFrame = NoteEditorContainerTransformPolicy.sourcePreviewFrameFor(
                    containerFrame = frame,
                    rootWidth = rootSize.width,
                    rootHeight = rootSize.height,
                    sourceWidth = transitionBounds.width,
                    sourceHeight = transitionBounds.height
                )
                val previewCornerRadius = with(density) { previewFrame.cornerRadiusPx.toDp() }
                Box(
                    modifier = Modifier
                        .layoutId(NoteEditorMotionIds.SourcePreview)
                        .motionNoteEditorTransformLayer(
                            alpha = contentAlpha.sourcePreviewAlpha,
                            cornerRadius = previewCornerRadius
                        )
                ) {
                    sourcePreview()
                }
            }
        }
    }
}

private object NoteEditorMotionIds {
    const val Editor = "editor"
    const val SourcePreview = "sourcePreview"
}

private fun noteEditorMotionConstraintSet(
    rootSize: IntSize,
    sourceLeft: Int,
    sourceTop: Int,
    sourceWidth: Int,
    sourceHeight: Int,
    density: Float,
    atEnd: Boolean
): ConstraintSet {
    val safeDensity = density.takeIf { it > 0f } ?: 1f
    val rootWidth = rootSize.width.coerceAtLeast(1).toFloat()
    val rootHeight = rootSize.height.coerceAtLeast(1).toFloat()
    val sourceSafeWidth = sourceWidth.coerceAtLeast(1)
    val sourceSafeHeight = sourceHeight.coerceAtLeast(1)
    val editorScaleX = if (atEnd) 1f else sourceSafeWidth / rootWidth
    val editorScaleY = if (atEnd) 1f else sourceSafeHeight / rootHeight
    val previewScaleX = if (atEnd) rootWidth / sourceSafeWidth else 1f
    val previewScaleY = if (atEnd) rootHeight / sourceSafeHeight else 1f
    val editorTranslationX = if (atEnd) 0.dp else (sourceLeft / safeDensity).dp
    val editorTranslationY = if (atEnd) 0.dp else (sourceTop / safeDensity).dp
    val previewTranslationX = if (atEnd) 0.dp else (sourceLeft / safeDensity).dp
    val previewTranslationY = if (atEnd) 0.dp else (sourceTop / safeDensity).dp
    val sourceWidthDp = (sourceSafeWidth / safeDensity).dp
    val sourceHeightDp = (sourceSafeHeight / safeDensity).dp

    return ConstraintSet {
        val editor = createRefFor(NoteEditorMotionIds.Editor)
        val sourcePreview = createRefFor(NoteEditorMotionIds.SourcePreview)

        constrain(editor) {
            width = Dimension.fillToConstraints
            height = Dimension.fillToConstraints
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            scaleX = editorScaleX
            scaleY = editorScaleY
            translationX = editorTranslationX
            translationY = editorTranslationY
            pivotX = 0f
            pivotY = 0f
        }
        constrain(sourcePreview) {
            width = Dimension.value(sourceWidthDp)
            height = Dimension.value(sourceHeightDp)
            start.linkTo(parent.start)
            top.linkTo(parent.top)
            scaleX = previewScaleX
            scaleY = previewScaleY
            translationX = previewTranslationX
            translationY = previewTranslationY
            pivotX = 0f
            pivotY = 0f
        }
    }
}

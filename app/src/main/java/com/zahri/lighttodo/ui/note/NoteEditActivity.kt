package com.zahri.lighttodo.ui.note

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zahri.lighttodo.R
import com.zahri.lighttodo.ui.theme.AppColors
import com.zahri.lighttodo.ui.theme.AppType
import com.zahri.lighttodo.ui.theme.LightTodoTheme

class NoteEditActivity : ComponentActivity() {
    private val noteEditViewModel: NoteEditViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val launchMode = intent.noteLaunchAnimationModeExtra()
        if (NoteEditorWindowPolicy.shouldForceTransparentWindow(launchMode)) {
            setTheme(R.style.Theme_LightTodo_NoteTransform)
        } else {
            setTheme(R.style.Theme_LightTodo)
        }
        super.onCreate(savedInstanceState)
        if (NoteEditorWindowPolicy.shouldForceTransparentWindow(launchMode)) {
            forceTransparentWindow()
        }
        val editingId = intent.noteIdExtra()
        val launchSeed = intent.noteLaunchSeedExtra()
        noteEditViewModel.load(editingId, launchSeed)
        val transitionBounds = intent.noteTransitionBoundsExtra()

        setContent {
            var exitRequested by remember { mutableStateOf(false) }
            LightTodoTheme {
                NoteEditorContainerTransformHost(
                    transitionBounds = transitionBounds.takeIf {
                        launchMode == NoteEditorLaunchAnimationMode.CustomContainerTransform
                    },
                    exitRequested = exitRequested,
                    onExitFinished = { finishWithoutWindowAnimation() },
                    sourcePreview = {
                        NoteEditorSourcePreview(
                            editingId = editingId,
                            launchSeed = launchSeed
                        )
                    }
                ) {
                    Surface(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                        NoteEditScreen(
                            editingId = editingId,
                            onBack = {
                                when (launchMode) {
                                    NoteEditorLaunchAnimationMode.CustomContainerTransform -> {
                                        exitRequested = true
                                    }
                                    NoteEditorLaunchAnimationMode.MiuiSystemScaleUpDown -> {
                                        finish()
                                    }
                                    NoteEditorLaunchAnimationMode.Plain -> {
                                        finishWithoutWindowAnimation()
                                    }
                                }
                            },
                            vm = noteEditViewModel
                        )
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun finishWithoutWindowAnimation() {
        finish()
        overridePendingTransition(0, 0)
    }

    private fun forceTransparentWindow() {
        window.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
        window.decorView.setBackgroundColor(AndroidColor.TRANSPARENT)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
    }

    companion object {
        private const val ExtraNoteId = "com.zahri.lighttodo.extra.NOTE_ID"
        private const val ExtraLaunchAnimationMode = "com.zahri.lighttodo.extra.NOTE_LAUNCH_ANIMATION_MODE"
        private const val ExtraTransitionScreenLeft = "com.zahri.lighttodo.extra.NOTE_TRANSITION_SCREEN_LEFT"
        private const val ExtraTransitionScreenTop = "com.zahri.lighttodo.extra.NOTE_TRANSITION_SCREEN_TOP"
        private const val ExtraTransitionWidth = "com.zahri.lighttodo.extra.NOTE_TRANSITION_WIDTH"
        private const val ExtraTransitionHeight = "com.zahri.lighttodo.extra.NOTE_TRANSITION_HEIGHT"
        private const val ExtraTransitionCornerRadius =
            "com.zahri.lighttodo.extra.NOTE_TRANSITION_CORNER_RADIUS"
        private const val ExtraSeedId = "com.zahri.lighttodo.extra.NOTE_SEED_ID"
        private const val ExtraSeedTitle = "com.zahri.lighttodo.extra.NOTE_SEED_TITLE"
        private const val ExtraSeedContent = "com.zahri.lighttodo.extra.NOTE_SEED_CONTENT"
        private const val ExtraSeedCreatedAt = "com.zahri.lighttodo.extra.NOTE_SEED_CREATED_AT"
        private const val ExtraSeedUpdatedAt = "com.zahri.lighttodo.extra.NOTE_SEED_UPDATED_AT"

        fun intent(context: Context, noteId: Long?, launchSeed: NoteEditLaunchSeed? = null): Intent =
            Intent(context, NoteEditActivity::class.java).apply {
                if (noteId != null) putExtra(ExtraNoteId, noteId)
                if (launchSeed != null) {
                    putExtra(ExtraSeedId, launchSeed.id)
                    putExtra(ExtraSeedTitle, launchSeed.title)
                    putExtra(ExtraSeedContent, launchSeed.content)
                    putExtra(ExtraSeedCreatedAt, launchSeed.createdAtMillis)
                    putExtra(ExtraSeedUpdatedAt, launchSeed.updatedAtMillis)
                }
            }

        fun setLaunchAnimationMode(intent: Intent, mode: NoteEditorLaunchAnimationMode) {
            intent.putExtra(ExtraLaunchAnimationMode, mode.name)
        }

        fun setTransitionBounds(intent: Intent, bounds: NoteEditorTransitionBounds) {
            intent.putExtra(ExtraTransitionScreenLeft, bounds.screenLeft)
            intent.putExtra(ExtraTransitionScreenTop, bounds.screenTop)
            intent.putExtra(ExtraTransitionWidth, bounds.width)
            intent.putExtra(ExtraTransitionHeight, bounds.height)
            intent.putExtra(ExtraTransitionCornerRadius, bounds.cornerRadiusPx)
        }

        private fun Intent.noteIdExtra(): Long? =
            if (hasExtra(ExtraNoteId)) getLongExtra(ExtraNoteId, 0L) else null

        private fun Intent.noteLaunchAnimationModeExtra(): NoteEditorLaunchAnimationMode =
            getStringExtra(ExtraLaunchAnimationMode)
                ?.let { runCatching { NoteEditorLaunchAnimationMode.valueOf(it) }.getOrNull() }
                ?: NoteEditorLaunchAnimationMode.Plain

        private fun Intent.noteTransitionBoundsExtra(): NoteEditorTransitionBounds? =
            if (!hasExtra(ExtraTransitionScreenLeft)) {
                null
            } else {
                NoteEditorTransitionBounds(
                    screenLeft = getIntExtra(ExtraTransitionScreenLeft, 0),
                    screenTop = getIntExtra(ExtraTransitionScreenTop, 0),
                    width = getIntExtra(ExtraTransitionWidth, 1),
                    height = getIntExtra(ExtraTransitionHeight, 1),
                    cornerRadiusPx = getIntExtra(ExtraTransitionCornerRadius, 0)
                )
            }

        private fun Intent.noteLaunchSeedExtra(): NoteEditLaunchSeed? =
            if (!hasExtra(ExtraSeedId)) {
                null
            } else {
                NoteEditLaunchSeed(
                    id = getLongExtra(ExtraSeedId, 0L),
                    title = getStringExtra(ExtraSeedTitle),
                    content = getStringExtra(ExtraSeedContent).orEmpty(),
                    createdAtMillis = getLongExtra(ExtraSeedCreatedAt, 0L),
                    updatedAtMillis = getLongExtra(ExtraSeedUpdatedAt, 0L)
                )
            }
    }
}

@Composable
private fun NoteEditorContainerTransformHost(
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

    val progress = remember { Animatable(0f) }
    val view = LocalView.current
    val density = LocalDensity.current
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
    var entryPlayed by remember { mutableStateOf(false) }
    val canRender = NoteEditorContainerTransformPolicy.shouldRender(
        rootWidth = rootSize.width,
        rootHeight = rootSize.height
    )

    LaunchedEffect(canRender, exitRequested) {
        if (canRender && !entryPlayed && !exitRequested) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = NoteEditorContainerTransformPolicy.EntryDurationMillis,
                    easing = NoteEditorContainerTransformPolicy.EntryEasing.toComposeEasing()
                )
            )
            entryPlayed = true
        }
    }

    LaunchedEffect(exitRequested, canRender) {
        if (exitRequested && canRender) {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = NoteEditorContainerTransformPolicy.ExitDurationMillis,
                    easing = NoteEditorContainerTransformPolicy.ExitEasing.toComposeEasing()
                )
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
            val frame = NoteEditorContainerTransformPolicy.frameFor(
                rootWidth = rootSize.width,
                rootHeight = rootSize.height,
                sourceLeft = transitionBounds.screenLeft - rootScreenOffset.x,
                sourceTop = transitionBounds.screenTop - rootScreenOffset.y,
                sourceWidth = transitionBounds.width,
                sourceHeight = transitionBounds.height,
                sourceCornerRadiusPx = transitionBounds.cornerRadiusPx,
                progress = progress.value
            )
            val cornerRadius = with(density) { frame.cornerRadiusPx.toDp() }
            val contentPhase = NoteEditorContainerTransformPolicy.contentPhaseFor(progress.value)
            val editorAlpha = if (contentPhase == NoteEditorTransformContentPhase.Editor) 1f else 0f
            val sourcePreviewAlpha =
                if (contentPhase == NoteEditorTransformContentPhase.SourcePreview) 1f else 0f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        translationX = frame.translationX
                        translationY = frame.translationY
                        scaleX = frame.scaleX
                        scaleY = frame.scaleY
                        alpha = editorAlpha
                        clip = cornerRadius > 0.dp
                        shape = RoundedCornerShape(cornerRadius)
                    }
            ) {
                content()
            }

            val previewFrame = NoteEditorContainerTransformPolicy.sourcePreviewFrameFor(
                rootWidth = rootSize.width,
                rootHeight = rootSize.height,
                sourceLeft = transitionBounds.screenLeft - rootScreenOffset.x,
                sourceTop = transitionBounds.screenTop - rootScreenOffset.y,
                sourceWidth = transitionBounds.width,
                sourceHeight = transitionBounds.height,
                sourceCornerRadiusPx = transitionBounds.cornerRadiusPx,
                progress = progress.value
            )
            val previewCornerRadius = with(density) { previewFrame.cornerRadiusPx.toDp() }
            Box(
                modifier = Modifier
                    .width(with(density) { transitionBounds.width.toDp() })
                    .height(with(density) { transitionBounds.height.toDp() })
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        translationX = previewFrame.translationX
                        translationY = previewFrame.translationY
                        scaleX = previewFrame.scaleX
                        scaleY = previewFrame.scaleY
                        alpha = sourcePreviewAlpha
                        clip = previewCornerRadius > 0.dp
                        shape = RoundedCornerShape(previewCornerRadius)
                    }
            ) {
                sourcePreview()
            }
        }
    }
}

@Composable
private fun NoteEditorSourcePreview(
    editingId: Long?,
    launchSeed: NoteEditLaunchSeed?
) {
    if (editingId == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Brand, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
        return
    }

    val title = launchSeed?.title?.takeIf { it.isNotBlank() }
    val preview = launchSeed?.content
        ?.takeIf { it.isNotBlank() }
        ?.let { MarkdownSpanApplier.stripMarkdown(it) }
        ?.takeIf { it.isNotBlank() }
    val noteBackground = MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(noteBackground)
            .padding(12.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = AppType.headline,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
        }
        if (preview != null) {
            Text(
                text = preview,
                style = AppType.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (title != null) 5 else 7,
                overflow = TextOverflow.Ellipsis
            )
        } else if (title == null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.note_no_title),
                style = AppType.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

private fun NoteEditorContainerTransformEasing.toComposeEasing(): Easing =
    CubicBezierEasing(x1, y1, x2, y2)

package com.zahri.lighttodo.ui.note

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.core.view.WindowCompat
import com.zahri.lighttodo.R
import com.zahri.lighttodo.ui.motion.components.motionNoteEditorTransformLayer
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
        if (NoteEditorWindowPolicy.shouldUseEdgeToEdgeWindow(launchMode)) {
            configureEdgeToEdgeWindow()
        }
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
                NoteEditorMotionLayoutTransformHost(
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

    private fun configureEdgeToEdgeWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        val isDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
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

@OptIn(ExperimentalMotionApi::class)
@Composable
private fun NoteEditorMotionLayoutTransformHost(
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
                modifier = Modifier
                    .fillMaxSize()
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
    val noteBackground = NoteEditorColors.transformPreviewBackground(isSystemInDarkTheme())

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
            NoteEmptyPlaceholder()
        }
    }
}

package com.zahri.lighttodo.feature.noteeditor

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.zahri.lighttodo.R
import com.zahri.lighttodo.ui.motion.NoteEditorTransitionBounds
import com.zahri.lighttodo.ui.motion.components.NoteEditorMotionLayoutTransformHost
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

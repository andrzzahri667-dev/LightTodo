package com.zahri.lighttodo.ui.note

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zahri.lighttodo.R
import com.zahri.lighttodo.ui.motion.AppMotion
import com.zahri.lighttodo.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun NoteEditScreen(
    editingId: Long?,
    onBack: () -> Unit,
    vm: NoteEditViewModel = viewModel()
) {
    val title by vm.title.collectAsStateWithLifecycle()
    val content by vm.content.collectAsStateWithLifecycle()
    val createdAt by vm.createdAt.collectAsStateWithLifecycle()
    val updatedAt by vm.updatedAt.collectAsStateWithLifecycle()
    val mediaState by vm.mediaState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val contentHint = stringResource(R.string.note_content_hint)
    val noteBackground = NoteEditorColors.editorBackground(isDark)
    val contentBlocks = remember(content) { NoteContentBlocks.parse(content) }
    val keyboardVisible = WindowInsets.ime.getBottom(density) > 0

    var editorFocused by remember { mutableStateOf(false) }
    val toolbarVisible = keyboardVisible || editorFocused
    var focusedTextIndex by remember { mutableStateOf(0) }
    var focusedCursor by remember { mutableStateOf(0) }
    var focusedEditor by remember { mutableStateOf<MarkdownEditText?>(null) }
    var pendingFocusTextIndex by remember { mutableStateOf<Int?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var previewImageRef by remember { mutableStateOf<String?>(null) }
    var pendingDeleteAttachment by remember { mutableStateOf<NoteAttachmentMarkdown.Attachment?>(null) }
    var pendingKeyboardMediaDelete by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val formatMode = remember { mutableStateOf(false) }
    var styleState by remember { mutableStateOf(MarkdownStyleState()) }
    val formattingController = remember { MarkdownFormattingController() }

    fun updateBlocks(blocks: List<NoteContentBlock>) {
        vm.updateContent(NoteContentBlocks.serialize(blocks))
    }

    fun updateTextBlock(index: Int, text: String) {
        pendingKeyboardMediaDelete = null
        val blocks = NoteContentBlocks.parse(content).toMutableList()
        if (index !in blocks.indices || blocks[index] !is NoteContentBlock.Text) return
        blocks[index] = NoteContentBlock.Text(text)
        updateBlocks(blocks)
    }

    fun insertBlock(block: NoteContentBlock) {
        pendingKeyboardMediaDelete = null
        val result = NoteContentBlocks.insertAfterTextCursor(
            blocks = contentBlocks,
            textBlockIndex = focusedTextIndex,
            cursor = focusedEditor?.selectionStart ?: focusedCursor,
            insertedBlock = block
        )
        pendingFocusTextIndex = result.focusTextIndex
        updateBlocks(result.blocks)
    }

    fun deleteMediaBeforeTextBlock(index: Int): Boolean {
        val blocks = NoteContentBlocks.parse(content)
        val ref = NoteContentBlocks.mediaRefBeforeTextCursor(
            blocks = blocks,
            textBlockIndex = index,
            cursor = 0
        ) ?: run {
            pendingKeyboardMediaDelete = null
            return false
        }
        val marker = index to ref
        if (pendingKeyboardMediaDelete != marker) {
            pendingKeyboardMediaDelete = marker
            return true
        }
        val result = NoteContentBlocks.removeMediaBeforeTextCursor(
            blocks = blocks,
            textBlockIndex = index,
            cursor = 0
        ) ?: return false
        pendingKeyboardMediaDelete = null
        pendingFocusTextIndex = result.focusTextIndex
        updateBlocks(result.blocks)
        vm.stopAudioPlayback(result.removedRef)
        return true
    }

    fun focusAfterContent() {
        pendingKeyboardMediaDelete = null
        when (contentBlocks.lastOrNull()) {
            is NoteContentBlock.Text -> {
                pendingFocusTextIndex = contentBlocks.lastIndex
            }
            null -> {
                pendingFocusTextIndex = 0
                updateBlocks(listOf(NoteContentBlock.Text("")))
            }
            else -> {
                val nextBlocks = contentBlocks + NoteContentBlock.Text("\n")
                pendingFocusTextIndex = nextBlocks.lastIndex
                updateBlocks(nextBlocks)
            }
        }
    }

    fun playAudio(ref: String) {
        if (!vm.toggleAudioPlayback(context, ref)) {
            Toast.makeText(context, R.string.note_audio_play_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun startRecording() {
        if (!vm.startRecording(context)) {
            Toast.makeText(context, R.string.note_record_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        val recorded = vm.stopRecording()
        if (recorded != null) {
            insertBlock(
                NoteContentBlock.Audio(
                    ref = NoteAttachmentStore.audioRef(recorded.file),
                    durationLabel = NoteAttachmentMarkdown.formatDuration(recorded.durationMillis)
                )
            )
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording()
        else Toast.makeText(context, R.string.note_audio_permission_denied, Toast.LENGTH_SHORT).show()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    NoteAttachmentStore.copyImageFromUri(context, uri)
                }
            }.onSuccess { file ->
                insertBlock(NoteContentBlock.Image(NoteAttachmentStore.imageRef(file)))
            }.onFailure {
                Toast.makeText(context, R.string.note_image_insert_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (saved && file != null && file.exists() && file.length() > 0L) {
            insertBlock(NoteContentBlock.Image(NoteAttachmentStore.imageRef(file)))
        } else {
            file?.delete()
        }
    }

    fun toggleRecording() {
        if (mediaState.recording) {
            stopRecording()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) startRecording()
        else recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun leaveNote() {
        hideNoteKeyboard(context, view, focusedEditor)
        vm.save()
        onBack()
    }

    LaunchedEffect(Unit) { vm.load(editingId) }

    LaunchedEffect(toolbarVisible) {
        if (!toolbarVisible) formatMode.value = false
    }

    DisposableEffect(Unit) {
        onDispose {
            vm.save()
        }
    }

    BackHandler { leaveNote() }

    Scaffold(containerColor = noteBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { leaveNote() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.note_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.weight(1f))
                if (editingId != null) {
                    IconButton(onClick = {
                        hideNoteKeyboard(context, view, focusedEditor)
                        vm.delete { onBack() }
                    }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.note_delete),
                            tint = AppColors.Overdue
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { vm.updateTitle(it) },
                    textStyle = TextStyle(
                        fontSize = 28.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(AppColors.Brand),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (title.isEmpty()) {
                            Text(
                                stringResource(R.string.note_title_hint),
                                style = TextStyle(
                                    fontSize = 28.sp,
                                    lineHeight = 36.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        inner()
                    }
                )

                Spacer(Modifier.height(14.dp))

                if (createdAt > 0) {
                    val fmt = remember { SimpleDateFormat("MMM d h:mm a", Locale.getDefault()) }
                    val metaTime = updatedAt.takeIf { it > 0 } ?: createdAt
                    Text(
                        text = stringResource(
                            R.string.note_meta,
                            fmt.format(Date(metaTime)),
                            (title.length + content.length)
                        ),
                        style = TextStyle(fontSize = 12.sp, lineHeight = 24.sp),
                        color = Color(0xFF9A9A9A)
                    )
                    Spacer(Modifier.height(22.dp))
                }

                contentBlocks.forEachIndexed { index, block ->
                    key(NoteContentBlockUiKeys.keyFor(index, block)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(animationSpec = AppMotion.noteContentSizeSpring())
                        ) {
                            when (block) {
                                is NoteContentBlock.Text -> NoteTextBlockEditor(
                                    index = index,
                                    text = block.text,
                                    hint = if (contentBlocks.size == 1) contentHint else "",
                                    minHeight = if (contentBlocks.size == 1) 360.dp else 56.dp,
                                    requestFocus = pendingFocusTextIndex == index,
                                    cursorVisible = pendingKeyboardMediaDelete?.first != index,
                                    onFocusApplied = { pendingFocusTextIndex = null },
                                    onTextChanged = ::updateTextBlock,
                                    onFocused = { editText ->
                                        if (focusedTextIndex != index) {
                                            pendingKeyboardMediaDelete = null
                                        }
                                        focusedTextIndex = index
                                        focusedCursor = editText.selectionStart.coerceAtLeast(0)
                                        focusedEditor = editText
                                        editorFocused = true
                                        styleState = formattingController.bind(editText)
                                    },
                                    onBlurred = {
                                        editorFocused = false
                                    },
                                    onSelectionChanged = { start, end, editText ->
                                        if (focusedTextIndex == index) {
                                            if (start != 0 || end != 0) {
                                                pendingKeyboardMediaDelete = null
                                            }
                                            focusedCursor = start
                                            styleState = formattingController.onSelectionChanged(editText, start, end)
                                        }
                                    },
                                    onDeletePreviousMedia = {
                                        deleteMediaBeforeTextBlock(index)
                                    },
                                    onLinkClick = { url ->
                                        val intent = Intent(Intent.ACTION_VIEW, browsableUri(url))
                                        context.startActivity(intent)
                                    }
                                )

                                is NoteContentBlock.Image -> NoteImageBlock(
                                    ref = block.ref,
                                    selected = pendingKeyboardMediaDelete?.second == block.ref,
                                    onOpen = { previewImageRef = block.ref },
                                    onDelete = {
                                        pendingDeleteAttachment = NoteAttachmentMarkdown.Attachment(
                                            kind = NoteAttachmentMarkdown.Kind.Image,
                                            ref = block.ref,
                                            label = "Image"
                                        )
                                    }
                                )

                                is NoteContentBlock.Audio -> NoteAudioBlock(
                                    durationLabel = block.durationLabel,
                                    playing = mediaState.playingAudioRef == block.ref,
                                    selected = pendingKeyboardMediaDelete?.second == block.ref,
                                    onPlayPause = { playAudio(block.ref) },
                                    onDelete = {
                                        pendingDeleteAttachment = NoteAttachmentMarkdown.Attachment(
                                            kind = NoteAttachmentMarkdown.Kind.Audio,
                                            ref = block.ref,
                                            label = block.durationLabel
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .pointerInput(contentBlocks) {
                            detectTapGestures(onTap = { focusAfterContent() })
                        }
                )
            }

            AnimatedVisibility(
                visible = toolbarVisible,
                enter = AppMotion.transientSurfaceEnter(),
                exit = AppMotion.transientSurfaceExit()
            ) {
                NoteToolbar(
                    formatMode = formatMode,
                    styleState = styleState,
                    recording = mediaState.recording,
                    onPickImage = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onTakePhoto = {
                        val file = NoteAttachmentStore.createImageFile(context)
                        pendingCameraFile = file
                        cameraLauncher.launch(NoteAttachmentStore.fileProviderUri(context, file))
                    },
                    onToggleRecording = { toggleRecording() },
                    onFormatAction = { action ->
                        styleState = formattingController.apply(action)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                )
            }
        }
    }

    previewImageRef?.let { ref ->
        ImagePreviewDialog(ref = ref, onDismiss = { previewImageRef = null })
    }

    pendingDeleteAttachment?.let { attachment ->
        AlertDialog(
            onDismissRequest = { pendingDeleteAttachment = null },
            title = { Text(stringResource(R.string.note_delete_attachment)) },
            text = { Text(stringResource(R.string.note_delete_attachment_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val next = NoteContentBlocks.removeAttachment(content, attachment.ref)
                        vm.updateContent(next)
                        vm.stopAudioPlayback(attachment.ref)
                        pendingDeleteAttachment = null
                    }
                ) {
                    Text(stringResource(R.string.note_delete), color = AppColors.Overdue)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAttachment = null }) {
                    Text(stringResource(R.string.edit_picker_cancel))
                }
            }
        )
    }
}

@Composable
private fun NoteTextBlockEditor(
    index: Int,
    text: String,
    hint: String,
    minHeight: Dp,
    requestFocus: Boolean,
    cursorVisible: Boolean,
    onFocusApplied: () -> Unit,
    onTextChanged: (Int, String) -> Unit,
    onFocused: (MarkdownEditText) -> Unit,
    onBlurred: () -> Unit,
    onSelectionChanged: (Int, Int, MarkdownEditText) -> Unit,
    onDeletePreviousMedia: () -> Boolean,
    onLinkClick: (String) -> Unit
) {
    val context = LocalContext.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val editText = remember { MarkdownEditText(context) }

    AndroidView(
        factory = { editText },
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight),
        update = { view ->
            val textColor = if (isDark) 0xFFFFFFFF.toInt() else 0xFF202124.toInt()
            view.setTextColor(textColor)
            view.setHintTextColor(0xFF8E8E93.toInt())
            view.hint = hint
            view.isCursorVisible = cursorVisible
            view.contentUpdateCallback = { onTextChanged(index, it) }
            view.selectionChangedCallback = { start, end -> onSelectionChanged(start, end, view) }
            view.deletePreviousMediaCallback = onDeletePreviousMedia
            view.linkClickCallback = onLinkClick
            view.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) onFocused(view) else onBlurred()
            }
            if (view.editableText.toString() != text) {
                view.setContentWithoutTrigger(text)
            }
        }
    )

    LaunchedEffect(requestFocus, text) {
        if (requestFocus) {
            editText.post {
                editText.requestFocus()
                editText.setSelection(editText.editableText.length)
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
                onFocusApplied()
            }
        }
    }

    DisposableEffect(editText) {
        onDispose {
            editText.contentUpdateCallback = null
            editText.selectionChangedCallback = null
            editText.deletePreviousMediaCallback = null
            editText.linkClickCallback = null
            editText.onFocusChangeListener = null
        }
    }
}

@Composable
private fun NoteImageBlock(
    ref: String,
    selected: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val shape = RoundedCornerShape(12.dp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        val targetWidthPx = with(density) { maxWidth.toPx() }.roundToInt().coerceAtLeast(1)
        val image by produceState<LoadedNoteImage?>(
            initialValue = null,
            context,
            ref,
            targetWidthPx
        ) {
            value = withContext(Dispatchers.IO) {
                loadNoteImage(context, ref, targetWidthPx)
            }
        }

        val loadedImage = image
        if (loadedImage != null) {
            Image(
                bitmap = loadedImage.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(loadedImage.aspectRatio)
                    .clip(shape)
                    .then(mediaSelectionModifier(selected, shape))
                    .pointerInput(ref) {
                        detectTapGestures(
                            onTap = { onOpen() },
                            onLongPress = { onDelete() }
                        )
                    },
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(mediaSelectionModifier(selected, shape))
                    .pointerInput(ref) {
                        detectTapGestures(onLongPress = { onDelete() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.note_image_missing),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = TextStyle(fontSize = 14.sp)
                )
            }
        }
    }
}

@Composable
private fun NoteAudioBlock(
    durationLabel: String,
    playing: Boolean,
    selected: Boolean,
    onPlayPause: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .padding(vertical = 5.dp)
            .clip(shape)
            .background(if (playing) Color(0xFFFFE4B8) else Color(0xFFFFF1DA))
            .then(mediaSelectionModifier(selected, shape))
            .height(44.dp)
            .pointerInput(durationLabel, playing) {
                detectTapGestures(
                    onTap = { onPlayPause() },
                    onLongPress = { onDelete() }
                )
            }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = stringResource(
                if (playing) R.string.note_audio_pause else R.string.note_audio_play
            ),
            tint = Color(0xFFFF9F0A),
            modifier = Modifier.size(24.dp)
        )
        AudioWaveBars()
        Text(
            text = durationLabel,
            color = Color(0xFF5C4A26),
            style = TextStyle(fontSize = 15.sp, lineHeight = 20.sp)
        )
    }
}

@Composable
private fun mediaSelectionModifier(
    selected: Boolean,
    shape: RoundedCornerShape
): Modifier {
    return if (selected) {
        Modifier.border(width = 2.dp, color = Color(0xFFFFC400), shape = shape)
    } else {
        Modifier
    }
}

@Composable
private fun AudioWaveBars() {
    val heights = listOf(12.dp, 20.dp, 16.dp, 26.dp, 14.dp, 22.dp, 12.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFFFB340))
            )
        }
    }
}

@Composable
private fun ImagePreviewDialog(ref: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val targetWidthPx = with(density) { maxWidth.toPx() }.roundToInt().coerceAtLeast(1)
            val targetHeightPx = with(density) { maxHeight.toPx() }.roundToInt().coerceAtLeast(1)
            val image by produceState<LoadedNoteImage?>(
                initialValue = null,
                context,
                ref,
                targetWidthPx,
                targetHeightPx
            ) {
                value = withContext(Dispatchers.IO) {
                    loadNoteImage(
                        context = context,
                        ref = ref,
                        targetWidthPx = targetWidthPx,
                        targetHeightPx = targetHeightPx
                    )
                }
            }

            val loadedImage = image
            if (loadedImage != null) {
                Image(
                    bitmap = loadedImage.bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

private fun browsableUri(url: String): Uri {
    val trimmed = url.trim()
    val hasScheme = trimmed.contains("://")
    return Uri.parse(if (hasScheme) trimmed else "https://$trimmed")
}

private fun hideNoteKeyboard(
    context: Context,
    fallbackView: View,
    editText: MarkdownEditText?
) {
    editText?.clearFocus()
    val token = editText?.windowToken ?: fallbackView.windowToken
    context.getSystemService(InputMethodManager::class.java)
        ?.hideSoftInputFromWindow(token, 0)
}

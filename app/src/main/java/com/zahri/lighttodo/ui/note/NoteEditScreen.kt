package com.zahri.lighttodo.ui.note

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
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

    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val contentHint = stringResource(R.string.note_content_hint)
    val noteBackground = if (isDark) Color.Black else Color(0xFFFFFCF6)
    val contentBlocks = remember(content) { NoteContentBlocks.parse(content) }
    val keyboardVisible = WindowInsets.ime.getBottom(density) > 0

    var editorFocused by remember { mutableStateOf(false) }
    var focusedTextIndex by remember { mutableStateOf(0) }
    var focusedCursor by remember { mutableStateOf(0) }
    var focusedEditor by remember { mutableStateOf<MarkdownEditText?>(null) }
    var pendingFocusTextIndex by remember { mutableStateOf<Int?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordingStartedAt by remember { mutableStateOf(0L) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingAudioRef by remember { mutableStateOf<String?>(null) }
    var previewImageRef by remember { mutableStateOf<String?>(null) }
    var pendingDeleteAttachment by remember { mutableStateOf<NoteAttachmentMarkdown.Attachment?>(null) }

    val latestRecorder by rememberUpdatedState(recorder)
    val latestPlayer by rememberUpdatedState(player)

    fun updateBlocks(blocks: List<NoteContentBlock>) {
        vm.updateContent(NoteContentBlocks.serialize(blocks))
    }

    fun updateTextBlock(index: Int, text: String) {
        val blocks = NoteContentBlocks.parse(content).toMutableList()
        if (index !in blocks.indices || blocks[index] !is NoteContentBlock.Text) return
        blocks[index] = NoteContentBlock.Text(text)
        updateBlocks(blocks)
    }

    fun insertBlock(block: NoteContentBlock) {
        val blocks = contentBlocks.toMutableList()
        val index = focusedTextIndex.coerceIn(0, blocks.lastIndex.coerceAtLeast(0))
        val current = blocks.getOrNull(index)
        val nextBlocks = mutableListOf<NoteContentBlock>()
        var focusAfterInsert = 0

        if (current is NoteContentBlock.Text) {
            val cursor = (focusedEditor?.selectionStart ?: focusedCursor)
                .coerceIn(0, current.text.length)
            val before = current.text.substring(0, cursor)
            val after = current.text.substring(cursor)
            blocks.forEachIndexed { blockIndex, existing ->
                if (blockIndex != index) {
                    nextBlocks += existing
                } else {
                    if (before.isNotEmpty()) nextBlocks += NoteContentBlock.Text(before)
                    nextBlocks += block
                    focusAfterInsert = nextBlocks.size
                    nextBlocks += NoteContentBlock.Text(after)
                }
            }
        } else {
            nextBlocks += blocks
            nextBlocks += block
            focusAfterInsert = nextBlocks.size
            nextBlocks += NoteContentBlock.Text("")
        }

        pendingFocusTextIndex = focusAfterInsert
        updateBlocks(nextBlocks)
    }

    fun playAudio(ref: String) {
        if (playingAudioRef == ref) {
            player?.release()
            player = null
            playingAudioRef = null
            return
        }

        val file = NoteAttachmentStore.resolve(context, ref)
        if (file == null) {
            Toast.makeText(context, R.string.note_audio_play_failed, Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            player?.release()
            val nextPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { completed ->
                    completed.release()
                    if (player === completed) {
                        player = null
                        playingAudioRef = null
                    }
                }
                prepare()
                start()
            }
            player = nextPlayer
            playingAudioRef = ref
        }.onFailure {
            player?.release()
            player = null
            playingAudioRef = null
            Toast.makeText(context, R.string.note_audio_play_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun startRecording() {
        player?.release()
        player = null
        playingAudioRef = null

        val file = NoteAttachmentStore.createAudioFile(context)
        val nextRecorder = createNoteMediaRecorder(context, file)
        runCatching {
            nextRecorder.prepare()
            nextRecorder.start()
        }.onSuccess {
            recordingFile = file
            recordingStartedAt = System.currentTimeMillis()
            recorder = nextRecorder
        }.onFailure {
            nextRecorder.release()
            file.delete()
            Toast.makeText(context, R.string.note_record_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        val activeRecorder = recorder ?: return
        val file = recordingFile
        val startedAt = recordingStartedAt
        recorder = null
        recordingFile = null
        recordingStartedAt = 0L

        val stopped = runCatching { activeRecorder.stop() }.isSuccess
        activeRecorder.release()
        val duration = System.currentTimeMillis() - startedAt
        if (stopped && file != null && file.exists() && file.length() > 0L) {
            insertBlock(
                NoteContentBlock.Audio(
                    ref = NoteAttachmentStore.audioRef(file),
                    durationLabel = NoteAttachmentMarkdown.formatDuration(duration)
                )
            )
        } else {
            file?.delete()
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording()
        else Toast.makeText(context, R.string.note_audio_permission_denied, Toast.LENGTH_SHORT).show()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                NoteAttachmentStore.copyImageFromUri(context, uri)
            }
            insertBlock(NoteContentBlock.Image(NoteAttachmentStore.imageRef(file)))
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
        if (recorder != null) {
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

    DisposableEffect(Unit) {
        onDispose {
            vm.save()
            latestRecorder?.runCatching { stop() }
            latestRecorder?.release()
            latestPlayer?.release()
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
                    when (block) {
                        is NoteContentBlock.Text -> NoteTextBlockEditor(
                            index = index,
                            text = block.text,
                            hint = if (contentBlocks.size == 1) contentHint else "",
                            minHeight = if (contentBlocks.size == 1) 360.dp else 56.dp,
                            requestFocus = pendingFocusTextIndex == index,
                            onFocusApplied = { pendingFocusTextIndex = null },
                            onTextChanged = ::updateTextBlock,
                            onFocused = { editText ->
                                focusedTextIndex = index
                                focusedCursor = editText.selectionStart.coerceAtLeast(0)
                                focusedEditor = editText
                                editorFocused = true
                            },
                            onBlurred = {
                                editorFocused = false
                            },
                            onSelectionChanged = { cursor ->
                                if (focusedTextIndex == index) focusedCursor = cursor
                            },
                            onLinkClick = { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        )

                        is NoteContentBlock.Image -> NoteImageBlock(
                            ref = block.ref,
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
                            playing = playingAudioRef == block.ref,
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

                Spacer(Modifier.height(32.dp))
            }

            AnimatedVisibility(visible = keyboardVisible || editorFocused || recorder != null) {
                NoteAttachmentToolbar(
                    recording = recorder != null,
                    onPickImage = { galleryLauncher.launch("image/*") },
                    onTakePhoto = {
                        val file = NoteAttachmentStore.createImageFile(context)
                        pendingCameraFile = file
                        cameraLauncher.launch(NoteAttachmentStore.fileProviderUri(context, file))
                    },
                    onToggleRecording = { toggleRecording() },
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
                        NoteAttachmentStore.delete(context, attachment.ref)
                        if (playingAudioRef == attachment.ref) {
                            player?.release()
                            player = null
                            playingAudioRef = null
                        }
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
    onFocusApplied: () -> Unit,
    onTextChanged: (Int, String) -> Unit,
    onFocused: (MarkdownEditText) -> Unit,
    onBlurred: () -> Unit,
    onSelectionChanged: (Int) -> Unit,
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
            view.contentUpdateCallback = { onTextChanged(index, it) }
            view.selectionChangedCallback = { onSelectionChanged(it) }
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
            editText.linkClickCallback = null
            editText.onFocusChangeListener = null
        }
    }
}

@Composable
private fun NoteImageBlock(
    ref: String,
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
        val image = remember(ref, targetWidthPx) {
            loadNoteImage(context, ref, targetWidthPx)
        }

        if (image != null) {
            Image(
                bitmap = image.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(image.aspectRatio)
                    .clip(shape)
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
    onPlayPause: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (playing) Color(0xFFFFE4B8) else Color(0xFFFFF1DA))
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
            contentDescription = null,
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
    val bitmap = remember(ref) {
        NoteAttachmentStore.resolve(context, ref)?.absolutePath?.let(BitmapFactory::decodeFile)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun NoteAttachmentToolbar(
    recording: Boolean,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AttachmentIconButton(
            onClick = onPickImage,
            contentDescription = stringResource(R.string.note_insert_image),
            imageVector = Icons.Default.Image
        )
        AttachmentIconButton(
            onClick = onTakePhoto,
            contentDescription = stringResource(R.string.note_take_photo),
            imageVector = Icons.Default.PhotoCamera
        )
        AttachmentIconButton(
            onClick = onToggleRecording,
            selected = recording,
            contentDescription = if (recording) {
                stringResource(R.string.note_stop_recording)
            } else {
                stringResource(R.string.note_record_audio)
            },
            imageVector = if (recording) Icons.Default.Stop else Icons.Default.Mic
        )
        if (recording) {
            Spacer(Modifier.width(2.dp))
            Text(
                text = stringResource(R.string.note_recording),
                color = AppColors.Overdue,
                style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
            )
        }
    }
}

@Composable
private fun AttachmentIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    selected: Boolean = false,
    imageVector: ImageVector
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) AppColors.Brand else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = if (selected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private data class LoadedNoteImage(
    val bitmap: Bitmap,
    val aspectRatio: Float
)

private fun loadNoteImage(context: Context, ref: String, targetWidthPx: Int): LoadedNoteImage? {
    val file = NoteAttachmentStore.resolve(context, ref) ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    val sourceWidth = bounds.outWidth.takeIf { it > 0 } ?: return null
    val sourceHeight = bounds.outHeight.takeIf { it > 0 } ?: return null
    val bitmap = BitmapFactory.decodeFile(
        file.absolutePath,
        BitmapFactory.Options().apply {
            inSampleSize = calculateImageSampleSize(sourceWidth, targetWidthPx)
        }
    ) ?: return null
    return LoadedNoteImage(
        bitmap = bitmap,
        aspectRatio = sourceWidth.toFloat() / sourceHeight.toFloat()
    )
}

private fun calculateImageSampleSize(width: Int, targetWidth: Int): Int {
    var sample = 1
    while (width / sample > targetWidth * 2) sample *= 2
    return sample
}

private fun createNoteMediaRecorder(context: Context, file: File): MediaRecorder {
    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
    }
    return recorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioEncodingBitRate(96_000)
        setAudioSamplingRate(44_100)
        setOutputFile(file.absolutePath)
    }
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

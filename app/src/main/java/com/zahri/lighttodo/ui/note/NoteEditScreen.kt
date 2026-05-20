package com.zahri.lighttodo.ui.note

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zahri.lighttodo.R
import com.zahri.lighttodo.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val contentHint = stringResource(R.string.note_content_hint)
    val noteBackground = if (isDark) Color.Black else Color(0xFFFFFCF6)

    val markdownEditText = remember { MarkdownEditText(context) }

    fun leaveNote() {
        hideNoteKeyboard(context, view, markdownEditText)
        vm.save()
        onBack()
    }

    LaunchedEffect(Unit) { vm.load(editingId) }

    // Sync ViewModel content → EditText
    LaunchedEffect(content) {
        if (content != markdownEditText.editableText.toString()) {
            markdownEditText.setContentWithoutTrigger(content)
        }
    }

    // Connect EditText → ViewModel
    DisposableEffect(markdownEditText) {
        markdownEditText.contentUpdateCallback = { vm.updateContent(it) }
        onDispose { markdownEditText.contentUpdateCallback = null }
    }

    // Auto-save on leave
    DisposableEffect(Unit) {
        onDispose { vm.save() }
    }

    BackHandler {
        leaveNote()
    }

    // Apply theme colors to EditText
    LaunchedEffect(isDark, contentHint) {
        val textColor = if (isDark) 0xFFFFFFFF.toInt() else 0xFF202124.toInt()
        val hintColor = 0xFF8E8E93.toInt()
        markdownEditText.setTextColor(textColor)
        markdownEditText.setHintTextColor(hintColor)
        markdownEditText.hint = contentHint
    }

    Scaffold(
        containerColor = noteBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .imePadding()
        ) {
            // ── Top bar ──────────────────────────────────────
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
                        hideNoteKeyboard(context, view, markdownEditText)
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

            // ── Scrollable content ───────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp)
            ) {
                // Title
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
                    Spacer(Modifier.height(28.dp))
                }

                // Content — AndroidView wrapping MarkdownEditText
                AndroidView(
                    factory = { markdownEditText },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 360.dp)
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

private fun hideNoteKeyboard(context: Context, fallbackView: View, editText: MarkdownEditText) {
    editText.clearFocus()
    val token = editText.windowToken ?: fallbackView.windowToken
    context.getSystemService(InputMethodManager::class.java)
        ?.hideSoftInputFromWindow(token, 0)
}

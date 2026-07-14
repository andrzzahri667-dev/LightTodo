package com.zahri.lighttodo.feature.noteeditor

import android.text.Editable
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zahri.lighttodo.R
import com.zahri.lighttodo.domain.markdown.MarkdownTextTransforms
import com.zahri.lighttodo.ui.theme.AppColors

enum class MarkdownFormatAction {
    Bold,
    Italic,
    Strikethrough,
    H1,
    H2,
    H3,
    Bullet,
    OrderedList,
    Checkbox,
    Quote,
    Underline
}

data class MarkdownStyleState(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strikethrough: Boolean = false,
    val underline: Boolean = false,
    val h1: Boolean = false,
    val h2: Boolean = false,
    val h3: Boolean = false,
    val bullet: Boolean = false,
    val orderedList: Boolean = false,
    val checkbox: Boolean = false,
    val quote: Boolean = false
)

class MarkdownFormattingController {
    private var editText: EditText? = null
    private var lastSelectionStart = 0
    private var lastSelectionEnd = 0

    fun bind(editText: EditText): MarkdownStyleState {
        this.editText = editText
        val start = editText.selectionStart.coerceAtLeast(0)
        val end = editText.selectionEnd.coerceAtLeast(0)
        lastSelectionStart = start
        lastSelectionEnd = end
        return MarkdownToolbarHelper.detectState(editText)
    }

    fun onSelectionChanged(editText: EditText, start: Int, end: Int): MarkdownStyleState {
        this.editText = editText
        if (start >= 0 && end >= 0) {
            lastSelectionStart = start
            lastSelectionEnd = end
        }
        return MarkdownToolbarHelper.detectState(editText)
    }

    fun apply(action: MarkdownFormatAction): MarkdownStyleState {
        val target = editText ?: return MarkdownStyleState()
        target.requestFocus()
        restoreSelectionIfNeeded(target)
        MarkdownToolbarHelper.apply(target, action)
        return MarkdownToolbarHelper.detectState(target)
    }

    private fun restoreSelectionIfNeeded(target: EditText) {
        if (target.selectionStart >= 0 && target.selectionEnd >= 0) return
        val start = lastSelectionStart.coerceIn(0, target.editableText.length)
        val end = lastSelectionEnd.coerceIn(0, target.editableText.length)
        target.setSelection(minOf(start, end), maxOf(start, end))
    }
}

@Composable
fun MarkdownFormattingToolbar(
    styleState: MarkdownStyleState,
    onAction: (MarkdownFormatAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
        ) {
            FormatActionSlot {
                FormatIconButton(
                    icon = Icons.Default.FormatBold,
                    contentDescription = stringResource(R.string.note_format_bold),
                    active = styleState.bold
                ) {
                    onAction(MarkdownFormatAction.Bold)
                }
            }
            FormatActionSlot {
                FormatIconButton(
                    icon = Icons.Default.FormatItalic,
                    contentDescription = stringResource(R.string.note_format_italic),
                    active = styleState.italic
                ) {
                    onAction(MarkdownFormatAction.Italic)
                }
            }
            FormatActionSlot {
                FormatIconButton(
                    icon = Icons.Default.FormatStrikethrough,
                    contentDescription = stringResource(R.string.note_format_strikethrough),
                    active = styleState.strikethrough
                ) {
                    onAction(MarkdownFormatAction.Strikethrough)
                }
            }
            FormatActionSlot {
                FormatIconButton(
                    icon = Icons.Default.FormatUnderlined,
                    contentDescription = stringResource(R.string.note_format_underline),
                    active = styleState.underline
                ) {
                    onAction(MarkdownFormatAction.Underline)
                }
            }
            FormatGroupDivider()
            FormatActionSlot {
                FormatTextButton(
                    text = "H1",
                    contentDescription = stringResource(R.string.note_format_heading_1),
                    active = styleState.h1
                ) { onAction(MarkdownFormatAction.H1) }
            }
            FormatActionSlot {
                FormatTextButton(
                    text = "H2",
                    contentDescription = stringResource(R.string.note_format_heading_2),
                    active = styleState.h2
                ) { onAction(MarkdownFormatAction.H2) }
            }
            FormatActionSlot {
                FormatTextButton(
                    text = "H3",
                    contentDescription = stringResource(R.string.note_format_heading_3),
                    active = styleState.h3
                ) { onAction(MarkdownFormatAction.H3) }
            }
            FormatGroupDivider()
            FormatActionSlot {
                FormatIconButton(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = stringResource(R.string.note_format_bullet_list),
                    active = styleState.bullet
                ) {
                    onAction(MarkdownFormatAction.Bullet)
                }
            }
            FormatActionSlot {
                FormatIconButton(
                    icon = Icons.Default.FormatListNumbered,
                    contentDescription = stringResource(R.string.note_format_numbered_list),
                    active = styleState.orderedList
                ) {
                    onAction(MarkdownFormatAction.OrderedList)
                }
            }
            FormatActionSlot {
                FormatIconButton(
                    icon = Icons.Default.CheckBox,
                    contentDescription = stringResource(R.string.note_format_checkbox),
                    active = styleState.checkbox
                ) {
                    onAction(MarkdownFormatAction.Checkbox)
                }
            }
            FormatActionSlot {
                FormatIconButton(
                    icon = Icons.Default.FormatQuote,
                    contentDescription = stringResource(R.string.note_format_quote),
                    active = styleState.quote
                ) {
                    onAction(MarkdownFormatAction.Quote)
                }
            }
        }
        FormatGroupDivider()
        FormatActionSlot {
            FormatIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.note_format_back),
                active = false,
                reportsSelection = false,
                onClick = onBack
            )
        }
    }
}

@Composable
private fun FormatActionSlot(content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.width(50.dp)
    ) {
        content()
    }
}

@Composable
private fun FormatGroupDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun FormatIconButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    reportsSelection: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (active) AppColors.Brand.copy(alpha = 0.14f) else Color.Transparent
    val tint = if (active) formattingActiveColor() else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bg)
            .semantics {
                this.contentDescription = contentDescription
                if (reportsSelection) selected = active
            }
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun FormatTextButton(
    text: String,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val bg = if (active) AppColors.Brand.copy(alpha = 0.14f) else Color.Transparent
    val color = if (active) formattingActiveColor() else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bg)
            .semantics {
                this.contentDescription = contentDescription
                selected = active
            }
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Text(text, color = color, fontSize = 16.sp)
    }
}

@Composable
private fun formattingActiveColor(): Color =
    if (isSystemInDarkTheme()) AppColors.BrandForegroundDark else AppColors.BrandForegroundLight

object MarkdownToolbarHelper {
    fun apply(edit: EditText, action: MarkdownFormatAction) {
        when (action) {
            MarkdownFormatAction.Bold -> toggleInline(edit, "**")
            MarkdownFormatAction.Italic -> toggleInline(edit, "*")
            MarkdownFormatAction.Strikethrough -> toggleInline(edit, "~~")
            MarkdownFormatAction.H1 -> toggleLinePrefix(edit, "# ")
            MarkdownFormatAction.H2 -> toggleLinePrefix(edit, "## ")
            MarkdownFormatAction.H3 -> toggleLinePrefix(edit, "### ")
            MarkdownFormatAction.Bullet -> toggleLinePrefix(edit, "- ")
            MarkdownFormatAction.OrderedList -> toggleOrderedList(edit)
            MarkdownFormatAction.Checkbox -> toggleCheckbox(edit)
            MarkdownFormatAction.Quote -> toggleLinePrefix(edit, "> ")
            MarkdownFormatAction.Underline -> toggleInline(edit, "<u>", "</u>")
        }
    }

    fun detectState(edit: EditText): MarkdownStyleState {
        val start = edit.selectionStart
        val end = edit.selectionEnd
        if (start < 0 || end < 0) return MarkdownStyleState()
        val text = edit.editableText
        val cursor = start.coerceIn(0, text.length)
        val (lineStart, lineEnd) = findLineRange(text, cursor)
        val line = text.subSequence(lineStart, lineEnd).toString()
        val trimmed = line.trimStart()
        val bold = MarkdownTextTransforms.isSelectionInsideInlineStyle(
            text = text,
            selectionStart = start,
            selectionEnd = end,
            openMarker = "**",
            closeMarker = "**"
        )

        return MarkdownStyleState(
            bold = bold,
            italic = !bold && MarkdownTextTransforms.isSelectionInsideInlineStyle(
                text = text,
                selectionStart = start,
                selectionEnd = end,
                openMarker = "*",
                closeMarker = "*"
            ),
            strikethrough = MarkdownTextTransforms.isSelectionInsideInlineStyle(
                text = text,
                selectionStart = start,
                selectionEnd = end,
                openMarker = "~~",
                closeMarker = "~~"
            ),
            underline = MarkdownTextTransforms.isSelectionInsideInlineStyle(
                text = text,
                selectionStart = start,
                selectionEnd = end,
                openMarker = "<u>",
                closeMarker = "</u>"
            ),
            h1 = trimmed.startsWith("# ") && !trimmed.startsWith("## "),
            h2 = trimmed.startsWith("## ") && !trimmed.startsWith("### "),
            h3 = trimmed.startsWith("### "),
            bullet = Regex("^\\s*[-*+]\\s+[^\\[]").containsMatchIn(line) &&
                !Regex("^\\s*[-*+]\\s+\\[").containsMatchIn(line),
            orderedList = Regex("^\\s*\\d+[.)]\\s+").containsMatchIn(line),
            checkbox = Regex("^\\s*[-*+]\\s+\\[").containsMatchIn(line),
            quote = trimmed.startsWith("> ")
        )
    }

    private fun toggleInline(edit: EditText, marker: String) {
        toggleInline(edit, marker, marker)
    }

    private fun toggleInline(edit: EditText, openMarker: String, closeMarker: String) {
        val rawStart = edit.selectionStart
        val rawEnd = edit.selectionEnd
        if (rawStart < 0 || rawEnd < 0) return
        val start = minOf(rawStart, rawEnd)
        val end = maxOf(rawStart, rawEnd)
        val text = edit.editableText

        if (start != end) {
            val before = if (start >= openMarker.length) {
                text.subSequence(start - openMarker.length, start).toString()
            } else {
                ""
            }
            val after = if (end + closeMarker.length <= text.length) {
                text.subSequence(end, end + closeMarker.length).toString()
            } else {
                ""
            }
            if (before == openMarker && after == closeMarker) {
                text.delete(end, end + closeMarker.length)
                text.delete(start - openMarker.length, start)
                edit.setSelection(start - openMarker.length, end - openMarker.length)
            } else {
                text.insert(end, closeMarker)
                text.insert(start, openMarker)
                edit.setSelection(start + openMarker.length, end + openMarker.length)
            }
        } else {
            val editOp = MarkdownTextTransforms.toggleInlineStyleAtCursor(
                text = text.toString(),
                cursor = start,
                openMarker = openMarker,
                closeMarker = closeMarker
            )
            text.replace(editOp.start, editOp.end, editOp.replacement)
            edit.setSelection(editOp.cursorAfter.coerceIn(0, text.length))
        }
    }

    private fun toggleLinePrefix(edit: EditText, prefix: String) {
        val text = edit.editableText
        val cursor = edit.selectionStart.coerceAtLeast(0)
        val editOp = MarkdownTextTransforms.toggleLinePrefix(text.toString(), cursor, prefix) ?: return
        text.replace(editOp.start, editOp.end, editOp.replacement)
        edit.setSelection(editOp.cursorAfter.coerceIn(0, text.length))
    }

    private fun toggleOrderedList(edit: EditText) {
        val text = edit.editableText
        val cursor = edit.selectionStart.coerceAtLeast(0)
        val editOp = MarkdownTextTransforms.toggleOrderedListPrefix(text.toString(), cursor) ?: return
        text.replace(editOp.start, editOp.end, editOp.replacement)
        edit.setSelection(editOp.cursorAfter.coerceIn(0, text.length))
    }

    private fun toggleCheckbox(edit: EditText) {
        val text = edit.editableText
        val cursor = edit.selectionStart.coerceAtLeast(0)
        val (lineStart, lineEnd) = findLineRange(text, cursor)
        val line = text.subSequence(lineStart, lineEnd).toString()
        val match = Regex("^(\\s*)[-*+]\\s+\\[([ xX])?]\\s*(.*)$").matchEntire(line)

        if (match != null) {
            val indent = match.groupValues[1]
            val content = match.groupValues[3]
            val newLine = if (content.isNotEmpty()) "$indent$content" else ""
            text.replace(lineStart, lineEnd, newLine)
            edit.setSelection((lineStart + newLine.length).coerceIn(0, text.length))
        } else {
            val trimmed = line.trimStart()
            val indentLen = line.length - trimmed.length
            val indent = line.substring(0, indentLen)
            val newLine = "${indent}- [ ] $trimmed"
            text.replace(lineStart, lineEnd, newLine)
            edit.setSelection((lineStart + newLine.length).coerceIn(0, text.length))
        }
    }

    private fun findLineRange(text: Editable, pos: Int): Pair<Int, Int> {
        val safePos = pos.coerceIn(0, text.length)
        val lineStart = if (safePos == 0) {
            0
        } else {
            text.lastIndexOf('\n', startIndex = safePos - 1).let {
                if (it == -1) 0 else it + 1
            }
        }
        val lineEnd = text.indexOf('\n', startIndex = safePos).let {
            if (it == -1) text.length else it
        }
        return lineStart to lineEnd
    }
}

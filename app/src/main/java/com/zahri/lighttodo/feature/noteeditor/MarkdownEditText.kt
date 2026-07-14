package com.zahri.lighttodo.feature.noteeditor

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.EditText
import com.zahri.lighttodo.domain.markdown.MarkdownEditorPolicy
import com.zahri.lighttodo.domain.markdown.MarkdownKeyboardDeletePolicy
import com.zahri.lighttodo.domain.markdown.MarkdownTextTransforms
import com.zahri.lighttodo.domain.note.NoteAttachmentMarkdown

/**
 * EditText that applies Markdown spans in real-time.
 * Syntax characters stay visible but dimmed; content gets styled.
 */
@SuppressLint("AppCompatCustomView")
class MarkdownEditText(context: Context) : EditText(context) {

    private data class PendingTextChange(
        val start: Int,
        val removedText: String,
        val insertedText: String,
        val beforeLine: String,
        val afterLine: String
    )

    var contentUpdateCallback: ((String) -> Unit)? = null
    var audioClickCallback: ((String) -> Unit)? = null
    var imageClickCallback: ((String) -> Unit)? = null
    var attachmentLongClickCallback: ((NoteAttachmentMarkdown.Attachment) -> Unit)? = null
    var linkClickCallback: ((String) -> Unit)? = null
    var selectionChangedCallback: ((Int, Int) -> Unit)? = null
    var deletePreviousMediaCallback: (() -> Boolean)? = null
    var attachmentResolver: NoteAttachmentResolver? = null

    private var isApplyingSpans = false
    private var isPasting = false
    private var pendingTextChange: PendingTextChange? = null
    private var lastActiveOffset: Int? = null
    private var pressedAttachment: NoteAttachmentMarkdown.Attachment? = null
    private var longPressTriggered = false
    private var longPressRunnable: Runnable? = null
    private var renderStyle: MarkdownRenderStyle? = null
    private val currentRenderStyle: MarkdownRenderStyle
        get() = renderStyle ?: MarkdownRenderStyle.forDarkMode(false)
    private val taskToggleTouchWidthPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        64f,
        resources.displayMetrics
    )

    init {
        val initialStyle = MarkdownRenderStyle.forDarkMode(false)
        renderStyle = initialStyle
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setLineSpacing(0f, 1.32f)
        setPadding(0, 0, 0, 0)
        setBackgroundColor(Color.TRANSPARENT)
        setTextColor(initialStyle.bodyTextColor)
        setHintTextColor(initialStyle.hintTextColor)
        filters = arrayOf(InputFilter.LengthFilter(20_000))
        gravity = Gravity.TOP or Gravity.START
        inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        setHorizontallyScrolling(false)
        hint = ""

        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                if (isApplyingSpans) return
                val removedEnd = (start + count).coerceAtMost(s.length)
                pendingTextChange = PendingTextChange(
                    start = start,
                    removedText = s.subSequence(start, removedEnd).toString(),
                    insertedText = "",
                    beforeLine = lineTextAt(s, start),
                    afterLine = ""
                )
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (isApplyingSpans) return
                val insertedEnd = (start + count).coerceAtMost(s.length)
                val previous = pendingTextChange
                pendingTextChange = PendingTextChange(
                    start = start,
                    removedText = previous?.removedText.orEmpty(),
                    insertedText = s.subSequence(start, insertedEnd).toString(),
                    beforeLine = previous?.beforeLine.orEmpty(),
                    afterLine = lineTextAt(s, start)
                )
            }

            override fun afterTextChanged(s: Editable) {
                if (isApplyingSpans) return
                val change = pendingTextChange
                pendingTextChange = null
                isApplyingSpans = true
                try {
                    val newlineIndex = change?.let {
                        MarkdownEditorPolicy.continuationNewlineIndex(
                            changeStart = it.start,
                            insertedText = it.insertedText,
                            isPaste = isPasting
                        )
                    }
                    if (newlineIndex != null) applyPendingListContinuation(s, newlineIndex)
                    val fullRefresh = change == null || MarkdownEditorPolicy.requiresFullSpanRefresh(
                        removedText = change.removedText,
                        insertedText = change.insertedText,
                        beforeLine = change.beforeLine,
                        afterLine = change.afterLine
                    )
                    val activeOffset = selectionStart.takeIf { it >= 0 }
                    val changedOffset = change?.start
                    if (fullRefresh || changedOffset == null) {
                        MarkdownSpanApplier.apply(
                            editable = s,
                            activeOffset = activeOffset,
                            context = context,
                            renderStyle = currentRenderStyle,
                            resolveAttachment = attachmentResolver
                        )
                    } else {
                        MarkdownSpanApplier.applyChangedLine(
                            editable = s,
                            changedOffset = changedOffset,
                            activeOffset = activeOffset,
                            context = context,
                            renderStyle = currentRenderStyle,
                            resolveAttachment = attachmentResolver
                        )
                    }
                    lastActiveOffset = activeOffset
                    contentUpdateCallback?.invoke(s.toString())
                } finally {
                    isApplyingSpans = false
                }
            }
        })
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (isApplyingSpans) return
        selectionChangedCallback?.invoke(selStart.coerceAtLeast(0), selEnd.coerceAtLeast(0))
        val previousActiveOffset = lastActiveOffset
        val activeOffset = selStart.takeIf { it >= 0 }
        lastActiveOffset = activeOffset
        if (pendingTextChange != null) return
        isApplyingSpans = true
        try {
            MarkdownSpanApplier.applyActiveLineChange(
                editable = editableText,
                previousActiveOffset = previousActiveOffset,
                activeOffset = activeOffset,
                context = context,
                renderStyle = currentRenderStyle,
                resolveAttachment = attachmentResolver
            )
        } finally {
            isApplyingSpans = false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                findAttachmentAt(event)?.let { attachment ->
                    pressedAttachment = attachment
                    longPressTriggered = false
                    val runnable = Runnable {
                        longPressTriggered = true
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        attachmentLongClickCallback?.invoke(attachment)
                    }
                    longPressRunnable = runnable
                    postDelayed(runnable, ViewConfiguration.getLongPressTimeout().toLong())
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                cancelAttachmentLongPress()
                val attachment = pressedAttachment
                pressedAttachment = null
                if (attachment != null) {
                    if (!longPressTriggered) {
                        when (attachment.kind) {
                            NoteAttachmentMarkdown.Kind.Image -> imageClickCallback?.invoke(attachment.ref)
                            NoteAttachmentMarkdown.Kind.Audio -> audioClickCallback?.invoke(attachment.ref)
                        }
                    }
                    return true
                }
                // Check for link click
                val link = findLinkSpanAt(event)
                if (link != null) {
                    linkClickCallback?.invoke(link)
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelAttachmentLongPress()
                pressedAttachment = null
            }
        }
        if (event.action == MotionEvent.ACTION_UP && event.x <= taskToggleTouchWidthPx) {
            val layout = layout
            if (layout != null) {
                val vertical = (event.y + scrollY - totalPaddingTop).toInt()
                if (!MarkdownEditorPolicy.containsVertical(vertical, layout.height)) {
                    return super.onTouchEvent(event)
                }
                val lineIndex = layout.getLineForVertical(vertical)
                val lineStart = layout.getLineStart(lineIndex)
                val rawLineEnd = layout.getLineEnd(lineIndex)
                val lineEnd = if (
                    rawLineEnd > lineStart &&
                    editableText.getOrNull(rawLineEnd - 1) == '\n'
                ) {
                    rawLineEnd - 1
                } else {
                    rawLineEnd
                }
                val line = editableText.subSequence(lineStart, lineEnd).toString()
                val toggled = MarkdownTextTransforms.toggleTaskListLine(line)
                if (toggled != null) {
                    editableText.replace(lineStart, lineEnd, toggled)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL && tryDeletePreviousMediaFromKeyboard()) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        val pasteAction = id == android.R.id.paste || id == android.R.id.pasteAsPlainText
        if (!pasteAction) return super.onTextContextMenuItem(id)

        isPasting = true
        return try {
            val pasted = clipboardText()
            if (replaceSelectionWithPastedMarkdownLink(pasted)) {
                true
            } else {
                super.onTextContextMenuItem(id)
            }
        } finally {
            isPasting = false
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val inputConnection = super.onCreateInputConnection(outAttrs) ?: return null
        return object : InputConnectionWrapper(inputConnection, true) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                val markdownLink = text
                    ?.toString()
                    ?.let(MarkdownTextTransforms::markdownLinkForPastedText)
                return super.commitText(markdownLink ?: text, newCursorPosition)
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (beforeLength > 0 && afterLength == 0 && tryDeletePreviousMediaFromKeyboard()) {
                    return true
                }
                return super.deleteSurroundingText(beforeLength, afterLength)
            }

            override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
                if (beforeLength > 0 && afterLength == 0 && tryDeletePreviousMediaFromKeyboard()) {
                    return true
                }
                return super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
            }
        }
    }

    /** Sets text from ViewModel without triggering the TextWatcher callback. */
    fun setContentWithoutTrigger(text: String) {
        isApplyingSpans = true
        try {
            val selection = MarkdownEditorPolicy.clampSelection(
                start = selectionStart,
                end = selectionEnd,
                textLength = text.length
            )
            setText(text)
            MarkdownSpanApplier.apply(
                editable = editableText,
                activeOffset = selection.start,
                context = context,
                renderStyle = currentRenderStyle,
                resolveAttachment = attachmentResolver
            )
            setSelection(selection.start, selection.end)
            lastActiveOffset = selection.start
        } finally {
            isApplyingSpans = false
        }
    }

    fun insertAttachmentMarkdown(markdown: String) {
        val start = selectionStart.coerceAtLeast(0)
        val end = selectionEnd.coerceAtLeast(0)
        val from = minOf(start, end)
        val to = maxOf(start, end)
        val before = editableText.substring(0, from)
        val after = editableText.substring(to, editableText.length)
        val prefix = if (before.isEmpty() || before.endsWith('\n')) "" else "\n"
        val suffix = if (after.isEmpty() || after.startsWith('\n')) "\n" else "\n"
        val inserted = "$prefix$markdown$suffix"
        editableText.replace(from, to, inserted)
        setSelection((from + inserted.length).coerceIn(0, editableText.length))
    }

    fun setMarkdownAppearance(isDark: Boolean) {
        val nextStyle = MarkdownRenderStyle.forDarkMode(isDark)
        if (renderStyle == nextStyle) return
        renderStyle = nextStyle
        setTextColor(nextStyle.bodyTextColor)
        setHintTextColor(nextStyle.hintTextColor)
        isApplyingSpans = true
        try {
            MarkdownSpanApplier.apply(
                editable = editableText,
                activeOffset = selectionStart.takeIf { it >= 0 },
                context = context,
                renderStyle = nextStyle,
                resolveAttachment = attachmentResolver
            )
        } finally {
            isApplyingSpans = false
        }
    }

    fun removeAttachment(ref: String) {
        val next = NoteAttachmentMarkdown.removeAttachment(editableText.toString(), ref)
        editableText.replace(0, editableText.length, next)
        setSelection(selectionStart.coerceIn(0, editableText.length))
    }

    private fun replaceSelectionWithPastedMarkdownLink(text: String?): Boolean {
        val markdownLink = text?.let(MarkdownTextTransforms::markdownLinkForPastedText) ?: return false
        val start = selectionStart.coerceAtLeast(0)
        val end = selectionEnd.coerceAtLeast(0)
        val from = minOf(start, end)
        val to = maxOf(start, end)
        editableText.replace(from, to, markdownLink)
        setSelection((from + markdownLink.length).coerceIn(0, editableText.length))
        return true
    }

    private fun clipboardText(): String? {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        return clipboard
            ?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
    }

    private fun tryDeletePreviousMediaFromKeyboard(): Boolean {
        val text = editableText.toString()
        if (!MarkdownKeyboardDeletePolicy.shouldRequestPreviousMediaDelete(
                text = text,
                selectionStart = selectionStart,
                selectionEnd = selectionEnd
            )
        ) {
            return false
        }
        return deletePreviousMediaCallback?.invoke() == true
    }

    private fun applyPendingListContinuation(s: Editable, newlineIndex: Int) {
        val edit = MarkdownTextTransforms.listContinuationAfterNewline(s.toString(), newlineIndex) ?: return
        s.replace(edit.start, edit.end, edit.replacement)
        val cursor = edit.cursorAfter.coerceIn(0, s.length)
        setSelection(cursor)
    }

    private fun findAttachmentAt(event: MotionEvent): NoteAttachmentMarkdown.Attachment? {
        findSpanAt(event, MarkdownAudioSpan::class.java)?.let { return it.attachment }
        findSpanAt(event, MarkdownImageSpan::class.java)?.let { return it.attachment }
        return null
    }

    private fun <T> findSpanAt(event: MotionEvent, type: Class<T>): T? {
        val layout = layout ?: return null
        val vertical = (event.y + scrollY - totalPaddingTop).toInt()
        if (!MarkdownEditorPolicy.containsVertical(vertical, layout.height)) return null
        val lineIndex = layout.getLineForVertical(vertical)
        val offset = layout.getOffsetForHorizontal(lineIndex, event.x)
        val spans = editableText.getSpans(0, editableText.length, type)
        return spans.firstOrNull { span ->
            val start = editableText.getSpanStart(span as Any)
            val end = editableText.getSpanEnd(span as Any)
            MarkdownEditorPolicy.containsOffset(offset, start, end)
        }
    }

    private fun findLinkSpanAt(event: MotionEvent): String? {
        val layout = layout ?: return null
        val vertical = (event.y + scrollY - totalPaddingTop).toInt()
        if (!MarkdownEditorPolicy.containsVertical(vertical, layout.height)) return null
        val lineIndex = layout.getLineForVertical(vertical)
        val offset = layout.getOffsetForHorizontal(lineIndex, event.x)
        val spans = editableText.getSpans(0, editableText.length, MarkdownLinkSpan::class.java)
        return spans.firstOrNull { span ->
            val start = editableText.getSpanStart(span)
            val end = editableText.getSpanEnd(span)
            MarkdownEditorPolicy.containsOffset(offset, start, end)
        }?.url
    }

    private fun lineTextAt(text: CharSequence, offset: Int): String {
        val safeOffset = offset.coerceIn(0, text.length)
        val lineStart = if (safeOffset == 0) {
            0
        } else {
            text.lastIndexOf('\n', safeOffset - 1).let { if (it == -1) 0 else it + 1 }
        }
        val lineEnd = text.indexOf('\n', safeOffset).let {
            if (it == -1) text.length else it
        }
        return text.subSequence(lineStart, lineEnd).toString()
    }

    private fun cancelAttachmentLongPress() {
        longPressRunnable?.let { removeCallbacks(it) }
        longPressRunnable = null
    }
}

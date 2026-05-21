package com.zahri.lighttodo.ui.note

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
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.EditText

/**
 * EditText that applies Markdown spans in real-time.
 * Syntax characters stay visible but dimmed; content gets styled.
 */
class MarkdownEditText(context: Context) : EditText(context) {

    var contentUpdateCallback: ((String) -> Unit)? = null
    var audioClickCallback: ((String) -> Unit)? = null
    var imageClickCallback: ((String) -> Unit)? = null
    var attachmentLongClickCallback: ((NoteAttachmentMarkdown.Attachment) -> Unit)? = null
    var linkClickCallback: ((String) -> Unit)? = null
    var selectionChangedCallback: ((Int, Int) -> Unit)? = null

    private var isApplyingSpans = false
    private var pendingNewlineIndex: Int? = null
    private var pressedAttachment: NoteAttachmentMarkdown.Attachment? = null
    private var longPressTriggered = false
    private var longPressRunnable: Runnable? = null
    private val taskToggleTouchWidthPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        64f,
        resources.displayMetrics
    )

    init {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setLineSpacing(0f, 1.32f)
        setPadding(0, 0, 0, 0)
        setBackgroundColor(Color.TRANSPARENT)
        setTextColor(Color.BLACK)
        setHintTextColor(Color.parseColor("#8E8E93"))
        filters = arrayOf(InputFilter.LengthFilter(20_000))
        gravity = Gravity.TOP or Gravity.START
        inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        setHorizontallyScrolling(false)
        hint = ""

        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (isApplyingSpans || count <= 0) return
                val insertedEnd = (start + count).coerceAtMost(s.length)
                val newlineOffset = s.subSequence(start, insertedEnd).indexOf('\n')
                if (newlineOffset >= 0) {
                    pendingNewlineIndex = start + newlineOffset
                }
            }

            override fun afterTextChanged(s: Editable) {
                if (isApplyingSpans) return
                isApplyingSpans = true
                try {
                    applyPendingListContinuation(s)
                    MarkdownSpanApplier.apply(s, selectionStart.takeIf { it >= 0 }, context)
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
        isApplyingSpans = true
        try {
            MarkdownSpanApplier.apply(editableText, selStart.takeIf { it >= 0 }, context)
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
                    MarkdownSpanApplier.apply(editableText, selectionStart.takeIf { it >= 0 }, context)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText) {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            val pasted = clipboard
                ?.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(context)
                ?.toString()
            if (replaceSelectionWithPastedMarkdownLink(pasted)) return true
        }
        return super.onTextContextMenuItem(id)
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
        }
    }

    /** Sets text from ViewModel without triggering the TextWatcher callback. */
    fun setContentWithoutTrigger(text: String) {
        isApplyingSpans = true
        try {
            val sel = selectionStart.coerceAtMost(text.length).coerceAtLeast(0)
            setText(text)
            MarkdownSpanApplier.apply(editableText, sel, context)
            setSelection(sel)
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

    private fun applyPendingListContinuation(s: Editable) {
        val newlineIndex = pendingNewlineIndex ?: return
        pendingNewlineIndex = null
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
        val lineIndex = layout.getLineForVertical(vertical)
        val offset = layout.getOffsetForHorizontal(lineIndex, event.x)
        val spans = editableText.getSpans(0, editableText.length, type)
        return spans.firstOrNull { span ->
            val start = editableText.getSpanStart(span as Any)
            val end = editableText.getSpanEnd(span as Any)
            offset in start..end
        }
    }

    private fun findLinkSpanAt(event: MotionEvent): String? {
        val layout = layout ?: return null
        val vertical = (event.y + scrollY - totalPaddingTop).toInt()
        val lineIndex = layout.getLineForVertical(vertical)
        val offset = layout.getOffsetForHorizontal(lineIndex, event.x)
        val spans = editableText.getSpans(0, editableText.length, MarkdownLinkSpan::class.java)
        return spans.firstOrNull { span ->
            val start = editableText.getSpanStart(span)
            val end = editableText.getSpanEnd(span)
            offset in start..end
        }?.url
    }

    private fun cancelAttachmentLongPress() {
        longPressRunnable?.let { removeCallbacks(it) }
        longPressRunnable = null
    }
}

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
import android.view.MotionEvent
import android.widget.EditText

/**
 * EditText that applies Markdown spans in real-time.
 * Syntax characters stay visible but dimmed; content gets styled.
 */
class MarkdownEditText(context: Context) : EditText(context) {

    var contentUpdateCallback: ((String) -> Unit)? = null

    private var isApplyingSpans = false
    private var pendingNewlineIndex: Int? = null
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
                    MarkdownSpanApplier.apply(s, selectionStart.takeIf { it >= 0 })
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
        isApplyingSpans = true
        try {
            MarkdownSpanApplier.apply(editableText, selStart.takeIf { it >= 0 })
        } finally {
            isApplyingSpans = false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
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
                    MarkdownSpanApplier.apply(editableText, selectionStart.takeIf { it >= 0 })
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
            val markdownLink = pasted?.let(MarkdownTextTransforms::markdownLinkForPastedText)
            if (markdownLink != null) {
                val start = selectionStart.coerceAtLeast(0)
                val end = selectionEnd.coerceAtLeast(0)
                editableText.replace(minOf(start, end), maxOf(start, end), markdownLink)
                return true
            }
        }
        return super.onTextContextMenuItem(id)
    }

    /** Sets text from ViewModel without triggering the TextWatcher callback. */
    fun setContentWithoutTrigger(text: String) {
        isApplyingSpans = true
        try {
            val sel = selectionStart.coerceAtMost(text.length).coerceAtLeast(0)
            setText(text)
            MarkdownSpanApplier.apply(editableText, sel)
            setSelection(sel)
        } finally {
            isApplyingSpans = false
        }
    }

    private fun applyPendingListContinuation(s: Editable) {
        val newlineIndex = pendingNewlineIndex ?: return
        pendingNewlineIndex = null
        val edit = MarkdownTextTransforms.listContinuationAfterNewline(s.toString(), newlineIndex) ?: return
        s.replace(edit.start, edit.end, edit.replacement)
        val cursor = edit.cursorAfter.coerceIn(0, s.length)
        setSelection(cursor)
    }
}

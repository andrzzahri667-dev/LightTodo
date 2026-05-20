package com.zahri.lighttodo.ui.note

import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan

/**
 * Applies Markdown formatting spans to an Editable buffer.
 * Called from MarkdownEditText's TextWatcher on every text change.
 *
 * Design: syntax characters remain visible but dimmed (MarkdownSyntaxSpan).
 * Content gets styled with Android spans for native rendering quality.
 */
object MarkdownSpanApplier {

    // ─── Colors ─────────────────────────────────────────────
    private const val QUOTE_TEXT_COLOR = "#8E8E93"
    private const val LINK_COLOR = "#FF9F0A"
    private const val CODE_BG = 0x0F000000  // very light gray

    // ─── Regex (same patterns as old MarkdownParser) ────────
    private val HeadingRegex = Regex("^(\\s*)(#{1,6})\\s+(.+)$")
    private val HorizontalRuleRegex = Regex("^\\s{0,3}([-*_])(?:\\s*\\1){2,}\\s*$")
    private val OrderedListRegex = Regex("^(\\s*)(\\d+)[.)]\\s+(.+)$")
    private val UnorderedListRegex = Regex("^(\\s*)[-*+]\\s+(.+)$")
    private val TaskListRegex = Regex("^(\\s*)[-*+]\\s+\\[([ xX]?)]\\s*(.*)$")

    /**
     * Strip markdown syntax for preview display.
     * Returns plain text suitable for a Text composable.
     */
    fun stripMarkdown(markdown: String): String {
        return markdown.lineSequence().mapNotNull { line ->
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("```") -> null
                HeadingRegex.matchEntire(line) != null ->
                    stripInlineMarkdown(HeadingRegex.matchEntire(line)!!.groupValues[3])
                HorizontalRuleRegex.matchEntire(line) != null -> null
                TaskListRegex.matchEntire(line) != null ->
                    stripInlineMarkdown(TaskListRegex.matchEntire(line)!!.groupValues[3])
                OrderedListRegex.matchEntire(line) != null ->
                    stripInlineMarkdown(OrderedListRegex.matchEntire(line)!!.groupValues[3])
                UnorderedListRegex.matchEntire(line) != null ->
                    stripInlineMarkdown(UnorderedListRegex.matchEntire(line)!!.groupValues[2])
                trimmed.startsWith(">") -> stripInlineMarkdown(trimmed.removePrefix(">").trimStart())
                else -> stripInlineMarkdown(line)
            }
        }.joinToString("\n").trim()
    }

    private fun stripInlineMarkdown(text: String): String {
        return text
            .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("__([^_]+)__"), "$1")
            .replace(Regex("~~([^~]+)~~"), "$1")
            .replace(Regex("~([^~]+)~"), "$1")
            .replace(Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)"), "$1")
            .replace(Regex("(?<!_)_([^_]+)_(?!_)"), "$1")
    }

    /** Managed span types — removed and re-applied each pass. */
    private val MANAGED_SPANS = arrayOf(
        MarkdownSpan::class.java,
        StyleSpan::class.java,
        StrikethroughSpan::class.java,
        ForegroundColorSpan::class.java,
        BackgroundColorSpan::class.java,
        UnderlineSpan::class.java
    )

    fun apply(editable: Editable, activeOffset: Int? = null) {
        // 1. Remove all managed spans
        for (type in MANAGED_SPANS) {
            val spans = editable.getSpans(0, editable.length, type)
            for (span in spans) {
                editable.removeSpan(span)
            }
        }

        // 2. Parse line by line
        var inCodeBlock = false
        var lineStart = 0
        val len = editable.length

        while (lineStart <= len) {
            val lineEnd = editable.indexOf('\n', lineStart).let { if (it == -1) len else it }
            val line = editable.subSequence(lineStart, lineEnd).toString()
            val trimmed = line.trimStart()
            val lineIsActive = activeOffset != null && activeOffset in lineStart..lineEnd

            // Code block fence toggle
            if (trimmed.startsWith("```")) {
                if (!lineIsActive) {
                    editable.setSpan(MarkdownSyntaxSpan(), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    editable.setSpan(MarkdownCodeBlockSpan(), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                inCodeBlock = !inCodeBlock
                lineStart = lineEnd + 1
                continue
            }

            if (inCodeBlock) {
                if (!lineIsActive) {
                    editable.setSpan(MarkdownCodeBlockSpan(), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    editable.setSpan(BackgroundColorSpan(CODE_BG), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                lineStart = lineEnd + 1
                continue
            }

            if (lineIsActive) {
                lineStart = lineEnd + 1
                continue
            }

            // Block-level patterns
            when {
                HeadingRegex.matchEntire(line) != null -> {
                    val match = HeadingRegex.matchEntire(line)!!
                    val indent = match.groupValues[1].length
                    val marker = match.groupValues[2]
                    val level = marker.length
                    val markerStart = lineStart + indent
                    val markerEnd = markerStart + marker.length + 1 // "# "
                    val contentStart = markerEnd
                    val contentEnd = lineEnd

                    // Dim the "# " syntax
                    if (markerEnd <= lineEnd) {
                        editable.setSpan(MarkdownSyntaxSpan(), markerStart, markerEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    // Style the heading content
                    editable.setSpan(MarkdownHeadingSpan(level), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    editable.setSpan(StyleSpan(Typeface.BOLD), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                HorizontalRuleRegex.matchEntire(line) != null -> {
                    // Dim the --- text, draw line via span
                    editable.setSpan(MarkdownSyntaxSpan(), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    editable.setSpan(MarkdownHrSpan(), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                TaskListRegex.matchEntire(line) != null -> {
                    val match = TaskListRegex.matchEntire(line)!!
                    val indent = match.groupValues[1].length
                    val checkedChar = match.groupValues[2]
                    val checked = checkedChar.equals("x", ignoreCase = true)

                    val markerStart = lineStart + indent
                    val markerEndInLine = line.contentStartAfterTaskMarker()
                    val contentStart = lineStart + markerEndInLine
                    val contentEnd = lineEnd

                    // Dim the "- [x] " syntax
                    editable.setSpan(MarkdownSyntaxSpan(), markerStart, contentStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    // Checkbox
                    editable.setSpan(MarkdownCheckboxSpan(checked), markerStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    if (checked) {
                        editable.setSpan(MarkdownCheckedAlphaSpan(), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(StrikethroughSpan(), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    // Parse inline within content
                    applyInline(editable, contentStart, contentEnd)
                }

                OrderedListRegex.matchEntire(line) != null -> {
                    val match = OrderedListRegex.matchEntire(line)!!
                    val indent = match.groupValues[1].length
                    val markerStart = lineStart + indent
                    val contentStart = line.indexOf(')').let { if (it == -1) line.indexOf('.') + 1 else it + 1 } + 1 + lineStart

                    editable.setSpan(MarkdownOrderedListSpan(), markerStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    applyInline(editable, contentStart, lineEnd)
                }

                UnorderedListRegex.matchEntire(line) != null -> {
                    val match = UnorderedListRegex.matchEntire(line)!!
                    val indent = match.groupValues[1].length
                    val markerStart = lineStart + indent
                    val contentStart = markerStart + 2

                    // Dim "- "
                    editable.setSpan(MarkdownSyntaxSpan(), markerStart, contentStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    // Bullet
                    editable.setSpan(MarkdownBulletSpan(), markerStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    applyInline(editable, contentStart, lineEnd)
                }

                isQuote(line) -> {
                    val quotePos = line.indexOf('>')
                    val contentStartInLine = (quotePos + 1).let { idx ->
                        var r = idx
                        while (r < line.length && line[r] == ' ') r++
                        r
                    }
                    val absContentStart = lineStart + contentStartInLine

                    // Dim "> "
                    editable.setSpan(MarkdownSyntaxSpan(), lineStart, absContentStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    // Quote indent + bar
                    editable.setSpan(MarkdownQuoteSpan(), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    // Muted text color
                    editable.setSpan(ForegroundColorSpan(Color.parseColor(QUOTE_TEXT_COLOR)), absContentStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    applyInline(editable, absContentStart, lineEnd)
                }

                else -> {
                    applyInline(editable, lineStart, lineEnd)
                }
            }

            lineStart = lineEnd + 1
        }
    }

    // ─── Inline parsing ─────────────────────────────────────

    private fun applyInline(editable: Editable, start: Int, end: Int) {
        if (start >= end) return
        val text = editable.subSequence(start, end)
        var i = 0
        while (i < text.length) {
            when {
                // Link [text](url)
                text[i] == '[' -> {
                    val closeB = text.indexOf(']', i + 1)
                    if (closeB > i && closeB + 1 < text.length && text[closeB + 1] == '(') {
                        val closeP = text.indexOf(')', closeB + 2)
                        if (closeP > closeB) {
                            // Dim [
                            editable.setSpan(MarkdownSyntaxSpan(), start + i, start + i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            // Style link text
                            val linkTextStart = start + i + 1
                            val linkTextEnd = start + closeB
                            editable.setSpan(ForegroundColorSpan(Color.parseColor(LINK_COLOR)), linkTextStart, linkTextEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            editable.setSpan(UnderlineSpan(), linkTextStart, linkTextEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            // Hide ](url)
                            editable.setSpan(MarkdownLinkUrlSpan(), start + closeB, start + closeP + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            i = closeP + 1
                            continue
                        }
                    }
                    i++
                }
                // Bold **text**
                text.startsWith("**", i) -> {
                    val close = text.indexOf("**", i + 2)
                    if (close > i) {
                        editable.setSpan(MarkdownSyntaxSpan(), start + i, start + i + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(StyleSpan(Typeface.BOLD), start + i + 2, start + close, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(MarkdownSyntaxSpan(), start + close, start + close + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = close + 2
                        continue
                    }
                    i++
                }
                // Bold __text__
                text.startsWith("__", i) -> {
                    val close = text.indexOf("__", i + 2)
                    if (close > i) {
                        editable.setSpan(MarkdownSyntaxSpan(), start + i, start + i + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(StyleSpan(Typeface.BOLD), start + i + 2, start + close, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(MarkdownSyntaxSpan(), start + close, start + close + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = close + 2
                        continue
                    }
                    i++
                }
                // Strikethrough ~~text~~
                text.startsWith("~~", i) -> {
                    val close = text.indexOf("~~", i + 2)
                    if (close > i) {
                        editable.setSpan(MarkdownSyntaxSpan(), start + i, start + i + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(StrikethroughSpan(), start + i + 2, start + close, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(MarkdownSyntaxSpan(), start + close, start + close + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = close + 2
                        continue
                    }
                    i++
                }
                // Single ~text~
                text[i] == '~' -> {
                    val close = text.indexOf('~', i + 1)
                    if (close > i + 1) {
                        editable.setSpan(MarkdownSyntaxSpan(), start + i, start + i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(StrikethroughSpan(), start + i + 1, start + close, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(MarkdownSyntaxSpan(), start + close, start + close + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = close + 1
                        continue
                    }
                    i++
                }
                // Italic *text* / _text_
                text[i] == '*' || text[i] == '_' -> {
                    val delim = text[i]
                    val isDouble = i + 1 < text.length && text[i + 1] == delim
                    if (!isDouble) {
                        val close = text.indexOf(delim, i + 1)
                        if (close > i + 1) {
                            editable.setSpan(MarkdownSyntaxSpan(), start + i, start + i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            editable.setSpan(StyleSpan(Typeface.ITALIC), start + i + 1, start + close, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            editable.setSpan(MarkdownSyntaxSpan(), start + close, start + close + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            i = close + 1
                            continue
                        }
                    }
                    i++
                }
                // Inline code `code`
                text[i] == '`' -> {
                    val close = text.indexOf('`', i + 1)
                    if (close > i) {
                        editable.setSpan(MarkdownSyntaxSpan(), start + i, start + i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(MarkdownInlineCodeSpan(), start + i + 1, start + close, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(BackgroundColorSpan(CODE_BG), start + i + 1, start + close, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(MarkdownSyntaxSpan(), start + close, start + close + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = close + 1
                        continue
                    }
                    i++
                }
                else -> i++
            }
        }
    }

    private fun isQuote(line: String): Boolean {
        val idx = line.indexOf('>')
        return idx >= 0 && line.take(idx).isBlank()
    }

    private fun String.contentStartAfterTaskMarker(): Int {
        val closeBracket = indexOf(']')
        if (closeBracket < 0) return length
        var index = closeBracket + 1
        while (index < length && this[index] == ' ') index++
        return index
    }
}

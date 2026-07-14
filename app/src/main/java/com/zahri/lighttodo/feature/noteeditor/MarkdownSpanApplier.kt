package com.zahri.lighttodo.feature.noteeditor

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import com.zahri.lighttodo.domain.markdown.MarkdownEditorPolicy
import com.zahri.lighttodo.domain.markdown.MarkdownTextTransforms
import com.zahri.lighttodo.domain.note.NoteAttachmentMarkdown

/**
 * Applies Markdown formatting spans to an Editable buffer.
 * Called from MarkdownEditText's TextWatcher on every text change.
 *
 * Design: syntax characters remain visible but dimmed (MarkdownSyntaxSpan).
 * Content gets styled with Android spans for native rendering quality.
 */
object MarkdownSpanApplier {

    // ─── Regex (same patterns as old MarkdownParser) ────────
    private val HeadingRegex = Regex("^(\\s*)(#{1,6})\\s+(.+)$")
    private val HorizontalRuleRegex = Regex("^\\s{0,3}([-*_])(?:\\s*\\1){2,}\\s*$")
    private val OrderedListRegex = Regex("^(\\s*)(\\d+)[.)]\\s+(.+)$")
    private val UnorderedListRegex = Regex("^(\\s*)[-*+]\\s+(.+)$")
    private val TaskListRegex = Regex("^(\\s*)[-*+]\\s+\\[([ xX]?)]\\s*(.*)$")
    private val InlineCodeRegex = Regex("`([^`]+)`")
    private val InlineBoldAsteriskRegex = Regex("\\*\\*([^*]+)\\*\\*")
    private val InlineBoldUnderscoreRegex = Regex(
        "(?<![\\p{L}\\p{N}_])__([^_\\n]+)__(?![\\p{L}\\p{N}_])"
    )
    private val InlineDoubleStrikeRegex = Regex("~~([^~]+)~~")
    private val InlineSingleStrikeRegex = Regex("~([^~]+)~")
    private val InlineUnderlineRegex = Regex("<u>(.*?)</u>")
    private val InlineItalicAsteriskRegex = Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)")
    private val InlineItalicUnderscoreRegex = Regex(
        "(?<![\\p{L}\\p{N}_])_([^_\\n]+)_(?![\\p{L}\\p{N}_])"
    )

    data class LinkRange(
        val textStart: Int,
        val textEnd: Int,
        val suffixStart: Int,
        val suffixEnd: Int,
        val url: String
    )

    /**
     * Strip markdown syntax for preview display.
     * Returns plain text suitable for a Text composable.
     */
    fun stripMarkdown(markdown: String): String = renderedText(
        markdown = markdown,
        includeAttachmentLabels = true
    )

    fun visibleCharacterCount(title: String, markdown: String): Int =
        title.length + renderedText(markdown, includeAttachmentLabels = false).length

    private fun renderedText(markdown: String, includeAttachmentLabels: Boolean): String {
        var inCodeBlock = false
        return markdown.lineSequence().mapNotNull { line ->
            val trimmed = line.trimStart()
            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                return@mapNotNull null
            }
            if (inCodeBlock) return@mapNotNull line
            NoteAttachmentMarkdown.parseLine(line)?.let { attachment ->
                return@mapNotNull if (includeAttachmentLabels) {
                    NoteAttachmentMarkdown.previewLabel(attachment)
                } else {
                    null
                }
            }
            when {
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
        return stripMarkdownLinks(text)
            .replace(InlineCodeRegex, "$1")
            .replace(InlineBoldAsteriskRegex, "$1")
            .replace(InlineBoldUnderscoreRegex, "$1")
            .replace(InlineDoubleStrikeRegex, "$1")
            .replace(InlineSingleStrikeRegex, "$1")
            .replace(InlineUnderlineRegex, "$1")
            .replace(InlineItalicAsteriskRegex, "$1")
            .replace(InlineItalicUnderscoreRegex, "$1")
    }

    private fun stripMarkdownLinks(text: String): String {
        val ranges = findMarkdownLinkRanges(text)
        if (ranges.isEmpty()) return text
        return buildString(text.length) {
            var copiedUntil = 0
            ranges.forEach { range ->
                val linkStart = range.textStart - 1
                if (linkStart < copiedUntil) return@forEach
                append(text, copiedUntil, linkStart)
                append(text, range.textStart, range.textEnd)
                copiedUntil = range.suffixEnd
            }
            append(text, copiedUntil, text.length)
        }
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

    private data class LineRange(
        val start: Int,
        val endExclusive: Int
    )

    fun apply(
        editable: Editable,
        activeOffset: Int? = null,
        context: Context? = null,
        renderStyle: MarkdownRenderStyle = MarkdownRenderStyle.forDarkMode(false),
        resolveAttachment: NoteAttachmentResolver? = null
    ) {
        removeAllManagedSpans(editable)

        var inCodeBlock = false
        var lineStart = 0
        while (lineStart <= editable.length) {
            val lineEnd = editable.indexOf('\n', lineStart).let {
                if (it == -1) editable.length else it
            }
            val range = LineRange(lineStart, lineEnd)
            val lineIsActive = activeOffset != null && activeOffset in lineStart..lineEnd
            inCodeBlock = applyLine(
                editable = editable,
                range = range,
                inCodeBlock = inCodeBlock,
                lineIsActive = lineIsActive,
                context = context,
                renderStyle = renderStyle,
                resolveAttachment = resolveAttachment
            )
            lineStart = lineEnd + 1
        }
    }

    fun applyChangedLine(
        editable: Editable,
        changedOffset: Int,
        activeOffset: Int? = null,
        context: Context? = null,
        renderStyle: MarkdownRenderStyle = MarkdownRenderStyle.forDarkMode(false),
        resolveAttachment: NoteAttachmentResolver? = null
    ) {
        val range = lineRangeAt(editable, changedOffset)
        removeManagedSpansInRange(editable, range)
        val activeLineStart = activeOffset?.let { lineRangeAt(editable, it).start }
        applyLine(
            editable = editable,
            range = range,
            inCodeBlock = codeBlockStateBefore(editable, range.start),
            lineIsActive = activeLineStart == range.start,
            context = context,
            renderStyle = renderStyle,
            resolveAttachment = resolveAttachment
        )
    }

    fun applyActiveLineChange(
        editable: Editable,
        previousActiveOffset: Int?,
        activeOffset: Int?,
        context: Context? = null,
        renderStyle: MarkdownRenderStyle = MarkdownRenderStyle.forDarkMode(false),
        resolveAttachment: NoteAttachmentResolver? = null
    ) {
        val previousRange = previousActiveOffset?.let { lineRangeAt(editable, it) }
        val activeRange = activeOffset?.let { lineRangeAt(editable, it) }
        if (previousRange?.start == activeRange?.start) return

        listOfNotNull(previousRange, activeRange)
            .distinctBy(LineRange::start)
            .forEach { removeManagedSpansInRange(editable, it) }

        previousRange?.let { range ->
            applyLine(
                editable = editable,
                range = range,
                inCodeBlock = codeBlockStateBefore(editable, range.start),
                lineIsActive = false,
                context = context,
                renderStyle = renderStyle,
                resolveAttachment = resolveAttachment
            )
        }
        activeRange?.let { range ->
            applyLine(
                editable = editable,
                range = range,
                inCodeBlock = codeBlockStateBefore(editable, range.start),
                lineIsActive = true,
                context = context,
                renderStyle = renderStyle,
                resolveAttachment = resolveAttachment
            )
        }
    }

    private fun removeAllManagedSpans(editable: Editable) {
        for (type in MANAGED_SPANS) {
            val spans = editable.getSpans(0, editable.length, type)
            for (span in spans) {
                editable.removeSpan(span)
            }
        }
    }

    private fun removeManagedSpansInRange(editable: Editable, range: LineRange) {
        for (type in MANAGED_SPANS) {
            val candidates = linkedSetOf<Any>()
            editable.getSpans(range.start, range.endExclusive, type).forEach {
                candidates += it
            }
            editable.getSpans(range.start, range.start, type).forEach {
                candidates += it
            }
            editable.getSpans(range.endExclusive, range.endExclusive, type).forEach {
                candidates += it
            }
            for (span in candidates) {
                val spanStart = editable.getSpanStart(span)
                val spanEnd = editable.getSpanEnd(span)
                if (MarkdownEditorPolicy.shouldRemoveSpanOnLineRefresh(
                        spanStart = spanStart,
                        spanEnd = spanEnd,
                        lineStart = range.start,
                        lineEndExclusive = range.endExclusive
                    )
                ) {
                    editable.removeSpan(span)
                }
            }
        }
    }

    private fun applyLine(
        editable: Editable,
        range: LineRange,
        inCodeBlock: Boolean,
        lineIsActive: Boolean,
        context: Context?,
        renderStyle: MarkdownRenderStyle,
        resolveAttachment: NoteAttachmentResolver?
    ): Boolean {
        val lineStart = range.start
        val lineEnd = range.endExclusive
        val line = editable.subSequence(lineStart, lineEnd).toString()
        val trimmed = line.trimStart()

        if (trimmed.startsWith("```")) {
            if (!lineIsActive) {
                editable.setSpan(MarkdownSyntaxSpan(), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                editable.setSpan(
                    MarkdownCodeBlockSpan(renderStyle.codeTextColor),
                    lineStart,
                    lineEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return !inCodeBlock
        }

        if (inCodeBlock) {
            if (!lineIsActive) {
                editable.setSpan(
                    MarkdownCodeBlockSpan(renderStyle.codeTextColor),
                    lineStart,
                    lineEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                editable.setSpan(
                    BackgroundColorSpan(renderStyle.codeBackgroundColor),
                    lineStart,
                    lineEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return true
        }

        if (context != null) {
            val attachment = NoteAttachmentMarkdown.parseLine(line)
            if (attachment != null) {
                val span = when (attachment.kind) {
                    NoteAttachmentMarkdown.Kind.Image -> MarkdownImageSpan(
                        context = context,
                        attachment = attachment,
                        resolveAttachment = resolveAttachment ?: { _: String -> null },
                        placeholderColor = renderStyle.imagePlaceholderColor,
                        placeholderTextColor = renderStyle.imagePlaceholderTextColor
                    )
                    NoteAttachmentMarkdown.Kind.Audio -> MarkdownAudioSpan(
                        context = context,
                        attachment = attachment,
                        backgroundColor = renderStyle.audioBackgroundColor,
                        accentColor = renderStyle.audioAccentColor,
                        waveColor = renderStyle.audioWaveColor,
                        textColor = renderStyle.audioTextColor
                    )
                }
                editable.setSpan(span, lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                return false
            }
        }

        if (lineIsActive) {
            applyActiveLine(
                editable = editable,
                lineStart = lineStart,
                lineEnd = lineEnd,
                renderStyle = renderStyle
            )
            return false
        }

        when {
            HeadingRegex.matchEntire(line) != null -> {
                val match = HeadingRegex.matchEntire(line)!!
                val indent = match.groupValues[1].length
                val marker = match.groupValues[2]
                val level = marker.length
                val markerStart = lineStart + indent
                val markerEnd = markerStart + marker.length + 1
                val contentStart = markerEnd
                val contentEnd = lineEnd

                if (markerEnd <= lineEnd) {
                    editable.setSpan(MarkdownSyntaxSpan(), markerStart, markerEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                editable.setSpan(MarkdownHeadingSpan(level), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                editable.setSpan(StyleSpan(Typeface.BOLD), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            HorizontalRuleRegex.matchEntire(line) != null -> {
                editable.setSpan(MarkdownSyntaxSpan(), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                editable.setSpan(
                    MarkdownHrSpan(renderStyle.dividerColor),
                    lineStart,
                    lineEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
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

                editable.setSpan(MarkdownSyntaxSpan(), markerStart, contentStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                editable.setSpan(
                    MarkdownCheckboxSpan(
                        checked = checked,
                        fillColor = renderStyle.checkboxFillColor,
                        outlineColor = renderStyle.checkboxOutlineColor,
                        markColor = renderStyle.checkboxMarkColor
                    ),
                    markerStart,
                    contentEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (checked) {
                    editable.setSpan(MarkdownCheckedAlphaSpan(), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    editable.setSpan(StrikethroughSpan(), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                applyInline(editable, contentStart, contentEnd, renderStyle)
            }

            OrderedListRegex.matchEntire(line) != null -> {
                val match = OrderedListRegex.matchEntire(line)!!
                val indent = match.groupValues[1].length
                val markerStart = lineStart + indent
                val contentStart = line.indexOf(')').let {
                    if (it == -1) line.indexOf('.') + 1 else it + 1
                } + 1 + lineStart

                editable.setSpan(MarkdownOrderedListSpan(), markerStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                applyInline(editable, contentStart, lineEnd, renderStyle)
            }

            UnorderedListRegex.matchEntire(line) != null -> {
                val match = UnorderedListRegex.matchEntire(line)!!
                val indent = match.groupValues[1].length
                val markerStart = lineStart + indent
                val contentStart = markerStart + 2

                editable.setSpan(MarkdownSyntaxSpan(), markerStart, contentStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                editable.setSpan(
                    MarkdownBulletSpan(renderStyle.bulletColor),
                    markerStart,
                    lineEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                applyInline(editable, contentStart, lineEnd, renderStyle)
            }

            isQuote(line) -> {
                val quotePos = line.indexOf('>')
                val contentStartInLine = (quotePos + 1).let { index ->
                    var result = index
                    while (result < line.length && line[result] == ' ') result++
                    result
                }
                val contentStart = lineStart + contentStartInLine

                editable.setSpan(MarkdownSyntaxSpan(), lineStart, contentStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                editable.setSpan(
                    MarkdownQuoteSpan(barColor = renderStyle.quoteBarColor),
                    lineStart,
                    lineEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                editable.setSpan(
                    ForegroundColorSpan(renderStyle.quoteTextColor),
                    contentStart,
                    lineEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                applyInline(editable, contentStart, lineEnd, renderStyle)
            }

            else -> applyInline(editable, lineStart, lineEnd, renderStyle)
        }
        return false
    }

    private fun applyActiveLine(
        editable: Editable,
        lineStart: Int,
        lineEnd: Int,
        renderStyle: MarkdownRenderStyle
    ) {
        val line = editable.subSequence(lineStart, lineEnd).toString()
        val heading = HeadingRegex.matchEntire(line)
        if (heading != null) {
            val contentStart = lineStart + heading.groupValues[1].length +
                heading.groupValues[2].length + 1
            val level = heading.groupValues[2].length
            editable.setSpan(
                MarkdownHeadingSpan(level),
                contentStart,
                lineEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            editable.setSpan(
                StyleSpan(Typeface.BOLD),
                contentStart,
                lineEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        applyActiveLineLinks(editable, lineStart, lineEnd, renderStyle)
    }

    private fun lineRangeAt(text: CharSequence, offset: Int): LineRange {
        val safeOffset = offset.coerceIn(0, text.length)
        val start = if (safeOffset == 0) {
            0
        } else {
            text.lastIndexOf('\n', safeOffset - 1).let { if (it == -1) 0 else it + 1 }
        }
        val end = text.indexOf('\n', safeOffset).let { if (it == -1) text.length else it }
        return LineRange(start, end)
    }

    private fun codeBlockStateBefore(text: CharSequence, targetLineStart: Int): Boolean {
        var inCodeBlock = false
        var lineStart = 0
        while (lineStart < targetLineStart) {
            val lineEnd = text.indexOf('\n', lineStart).let {
                if (it == -1) text.length else it
            }
            if (isCodeFenceLine(text, lineStart, lineEnd)) inCodeBlock = !inCodeBlock
            if (lineEnd == text.length) break
            lineStart = lineEnd + 1
        }
        return inCodeBlock
    }

    private fun isCodeFenceLine(text: CharSequence, start: Int, endExclusive: Int): Boolean {
        var index = start
        while (index < endExclusive && text[index].isWhitespace()) index++
        return index + 2 < endExclusive &&
            text[index] == '`' && text[index + 1] == '`' && text[index + 2] == '`'
    }

    // ─── Inline parsing ─────────────────────────────────────

    fun findMarkdownLinkRanges(line: String): List<LinkRange> {
        val ranges = mutableListOf<LinkRange>()
        var i = 0
        while (i < line.length) {
            if (line[i] == '[') {
                val closeB = line.indexOf(']', i + 1)
                if (closeB > i && closeB + 1 < line.length && line[closeB + 1] == '(') {
                    val openP = closeB + 1
                    val closeP = findMatchingCloseParenthesis(line, openP)
                    if (closeP != null) {
                        ranges += LinkRange(
                            textStart = i + 1,
                            textEnd = closeB,
                            suffixStart = closeB,
                            suffixEnd = closeP + 1,
                            url = line.substring(closeB + 2, closeP)
                        )
                        i = closeP + 1
                        continue
                    }
                }
            }
            i++
        }
        return ranges
    }

    private fun findMatchingCloseParenthesis(text: CharSequence, openIndex: Int): Int? {
        var depth = 0
        var escaped = false
        for (index in openIndex until text.length) {
            val char = text[index]
            if (escaped) {
                escaped = false
                continue
            }
            if (char == '\\') {
                escaped = true
                continue
            }
            when (char) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return index
                    if (depth < 0) return null
                }
            }
        }
        return null
    }

    private fun applyInline(
        editable: Editable,
        start: Int,
        end: Int,
        renderStyle: MarkdownRenderStyle
    ) {
        if (start >= end) return
        val text = editable.subSequence(start, end)
        val linkRanges = findMarkdownLinkRanges(text.toString()).associateBy { it.textStart - 1 }
        val boldAsteriskRanges = MarkdownTextTransforms.findInlineStyleRanges(
            text = text,
            openMarker = "**",
            closeMarker = "**"
        ).associateBy { it.openStart }
        val boldUnderscoreRanges = MarkdownTextTransforms.findInlineStyleRanges(
            text = text,
            openMarker = "__",
            closeMarker = "__"
        ).associateBy { it.openStart }
        val doubleStrikeRanges = MarkdownTextTransforms.findInlineStyleRanges(
            text = text,
            openMarker = "~~",
            closeMarker = "~~"
        ).associateBy { it.openStart }
        val singleStrikeRanges = MarkdownTextTransforms.findInlineStyleRanges(
            text = text,
            openMarker = "~",
            closeMarker = "~"
        ).associateBy { it.openStart }
        val italicAsteriskRanges = MarkdownTextTransforms.findInlineStyleRanges(
            text = text,
            openMarker = "*",
            closeMarker = "*"
        ).associateBy { it.openStart }
        val italicUnderscoreRanges = MarkdownTextTransforms.findInlineStyleRanges(
            text = text,
            openMarker = "_",
            closeMarker = "_"
        ).associateBy { it.openStart }
        val underlineRanges = MarkdownTextTransforms.findInlineStyleRanges(
            text = text,
            openMarker = "<u>",
            closeMarker = "</u>"
        ).associateBy { it.openStart }
        val inlineCodeRanges = MarkdownTextTransforms.findInlineStyleRanges(
            text = text,
            openMarker = "`",
            closeMarker = "`"
        ).associateBy { it.openStart }
        var i = 0
        while (i < text.length) {
            when {
                // Link [text](url)
                text[i] == '[' -> {
                    val range = linkRanges[i]
                    if (range != null) {
                        applyLinkRange(
                            editable = editable,
                            lineStart = start,
                            range = range,
                            hideSyntax = true,
                            renderStyle = renderStyle
                        )
                        i = range.suffixEnd
                        continue
                    }
                    i++
                }
                // Bold **text**
                text.startsWith("**", i) -> {
                    val range = boldAsteriskRanges[i]
                    if (range != null) {
                        editable.setSpan(MarkdownSyntaxSpan(), start + range.openStart, start + range.contentStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(StyleSpan(Typeface.BOLD), start + range.contentStart, start + range.contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(MarkdownSyntaxSpan(), start + range.contentEnd, start + range.closeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = range.closeEnd
                        continue
                    }
                    i++
                }
                // Bold __text__
                text.startsWith("__", i) -> {
                    val range = boldUnderscoreRanges[i]
                    if (range != null) {
                        editable.setSpan(MarkdownSyntaxSpan(), start + range.openStart, start + range.contentStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(StyleSpan(Typeface.BOLD), start + range.contentStart, start + range.contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(MarkdownSyntaxSpan(), start + range.contentEnd, start + range.closeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = range.closeEnd
                        continue
                    }
                    i++
                }
                // Strikethrough ~~text~~
                text.startsWith("~~", i) -> {
                    val range = doubleStrikeRanges[i]
                    if (range != null) {
                        editable.setSpan(MarkdownSyntaxSpan(), start + range.openStart, start + range.contentStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(StrikethroughSpan(), start + range.contentStart, start + range.contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(MarkdownSyntaxSpan(), start + range.contentEnd, start + range.closeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = range.closeEnd
                        continue
                    }
                    i++
                }
                // Single ~text~
                text[i] == '~' -> {
                    val range = singleStrikeRanges[i]
                    if (range != null) {
                        editable.setSpan(MarkdownSyntaxSpan(), start + range.openStart, start + range.contentStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(StrikethroughSpan(), start + range.contentStart, start + range.contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(MarkdownSyntaxSpan(), start + range.contentEnd, start + range.closeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = range.closeEnd
                        continue
                    }
                    i++
                }
                // Italic *text* / _text_
                text[i] == '*' || text[i] == '_' -> {
                    val delim = text[i]
                    val isDouble = i + 1 < text.length && text[i + 1] == delim
                    if (!isDouble) {
                        val range = if (delim == '*') {
                            italicAsteriskRanges[i]
                        } else {
                            italicUnderscoreRanges[i]
                        }
                        if (range != null) {
                            editable.setSpan(MarkdownSyntaxSpan(), start + range.openStart, start + range.contentStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            editable.setSpan(StyleSpan(Typeface.ITALIC), start + range.contentStart, start + range.contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            editable.setSpan(MarkdownSyntaxSpan(), start + range.contentEnd, start + range.closeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            i = range.closeEnd
                            continue
                        }
                    }
                    i++
                }
                // Underline <u>text</u>
                text.startsWith("<u>", i) -> {
                    val range = underlineRanges[i]
                    if (range != null) {
                        editable.setSpan(MarkdownSyntaxSpan(), start + range.openStart, start + range.contentStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(UnderlineSpan(), start + range.contentStart, start + range.contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(MarkdownSyntaxSpan(), start + range.contentEnd, start + range.closeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = range.closeEnd
                        continue
                    }
                    i++
                }
                // Inline code `code`
                text[i] == '`' -> {
                    val range = inlineCodeRanges[i]
                    if (range != null) {
                        editable.setSpan(MarkdownSyntaxSpan(), start + range.openStart, start + range.contentStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        editable.setSpan(
                            MarkdownInlineCodeSpan(renderStyle.codeTextColor),
                            start + range.contentStart,
                            start + range.contentEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        editable.setSpan(
                            BackgroundColorSpan(renderStyle.codeBackgroundColor),
                            start + range.contentStart,
                            start + range.contentEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        editable.setSpan(MarkdownSyntaxSpan(), start + range.contentEnd, start + range.closeEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = range.closeEnd
                        continue
                    }
                    i++
                }
                else -> i++
            }
        }
    }

    private fun applyActiveLineLinks(
        editable: Editable,
        start: Int,
        end: Int,
        renderStyle: MarkdownRenderStyle
    ) {
        if (start >= end) return
        val line = editable.subSequence(start, end).toString()
        findMarkdownLinkRanges(line).forEach { range ->
            applyLinkRange(
                editable = editable,
                lineStart = start,
                range = range,
                hideSyntax = false,
                renderStyle = renderStyle
            )
        }
    }

    private fun applyLinkRange(
        editable: Editable,
        lineStart: Int,
        range: LinkRange,
        hideSyntax: Boolean,
        renderStyle: MarkdownRenderStyle
    ) {
        if (hideSyntax) {
            editable.setSpan(
                MarkdownSyntaxSpan(),
                lineStart + range.textStart - 1,
                lineStart + range.textStart,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            editable.setSpan(
                MarkdownLinkUrlSpan(),
                lineStart + range.suffixStart,
                lineStart + range.suffixEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        editable.setSpan(
            MarkdownLinkSpan(range.url, renderStyle.linkColor),
            lineStart + range.textStart,
            lineStart + range.textEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
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

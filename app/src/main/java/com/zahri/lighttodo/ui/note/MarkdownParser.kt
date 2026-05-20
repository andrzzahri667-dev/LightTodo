package com.zahri.lighttodo.ui.note

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 将 Markdown 原文渲染为 AnnotatedString，用于所见即所得显示。
 *
 * 支持语法：
 * - 标题 # 到 ######
 * - 加粗 **text**
 * - 斜体 *text*
 * - 删除线 ~~text~~
 * - 行内代码 `code`
 * - 链接 [text](url)
 * - 无序列表 - item / * item / + item
 * - 有序列表 1. item / 1) item
 * - 任务列表 - [ ] item / - [x] item
 * - 引用 > text
 */
object MarkdownParser {

    private val LINK_TAG = "URL"
    private val HeadingRegex = Regex("^(\\s*)(#{1,6})\\s+(.+)$")
    private val HorizontalRuleRegex = Regex("^\\s{0,3}([-*_])(?:\\s*\\1){2,}\\s*$")
    private val OrderedListRegex = Regex("^\\s*(\\d+)[.)]\\s+(.+)$")
    private val UnorderedListRegex = Regex("^\\s*[-*+]\\s+(.+)$")
    private val TaskListRegex = Regex("^\\s*[-*+]\\s+\\[([ xX])]\\s+(.+)$")

    data class RenderedMarkdown(
        val text: AnnotatedString,
        val originalToTransformed: IntArray,
        val transformedToOriginal: IntArray
    )

    /** 将 Markdown 文本渲染为带格式的 AnnotatedString */
    fun render(markdown: String): AnnotatedString = renderForEditing(markdown).text

    /** 用于编辑态的实时渲染，保留光标 offset 映射。 */
    fun renderForEditing(markdown: String): RenderedMarkdown = MarkdownRenderer(markdown).render()

    private fun AnnotatedString.Builder.renderLine(line: String) {
        val trimmed = line.trimStart()

        HeadingRegex.matchEntire(trimmed)?.let { match ->
            val level = match.groupValues[2].length
            withStyle(ParagraphStyle(lineHeight = headingLineHeight(level))) {
                withStyle(headingStyle(level)) {
                    renderInline(match.groupValues[3])
                }
            }
            return
        }

        if (HorizontalRuleRegex.matches(line)) {
            withStyle(SpanStyle(color = Color(0x668E8E93))) {
                append("────────")
            }
            return
        }

        if (trimmed.startsWith(">")) {
            withStyle(SpanStyle(color = Color(0xFF6E6E73), fontStyle = FontStyle.Italic)) {
                append("┃ ")
                renderInline(trimmed.removePrefix(">").trimStart())
            }
            return
        }

        TaskListRegex.matchEntire(line)?.let { match ->
            append(if (match.groupValues[1].equals("x", ignoreCase = true)) "☑ " else "☐ ")
            renderInline(match.groupValues[2])
            return
        }

        OrderedListRegex.matchEntire(line)?.let { match ->
            append("${match.groupValues[1]}. ")
            renderInline(match.groupValues[2])
            return
        }

        UnorderedListRegex.matchEntire(line)?.let { match ->
            append("• ")
            renderInline(match.groupValues[1])
            return
        }

        renderInline(line)
    }

    private fun AnnotatedString.Builder.renderInline(text: String) {
        var i = 0
        while (i < text.length) {
            when {
                // 链接 [text](url)
                text[i] == '[' -> {
                    val closeB = text.indexOf(']', i + 1)
                    if (closeB > i && closeB + 1 < text.length && text[closeB + 1] == '(') {
                        val closeP = text.indexOf(')', closeB + 2)
                        if (closeP > closeB) {
                            val linkText = text.substring(i + 1, closeB)
                            val url = text.substring(closeB + 2, closeP)
                            pushStringAnnotation(tag = LINK_TAG, annotation = url)
                            withStyle(SpanStyle(color = Color(0xFF0A84FF), textDecoration = TextDecoration.Underline)) {
                                renderInline(linkText)
                            }
                            pop()
                            i = closeP + 1
                            continue
                        }
                    }
                    append(text[i])
                    i++
                }
                // 加粗 **text**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            renderInline(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 加粗 __text__
                text.startsWith("__", i) -> {
                    val end = text.indexOf("__", i + 2)
                    if (end > i) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            renderInline(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 删除线 ~~text~~
                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end > i) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            renderInline(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 兼容单波浪线 ~text~
                text[i] == '~' -> {
                    val end = text.indexOf('~', i + 1)
                    if (end > i + 1) {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            renderInline(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 斜体 *text* / _text_，避开 ** 和 __
                text[i] == '*' || text[i] == '_' -> {
                    val delimiter = text[i]
                    val isDouble = i + 1 < text.length && text[i + 1] == delimiter
                    if (isDouble) {
                        append(text[i])
                        i++
                    } else {
                        val end = text.indexOf(delimiter, i + 1)
                        if (end > i + 1) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                renderInline(text.substring(i + 1, end))
                            }
                            i = end + 1
                        } else {
                            append(text[i])
                            i++
                        }
                    }
                }
                // 行内代码 `code`
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i) {
                        withStyle(SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            background = Color(0x1A8E8E93)
                        )) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }

    private fun headingStyle(level: Int): SpanStyle {
        return SpanStyle(
            fontSize = when (level) {
                1 -> 28.sp
                2 -> 24.sp
                3 -> 21.sp
                4 -> 19.sp
                5 -> 18.sp
                else -> 17.sp
            },
            fontWeight = FontWeight.Bold
        )
    }

    private fun headingLineHeight(level: Int): TextUnit {
        return when (level) {
            1 -> 36.sp
            2 -> 32.sp
            3 -> 28.sp
            else -> 26.sp
        }
    }

    /** 从 AnnotatedString 中提取链接 URL（供点击使用） */
    fun getUrlAt(annotatedString: AnnotatedString, offset: Int): String? {
        return annotatedString.getStringAnnotations(tag = LINK_TAG, start = offset, end = offset)
            .firstOrNull()?.item
    }

    private class MarkdownRenderer(private val source: String) {
        private val builder = AnnotatedString.Builder()
        private val originalToTransformed = IntArray(source.length + 1) { -1 }
        private val transformedToOriginal = mutableListOf(0)
        private var transformedLength = 0
        private var inCodeBlock = false

        fun render(): RenderedMarkdown {
            var lineStart = 0
            while (lineStart <= source.length) {
                val lineEnd = source.indexOf('\n', lineStart).let { if (it == -1) source.length else it }
                renderLine(lineStart, lineEnd)
                if (lineEnd == source.length) break
                appendOriginalChar(lineEnd)
                lineStart = lineEnd + 1
            }

            setOriginalOffset(source.length, transformedLength)
            fillMissingOriginalOffsets()
            if (transformedToOriginal.size == transformedLength) {
                transformedToOriginal.add(source.length)
            } else {
                transformedToOriginal[transformedLength] = source.length
            }
            return RenderedMarkdown(
                text = builder.toAnnotatedString(),
                originalToTransformed = originalToTransformed,
                transformedToOriginal = transformedToOriginal.toIntArray()
            )
        }

        private fun renderLine(lineStart: Int, lineEnd: Int) {
            val line = source.substring(lineStart, lineEnd)
            val trimmed = line.trimStart()

            if (trimmed.startsWith("```")) {
                hideRange(lineStart, lineEnd)
                inCodeBlock = !inCodeBlock
                return
            }

            if (inCodeBlock) {
                withSpanStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        background = Color(0x1A8E8E93)
                    )
                ) {
                    appendOriginalRange(lineStart, lineEnd)
                }
                return
            }

            HorizontalRuleRegex.matchEntire(line)?.let {
                withSpanStyle(SpanStyle(color = Color(0x668E8E93))) {
                    appendReplacement("────────", lineStart, lineEnd)
                }
                return
            }

            HeadingRegex.matchEntire(line)?.let { match ->
                val marker = match.groups[2]!!.range
                val content = match.groups[3]!!.range
                val level = marker.last - marker.first + 1
                hideRange(lineStart, lineStart + content.first)
                withParagraphStyle(ParagraphStyle(lineHeight = headingLineHeight(level))) {
                    withSpanStyle(headingStyle(level)) {
                        renderInline(lineStart + content.first, lineStart + content.last + 1)
                    }
                }
                return
            }

            TaskListRegex.matchEntire(line)?.let { match ->
                val checkbox = match.groups[1]!!
                val content = match.groups[2]!!.range
                appendReplacement(
                    if (checkbox.value.equals("x", ignoreCase = true)) "☑ " else "☐ ",
                    lineStart,
                    lineStart + content.first
                )
                renderInline(lineStart + content.first, lineStart + content.last + 1)
                return
            }

            OrderedListRegex.matchEntire(line)?.let { match ->
                val number = match.groups[1]!!.range
                val content = match.groups[2]!!.range
                hideRange(lineStart, lineStart + number.first)
                appendOriginalRange(lineStart + number.first, lineStart + content.first)
                renderInline(lineStart + content.first, lineStart + content.last + 1)
                return
            }

            UnorderedListRegex.matchEntire(line)?.let { match ->
                val content = match.groups[1]!!.range
                appendReplacement("• ", lineStart, lineStart + content.first)
                renderInline(lineStart + content.first, lineStart + content.last + 1)
                return
            }

            val quoteIndex = line.indexOf('>')
            if (quoteIndex >= 0 && line.take(quoteIndex).isBlank()) {
                val contentStartInLine = (quoteIndex + 1).let { index ->
                    var result = index
                    while (result < line.length && line[result] == ' ') result++
                    result
                }
                withSpanStyle(SpanStyle(color = Color(0xFF6E6E73), fontStyle = FontStyle.Italic)) {
                    appendReplacement("┃ ", lineStart, lineStart + contentStartInLine)
                    renderInline(lineStart + contentStartInLine, lineEnd)
                }
                return
            }

            renderInline(lineStart, lineEnd)
        }

        private fun renderInline(start: Int, end: Int) {
            var index = start
            while (index < end) {
                when {
                    source[index] == '[' -> {
                        val closeBracket = source.indexOf(']', index + 1).takeIf { it in (index + 1) until end }
                        val openParen = closeBracket?.plus(1)
                        if (closeBracket != null && openParen != null && openParen < end && source[openParen] == '(') {
                            val closeParen = source.indexOf(')', openParen + 1).takeIf { it in (openParen + 1)..end }
                            if (closeParen != null) {
                                val url = source.substring(openParen + 1, closeParen)
                                hideRange(index, index + 1)
                                builder.pushStringAnnotation(tag = LINK_TAG, annotation = url)
                                withSpanStyle(SpanStyle(color = Color(0xFF0A84FF), textDecoration = TextDecoration.Underline)) {
                                    renderInline(index + 1, closeBracket)
                                }
                                builder.pop()
                                hideRange(closeBracket, closeParen + 1)
                                index = closeParen + 1
                                continue
                            }
                        }
                        appendOriginalChar(index)
                        index++
                    }
                    source.startsWith("**", index) -> {
                        val close = source.indexOf("**", index + 2).takeIf { it in (index + 2) until end }
                        if (close != null) {
                            hideRange(index, index + 2)
                            withSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                renderInline(index + 2, close)
                            }
                            hideRange(close, close + 2)
                            index = close + 2
                        } else {
                            appendOriginalChar(index)
                            index++
                        }
                    }
                    source.startsWith("__", index) -> {
                        val close = source.indexOf("__", index + 2).takeIf { it in (index + 2) until end }
                        if (close != null) {
                            hideRange(index, index + 2)
                            withSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                renderInline(index + 2, close)
                            }
                            hideRange(close, close + 2)
                            index = close + 2
                        } else {
                            appendOriginalChar(index)
                            index++
                        }
                    }
                    source.startsWith("~~", index) -> {
                        val close = source.indexOf("~~", index + 2).takeIf { it in (index + 2) until end }
                        if (close != null) {
                            hideRange(index, index + 2)
                            withSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                renderInline(index + 2, close)
                            }
                            hideRange(close, close + 2)
                            index = close + 2
                        } else {
                            appendOriginalChar(index)
                            index++
                        }
                    }
                    source[index] == '~' -> {
                        val close = source.indexOf('~', index + 1).takeIf { it in (index + 2) until end }
                        if (close != null) {
                            hideRange(index, index + 1)
                            withSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                renderInline(index + 1, close)
                            }
                            hideRange(close, close + 1)
                            index = close + 1
                        } else {
                            appendOriginalChar(index)
                            index++
                        }
                    }
                    source[index] == '*' || source[index] == '_' -> {
                        val delimiter = source[index]
                        val isDouble = index + 1 < end && source[index + 1] == delimiter
                        val close = if (isDouble) -1 else source.indexOf(delimiter, index + 1)
                        if (!isDouble && close in (index + 2) until end) {
                            hideRange(index, index + 1)
                            withSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                renderInline(index + 1, close)
                            }
                            hideRange(close, close + 1)
                            index = close + 1
                        } else {
                            appendOriginalChar(index)
                            index++
                        }
                    }
                    source[index] == '`' -> {
                        val close = source.indexOf('`', index + 1).takeIf { it in (index + 1) until end }
                        if (close != null) {
                            hideRange(index, index + 1)
                            withSpanStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp,
                                    background = Color(0x1A8E8E93)
                                )
                            ) {
                                appendOriginalRange(index + 1, close)
                            }
                            hideRange(close, close + 1)
                            index = close + 1
                        } else {
                            appendOriginalChar(index)
                            index++
                        }
                    }
                    else -> {
                        appendOriginalChar(index)
                        index++
                    }
                }
            }
        }

        private fun appendOriginalRange(start: Int, end: Int) {
            for (index in start until end) {
                appendOriginalChar(index)
            }
        }

        private fun appendOriginalChar(index: Int) {
            setOriginalOffset(index, transformedLength)
            builder.append(source[index])
            transformedLength++
            transformedToOriginal.add(index + 1)
            setOriginalOffset(index + 1, transformedLength)
        }

        private fun appendReplacement(text: String, sourceStart: Int, sourceEnd: Int) {
            val safeSourceEnd = sourceEnd.coerceAtLeast(sourceStart + 1).coerceAtMost(source.length)
            val sourceLength = safeSourceEnd - sourceStart
            val startTransformed = transformedLength
            for (offset in sourceStart..sourceEnd.coerceAtMost(source.length)) {
                val progress = offset - sourceStart
                val mapped = startTransformed + (progress * text.length / sourceLength)
                setOriginalOffset(offset, mapped.coerceIn(startTransformed, startTransformed + text.length))
            }
            text.forEachIndexed { index, char ->
                builder.append(char)
                transformedLength++
                val originalOffset = sourceStart + ((index + 1) * sourceLength / text.length)
                transformedToOriginal.add(originalOffset.coerceIn(sourceStart, safeSourceEnd))
            }
        }

        private fun hideRange(start: Int, end: Int) {
            val safeStart = start.coerceIn(0, source.length)
            val safeEnd = end.coerceIn(safeStart, source.length)
            for (offset in safeStart..safeEnd) {
                setOriginalOffset(offset, transformedLength)
            }
        }

        private fun withSpanStyle(style: SpanStyle, block: () -> Unit) {
            builder.withStyle(style) { block() }
        }

        private fun withParagraphStyle(style: ParagraphStyle, block: () -> Unit) {
            builder.withStyle(style) { block() }
        }

        private fun setOriginalOffset(offset: Int, transformed: Int) {
            if (offset in originalToTransformed.indices) {
                originalToTransformed[offset] = transformed
            }
        }

        private fun fillMissingOriginalOffsets() {
            var last = 0
            for (index in originalToTransformed.indices) {
                if (originalToTransformed[index] < 0) {
                    originalToTransformed[index] = last
                } else {
                    last = originalToTransformed[index]
                }
            }
        }
    }
}

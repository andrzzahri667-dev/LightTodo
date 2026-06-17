package com.zahri.lighttodo.domain.markdown

object MarkdownTextTransforms {
    private val BareUrlRegex = Regex("^(https?://\\S+|www\\.\\S+)$")
    private val MarkdownLinkRegex = Regex("^\\[[^\\]]+]\\([^)]*\\)$")
    private val TaskListLineRegex = Regex("^(\\s*[-*+]\\s+\\[)([ xX]?)(]\\s*.*)$")
    private val OrderedListLineRegex = Regex("^(\\s*)(\\d+)([.)])(\\s+)(.*)$")
    private val UnorderedListLineRegex = Regex("^(\\s*)([-*+])(\\s+)(.*)$")
    private val TaskListContinuationRegex = Regex("^(\\s*)([-*+])(\\s+\\[)([ xX]?)(])(\\s*)(.*)$")

    data class Edit(
        val start: Int,
        val end: Int,
        val replacement: String,
        val cursorAfter: Int
    )

    fun markdownLinkForPastedText(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        if (MarkdownLinkRegex.matches(trimmed)) return null
        if (!BareUrlRegex.matches(trimmed)) return null
        val url = if (trimmed.startsWith("www.")) "https://$trimmed" else trimmed
        return "[$trimmed]($url)"
    }

    fun toggleTaskListLine(line: String): String? {
        val match = TaskListLineRegex.matchEntire(line) ?: return null
        val nextMarker = if (match.groupValues[2].equals("x", ignoreCase = true)) " " else "x"
        return match.groupValues[1] + nextMarker + match.groupValues[3]
    }

    fun toggleLinePrefix(text: String, cursor: Int, prefix: String): Edit? {
        val safeCursor = cursor.coerceIn(0, text.length)
        val (lineStart, lineEnd) = findLineRange(text, safeCursor)
        val line = text.substring(lineStart, lineEnd)
        val replacement = if (line.startsWith(prefix)) {
            line.removePrefix(prefix)
        } else {
            prefix + removeBlockPrefix(line)
        }
        return Edit(lineStart, lineEnd, replacement, lineStart + replacement.length)
    }

    fun toggleOrderedListPrefix(text: String, cursor: Int): Edit? {
        val safeCursor = cursor.coerceIn(0, text.length)
        val (lineStart, lineEnd) = findLineRange(text, safeCursor)
        val line = text.substring(lineStart, lineEnd)
        val replacement = if (OrderedListLineRegex.matchEntire(line) != null) {
            removeBlockPrefix(line)
        } else {
            "1. " + removeBlockPrefix(line)
        }
        return Edit(lineStart, lineEnd, replacement, lineStart + replacement.length)
    }

    fun toggleInlineStyleAtCursor(
        text: String,
        cursor: Int,
        openMarker: String,
        closeMarker: String
    ): Edit {
        val safeCursor = cursor.coerceIn(0, text.length)
        val (lineStart, lineEnd) = findLineRange(text, safeCursor)
        val line = text.substring(lineStart, lineEnd)
        val (contentStartInLine, contentEndInLine) = findInlineContentRange(line)

        if (contentStartInLine >= contentEndInLine) {
            return Edit(
                safeCursor,
                safeCursor,
                openMarker + closeMarker,
                safeCursor + openMarker.length
            )
        }

        val content = line.substring(contentStartInLine, contentEndInLine)
        val prefix = line.substring(0, contentStartInLine)
        val suffix = line.substring(contentEndInLine)
        val newContent = if (
            content.startsWith(openMarker) &&
            content.endsWith(closeMarker) &&
            content.length >= openMarker.length + closeMarker.length
        ) {
            content.substring(openMarker.length, content.length - closeMarker.length)
        } else {
            openMarker + content + closeMarker
        }
        val replacement = prefix + newContent + suffix
        return Edit(lineStart, lineEnd, replacement, lineStart + replacement.length)
    }

    fun listContinuationAfterNewline(text: String, newlineIndex: Int): Edit? {
        if (newlineIndex !in text.indices || text[newlineIndex] != '\n') return null

        val lineStart = text.lastIndexOf('\n', startIndex = newlineIndex - 1).let {
            if (it == -1) 0 else it + 1
        }
        val previousLine = text.substring(lineStart, newlineIndex)

        TaskListContinuationRegex.matchEntire(previousLine)?.let { match ->
            val content = match.groupValues[7]
            val markerEnd = lineStart +
                match.groupValues[1].length +
                match.groupValues[2].length +
                match.groupValues[3].length +
                match.groupValues[4].length +
                match.groupValues[5].length +
                match.groupValues[6].length
            if (content.isBlank()) {
                return Edit(lineStart, markerEnd, "", lineStart)
            }
            val prefix = match.groupValues[1] + match.groupValues[2] + " [ ] "
            return Edit(newlineIndex + 1, newlineIndex + 1, prefix, newlineIndex + 1 + prefix.length)
        }

        OrderedListLineRegex.matchEntire(previousLine)?.let { match ->
            val content = match.groupValues[5]
            val markerEnd = lineStart +
                match.groupValues[1].length +
                match.groupValues[2].length +
                match.groupValues[3].length +
                match.groupValues[4].length
            if (content.isBlank()) {
                return Edit(lineStart, markerEnd, "", lineStart)
            }
            val nextNumber = match.groupValues[2].toIntOrNull()?.plus(1) ?: return null
            val prefix = match.groupValues[1] + nextNumber + match.groupValues[3] + " "
            return Edit(newlineIndex + 1, newlineIndex + 1, prefix, newlineIndex + 1 + prefix.length)
        }

        UnorderedListLineRegex.matchEntire(previousLine)?.let { match ->
            val content = match.groupValues[4]
            val markerEnd = lineStart +
                match.groupValues[1].length +
                match.groupValues[2].length +
                match.groupValues[3].length
            if (content.isBlank()) {
                return Edit(lineStart, markerEnd, "", lineStart)
            }
            val prefix = match.groupValues[1] + match.groupValues[2] + " "
            return Edit(newlineIndex + 1, newlineIndex + 1, prefix, newlineIndex + 1 + prefix.length)
        }

        return null
    }

    private fun findLineRange(text: String, pos: Int): Pair<Int, Int> {
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

    private fun removeBlockPrefix(line: String): String {
        for (prefixRegex in listOf(
            Regex("^\\s*#{1,6}\\s+"),
            Regex("^\\s*[-*+]\\s+\\[[ xX]?]\\s*"),
            Regex("^\\s*[-*+]\\s+"),
            Regex("^\\s*>\\s+"),
            Regex("^\\s*\\d+[.)]\\s+")
        )) {
            val match = prefixRegex.find(line)
            if (match != null) return line.substring(match.range.last + 1)
        }
        return line
    }

    private fun findInlineContentRange(line: String): Pair<Int, Int> {
        val prefixEnd = inlinePrefixEnd(line)
        var contentEnd = line.length
        while (contentEnd > prefixEnd && line[contentEnd - 1].isWhitespace()) {
            contentEnd--
        }
        return prefixEnd to contentEnd
    }

    private fun inlinePrefixEnd(line: String): Int {
        for (prefixRegex in listOf(
            Regex("^\\s*#{1,6}\\s+"),
            Regex("^\\s*[-*+]\\s+\\[[ xX]?]\\s*"),
            Regex("^\\s*[-*+]\\s+"),
            Regex("^\\s*>\\s+"),
            Regex("^\\s*\\d+[.)]\\s+"),
            Regex("^\\s+")
        )) {
            val match = prefixRegex.find(line)
            if (match != null) return match.range.last + 1
        }
        return 0
    }
}

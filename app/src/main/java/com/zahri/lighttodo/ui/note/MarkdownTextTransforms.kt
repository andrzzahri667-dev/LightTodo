package com.zahri.lighttodo.ui.note

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
        return "[$trimmed]($trimmed)"
    }

    fun toggleTaskListLine(line: String): String? {
        val match = TaskListLineRegex.matchEntire(line) ?: return null
        val nextMarker = if (match.groupValues[2].equals("x", ignoreCase = true)) " " else "x"
        return match.groupValues[1] + nextMarker + match.groupValues[3]
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
}

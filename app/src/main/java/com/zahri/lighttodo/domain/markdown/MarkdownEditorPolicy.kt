package com.zahri.lighttodo.domain.markdown

object MarkdownEditorPolicy {
    data class Selection(
        val start: Int,
        val end: Int
    )

    fun continuationNewlineIndex(
        changeStart: Int,
        insertedText: String,
        isPaste: Boolean = false
    ): Int? {
        return changeStart.takeIf { !isPaste && insertedText == "\n" }
    }

    fun containsOffset(offset: Int, start: Int, endExclusive: Int): Boolean {
        return offset >= start && offset < endExclusive
    }

    fun containsVertical(vertical: Int, layoutHeight: Int): Boolean {
        return vertical >= 0 && vertical < layoutHeight
    }

    fun shouldRemoveSpanOnLineRefresh(
        spanStart: Int,
        spanEnd: Int,
        lineStart: Int,
        lineEndExclusive: Int
    ): Boolean {
        val overlaps = spanStart < lineEndExclusive && spanEnd > lineStart
        val collapsedAtBoundary = spanStart == spanEnd &&
            spanStart >= lineStart && spanStart <= lineEndExclusive
        return overlaps || collapsedAtBoundary
    }

    fun clampSelection(start: Int, end: Int, textLength: Int): Selection {
        return Selection(
            start = start.coerceIn(0, textLength),
            end = end.coerceIn(0, textLength)
        )
    }

    fun requiresFullSpanRefresh(
        removedText: String,
        insertedText: String,
        beforeLine: String,
        afterLine: String
    ): Boolean {
        if ('\n' in removedText || '\n' in insertedText) return true
        return beforeLine.isCodeFence() != afterLine.isCodeFence()
    }

    private fun String.isCodeFence(): Boolean = trimStart().startsWith("```")
}

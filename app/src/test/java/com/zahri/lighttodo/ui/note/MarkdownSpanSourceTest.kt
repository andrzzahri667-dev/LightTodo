package com.zahri.lighttodo.feature.noteeditor

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownSpanSourceTest {
    @Test
    fun bulletSpanOnlyDrawsBulletOnFirstVisualLine() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/noteeditor/MarkdownSpan.kt").readText()
        val bulletSpan = source.substringAfter("class MarkdownBulletSpan(")
            .substringBefore("/** Adds a small leading gutter for ordered list rows")

        assertTrue(bulletSpan.contains("if (!first) return"))
    }

    @Test
    fun cardPreviewReusesCompiledInlinePatterns() {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/MarkdownSpanApplier.kt"
        ).readText()

        assertTrue(source.contains("private val InlineCodeRegex"))
        assertTrue(source.contains("stripMarkdownLinks(text)"))
        assertFalse(source.contains(".replace(Regex("))
    }

    @Test
    fun inlineSpansReusePairedDelimiterRanges() {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/MarkdownSpanApplier.kt"
        ).readText()

        assertTrue(source.contains("val boldAsteriskRanges = MarkdownTextTransforms.findInlineStyleRanges("))
        assertTrue(source.contains("val italicAsteriskRanges = MarkdownTextTransforms.findInlineStyleRanges("))
        assertTrue(source.contains("val range = boldAsteriskRanges[i]"))
    }

    @Test
    fun activeHeadingKeepsTypographyWhileExposingItsMarker() {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/MarkdownSpanApplier.kt"
        ).readText()
        val activeLine = source
            .substringAfter("private fun applyActiveLine(")
            .substringBefore("private fun lineRangeAt(")

        assertTrue(activeLine.contains("HeadingRegex.matchEntire(line)"))
        assertTrue(activeLine.contains("MarkdownHeadingSpan(level)"))
        assertTrue(activeLine.contains("StyleSpan(Typeface.BOLD)"))
        assertFalse(activeLine.contains("MarkdownSyntaxSpan()"))
    }
}

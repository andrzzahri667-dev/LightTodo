package com.zahri.lighttodo.ui.note

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun render_supportsItalicWithoutLeavingMarkdownMarkers() {
        val rendered = MarkdownParser.render("plain *italic* end")

        assertEquals("plain italic end", rendered.text)
        assertTrue(
            rendered.spanStyles.any { range ->
                rendered.text.substring(range.start, range.end) == "italic" &&
                    range.item.fontStyle == FontStyle.Italic
            }
        )
    }

    @Test
    fun render_supportsOrderedLists() {
        val rendered = MarkdownParser.render("1. first\n2. second")

        assertEquals("1. first\n2. second", rendered.text)
    }

    @Test
    fun renderForEditing_hidesMarkdownMarkersImmediately() {
        val rendered = MarkdownParser.renderForEditing(
            "# 人\n" +
                "SSS\n" +
                "- 无序列表\n" +
                "1. 有序\n" +
                "**加粗**\n" +
                "*斜体*\n" +
                "---\n" +
                "## 小标题\n" +
                "~删除线~\n" +
                "> 呢呢"
        ).text

        assertEquals(
            "人\n" +
                "SSS\n" +
                "• 无序列表\n" +
                "1. 有序\n" +
                "加粗\n" +
                "斜体\n" +
                "────────\n" +
                "小标题\n" +
                "删除线\n" +
                "┃ 呢呢",
            rendered.text
        )
        assertTrue(rendered.text.startsWith("人\nSSS\n• 无序列表"))
    }

    @Test
    fun renderForEditing_appliesInlineStyles() {
        val rendered = MarkdownParser.renderForEditing("**加粗** *斜体* ~删除线~").text

        assertTrue(
            rendered.spanStyles.any { range ->
                rendered.text.substring(range.start, range.end) == "加粗" &&
                    range.item.fontWeight == FontWeight.Bold
            }
        )
        assertTrue(
            rendered.spanStyles.any { range ->
                rendered.text.substring(range.start, range.end) == "斜体" &&
                    range.item.fontStyle == FontStyle.Italic
            }
        )
        assertTrue(
            rendered.spanStyles.any { range ->
                rendered.text.substring(range.start, range.end) == "删除线" &&
                    range.item.textDecoration == TextDecoration.LineThrough
            }
        )
    }

    @Test
    fun renderForEditing_offsetMappingsStayInRangeAndMonotonic() {
        val source = "# 人\n- 无序列表\n**加粗**\n---\n> 呢呢"
        val rendered = MarkdownParser.renderForEditing(source)

        rendered.originalToTransformed.toList().zipWithNext().forEach { (previous, next) ->
            assertTrue(next >= previous)
            assertTrue(previous in 0..rendered.text.length)
        }
        rendered.transformedToOriginal.toList().zipWithNext().forEach { (previous, next) ->
            assertTrue(next >= previous)
            assertTrue(previous in 0..source.length)
        }
    }
}

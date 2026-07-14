package com.zahri.lighttodo.feature.noteeditor

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownSpanApplierTest {

    @Test
    fun visibleCharacterCount_usesRenderedTextInsteadOfMarkdownSource() {
        val count = MarkdownSpanApplier.visibleCharacterCount(
            title = "T",
            markdown = "**bold**\n- [ ] task"
        )

        assertEquals(10, count)
    }

    @Test
    fun visibleCharacterCount_keepsLiteralMarkdownInsideCodeFences() {
        assertEquals(
            5,
            MarkdownSpanApplier.visibleCharacterCount(
                title = "",
                markdown = "```\n**x**\n```"
            )
        )
    }

    @Test
    fun visibleCharacterCount_excludesAttachmentSourceAndLabels() {
        assertEquals(
            12,
            MarkdownSpanApplier.visibleCharacterCount(
                title = "",
                markdown = "Before\n![image](attachments/image/a.jpg)\n" +
                    "[audio 00:08](attachments/audio/a.m4a)\nAfter"
            )
        )
    }

    @Test
    fun stripMarkdown_removesBlockMarkersForCardPreview() {
        val preview = MarkdownSpanApplier.stripMarkdown(
            "# 人\n" +
                "SSS\n" +
                "- 无序列表\n" +
                "1. 有序\n" +
                "---\n" +
                "## 小标题\n" +
                "> 呢呢"
        )

        assertEquals(
            "人\n" +
                "SSS\n" +
                "无序列表\n" +
                "有序\n" +
                "小标题\n" +
                "呢呢",
            preview
        )
    }

    @Test
    fun stripMarkdown_removesInlineMarkersForCardPreview() {
        val preview = MarkdownSpanApplier.stripMarkdown(
            "**加粗** *斜体* ~~删除线~~ <u>下划线</u> `code` [官网](https://example.com)"
        )

        assertEquals("加粗 斜体 删除线 下划线 code 官网", preview)
    }

    @Test
    fun stripMarkdown_removesTaskListMarkersButKeepsText() {
        val preview = MarkdownSpanApplier.stripMarkdown(
            "- [ ] 未完成\n" +
                "- [x] 已完成\n" +
                "+ [X] 也完成"
        )

        assertEquals("未完成\n已完成\n也完成", preview)
    }

    @Test
    fun stripMarkdown_collapsesEmptyStructuralLines() {
        val preview = MarkdownSpanApplier.stripMarkdown(
            "before\n" +
                "```\n" +
                "val x = 1\n" +
                "```\n" +
                "***\n" +
                "after"
        )

        assertEquals("before\nval x = 1\nafter", preview)
    }

    @Test
    fun findMarkdownLinkRanges_findsLinkTextOnActiveLine() {
        val ranges = MarkdownSpanApplier.findMarkdownLinkRanges("[site](https://example.com)")

        assertEquals(
            listOf(
                MarkdownSpanApplier.LinkRange(
                    textStart = 1,
                    textEnd = 5,
                    suffixStart = 5,
                    suffixEnd = 27,
                    url = "https://example.com"
                )
            ),
            ranges
        )
    }

    @Test
    fun findMarkdownLinkRanges_keepsBalancedParenthesesInsideUrl() {
        val ranges = MarkdownSpanApplier.findMarkdownLinkRanges(
            "[math](https://host/wiki/Foo_(bar))"
        )

        assertEquals(
            listOf(
                MarkdownSpanApplier.LinkRange(
                    textStart = 1,
                    textEnd = 5,
                    suffixStart = 5,
                    suffixEnd = 35,
                    url = "https://host/wiki/Foo_(bar)"
                )
            ),
            ranges
        )
    }

    @Test
    fun stripMarkdown_keepsBalancedLinkUrlOutOfCardPreview() {
        val preview = MarkdownSpanApplier.stripMarkdown(
            "See [math](https://host/wiki/Foo_(bar)) today"
        )

        assertEquals("See math today", preview)
    }

    @Test
    fun stripMarkdown_preservesIdentifierUnderscores() {
        assertEquals("foo_bar_baz", MarkdownSpanApplier.stripMarkdown("foo_bar_baz"))
    }

    @Test
    fun stripMarkdown_stillRemovesStandaloneUnderscoreItalicMarkers() {
        assertEquals("italic", MarkdownSpanApplier.stripMarkdown("_italic_"))
    }
}

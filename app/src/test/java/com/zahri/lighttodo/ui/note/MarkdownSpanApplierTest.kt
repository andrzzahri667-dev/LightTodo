package com.zahri.lighttodo.feature.noteeditor

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownSpanApplierTest {

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
}

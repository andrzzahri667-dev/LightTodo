package com.zahri.lighttodo.ui.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownTextTransformsTest {

    @Test
    fun markdownLinkForPastedText_wrapsHttpUrl() {
        val wrapped = MarkdownTextTransforms.markdownLinkForPastedText("https://example.com/a?b=1")

        assertEquals("[https://example.com/a?b=1](https://example.com/a?b=1)", wrapped)
    }

    @Test
    fun markdownLinkForPastedText_wrapsWwwUrl() {
        val wrapped = MarkdownTextTransforms.markdownLinkForPastedText("www.example.com")

        assertEquals("[www.example.com](https://www.example.com)", wrapped)
    }

    @Test
    fun markdownLinkForPastedText_ignoresExistingMarkdownLink() {
        val wrapped = MarkdownTextTransforms.markdownLinkForPastedText("[site](https://example.com)")

        assertNull(wrapped)
    }

    @Test
    fun markdownLinkForPastedText_ignoresMultiWordText() {
        val wrapped = MarkdownTextTransforms.markdownLinkForPastedText("see https://example.com")

        assertNull(wrapped)
    }

    @Test
    fun toggleLinePrefix_replacesExistingBlockStyle() {
        val edit = MarkdownTextTransforms.toggleLinePrefix(
            text = "# Title",
            cursor = 3,
            prefix = "## "
        )

        assertEquals(
            MarkdownTextTransforms.Edit(0, 7, "## Title", 8),
            edit
        )
    }

    @Test
    fun toggleLinePrefix_removesMatchingBlockStyle() {
        val edit = MarkdownTextTransforms.toggleLinePrefix(
            text = "## Title",
            cursor = 4,
            prefix = "## "
        )

        assertEquals(
            MarkdownTextTransforms.Edit(0, 8, "Title", 5),
            edit
        )
    }

    @Test
    fun toggleLinePrefix_handlesCursorBeforeLeadingNewline() {
        val edit = MarkdownTextTransforms.toggleLinePrefix(
            text = "\n",
            cursor = 0,
            prefix = "- "
        )

        assertEquals(
            MarkdownTextTransforms.Edit(0, 0, "- ", 2),
            edit
        )
    }

    @Test
    fun toggleOrderedListPrefix_addsOrderedMarker() {
        val edit = MarkdownTextTransforms.toggleOrderedListPrefix(
            text = "- item",
            cursor = 3
        )

        assertEquals(
            MarkdownTextTransforms.Edit(0, 6, "1. item", 7),
            edit
        )
    }

    @Test
    fun toggleOrderedListPrefix_removesAnyOrderedMarker() {
        val edit = MarkdownTextTransforms.toggleOrderedListPrefix(
            text = "2. item",
            cursor = 3
        )

        assertEquals(
            MarkdownTextTransforms.Edit(0, 7, "item", 4),
            edit
        )
    }

    @Test
    fun toggleInlineStyleAtCursor_wrapsCurrentLineTextWhenNoTextIsSelected() {
        val edit = MarkdownTextTransforms.toggleInlineStyleAtCursor(
            text = "hello world",
            cursor = 3,
            openMarker = "**",
            closeMarker = "**"
        )

        assertEquals(
            MarkdownTextTransforms.Edit(0, 11, "**hello world**", 15),
            edit
        )
    }

    @Test
    fun toggleInlineStyleAtCursor_wrapsOnlyBlockContentWhenLineHasPrefix() {
        val edit = MarkdownTextTransforms.toggleInlineStyleAtCursor(
            text = "# Title",
            cursor = 3,
            openMarker = "**",
            closeMarker = "**"
        )

        assertEquals(
            MarkdownTextTransforms.Edit(0, 7, "# **Title**", 11),
            edit
        )
    }

    @Test
    fun toggleInlineStyleAtCursor_unwrapsCurrentLineTextWhenAlreadyWrapped() {
        val edit = MarkdownTextTransforms.toggleInlineStyleAtCursor(
            text = "- **item**",
            cursor = 5,
            openMarker = "**",
            closeMarker = "**"
        )

        assertEquals(
            MarkdownTextTransforms.Edit(0, 10, "- item", 6),
            edit
        )
    }

    @Test
    fun toggleInlineStyleAtCursor_insertsMarkersOnEmptyLine() {
        val edit = MarkdownTextTransforms.toggleInlineStyleAtCursor(
            text = "before\n\nnext",
            cursor = 7,
            openMarker = "<u>",
            closeMarker = "</u>"
        )

        assertEquals(
            MarkdownTextTransforms.Edit(7, 7, "<u></u>", 10),
            edit
        )
    }

    @Test
    fun toggleTaskListLine_checksUncheckedItem() {
        val toggled = MarkdownTextTransforms.toggleTaskListLine("- [ ] buy milk")

        assertEquals("- [x] buy milk", toggled)
    }

    @Test
    fun toggleTaskListLine_checksCompactUncheckedItem() {
        val toggled = MarkdownTextTransforms.toggleTaskListLine("- [] buy milk")

        assertEquals("- [x] buy milk", toggled)
    }

    @Test
    fun toggleTaskListLine_unchecksCheckedItem() {
        val toggled = MarkdownTextTransforms.toggleTaskListLine("  + [X] done")

        assertEquals("  + [ ] done", toggled)
    }

    @Test
    fun toggleTaskListLine_ignoresRegularListItem() {
        val toggled = MarkdownTextTransforms.toggleTaskListLine("- buy milk")

        assertNull(toggled)
    }

    @Test
    fun listContinuation_continuesOrderedList() {
        val edit = MarkdownTextTransforms.listContinuationAfterNewline("1. 333\n", newlineIndex = 6)

        assertEquals(MarkdownTextTransforms.Edit(7, 7, "2. ", 10), edit)
    }

    @Test
    fun listContinuation_continuesUnorderedList() {
        val edit = MarkdownTextTransforms.listContinuationAfterNewline("- 333\n", newlineIndex = 5)

        assertEquals(MarkdownTextTransforms.Edit(6, 6, "- ", 8), edit)
    }

    @Test
    fun listContinuation_continuesTaskList() {
        val edit = MarkdownTextTransforms.listContinuationAfterNewline("- [x] 333\n", newlineIndex = 9)

        assertEquals(MarkdownTextTransforms.Edit(10, 10, "- [ ] ", 16), edit)
    }

    @Test
    fun listContinuation_exitsEmptyOrderedList() {
        val edit = MarkdownTextTransforms.listContinuationAfterNewline("1. \n", newlineIndex = 3)

        assertEquals(MarkdownTextTransforms.Edit(0, 3, "", 0), edit)
    }

    @Test
    fun listContinuation_exitsEmptyUnorderedList() {
        val edit = MarkdownTextTransforms.listContinuationAfterNewline("- \n", newlineIndex = 2)

        assertEquals(MarkdownTextTransforms.Edit(0, 2, "", 0), edit)
    }

    @Test
    fun listContinuation_exitsEmptyTaskList() {
        val edit = MarkdownTextTransforms.listContinuationAfterNewline("- [ ] \n", newlineIndex = 6)

        assertEquals(MarkdownTextTransforms.Edit(0, 6, "", 0), edit)
    }

    @Test
    fun listContinuation_exitsEmptySecondOrderedLineWithCursorOnSecondLine() {
        val edit = MarkdownTextTransforms.listContinuationAfterNewline("1. 111\n2. \n", newlineIndex = 10)

        assertEquals(MarkdownTextTransforms.Edit(7, 10, "", 7), edit)
    }
}

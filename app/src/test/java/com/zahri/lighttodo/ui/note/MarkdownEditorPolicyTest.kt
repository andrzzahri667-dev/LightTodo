package com.zahri.lighttodo.domain.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownEditorPolicyTest {

    @Test
    fun multilinePasteDoesNotRequestListContinuation() {
        assertNull(
            MarkdownEditorPolicy.continuationNewlineIndex(
                changeStart = 4,
                insertedText = "first\nsecond"
            )
        )
    }

    @Test
    fun singleInsertedNewlineRequestsListContinuation() {
        assertEquals(
            4,
            MarkdownEditorPolicy.continuationNewlineIndex(
                changeStart = 4,
                insertedText = "\n"
            )
        )
    }

    @Test
    fun pastedSingleNewlineDoesNotRequestListContinuation() {
        assertNull(
            MarkdownEditorPolicy.continuationNewlineIndex(
                changeStart = 4,
                insertedText = "\n",
                isPaste = true
            )
        )
    }

    @Test
    fun spanHitRangeExcludesItsEndOffset() {
        assertTrue(MarkdownEditorPolicy.containsOffset(offset = 4, start = 2, endExclusive = 5))
        assertFalse(MarkdownEditorPolicy.containsOffset(offset = 5, start = 2, endExclusive = 5))
    }

    @Test
    fun textLayoutVerticalRangeExcludesTheAreaBelowItsHeight() {
        assertTrue(MarkdownEditorPolicy.containsVertical(vertical = 39, layoutHeight = 40))
        assertFalse(MarkdownEditorPolicy.containsVertical(vertical = 40, layoutHeight = 40))
        assertFalse(MarkdownEditorPolicy.containsVertical(vertical = -1, layoutHeight = 40))
    }

    @Test
    fun lineRefreshRemovesCollapsedSpansAtBothBoundaries() {
        assertTrue(
            MarkdownEditorPolicy.shouldRemoveSpanOnLineRefresh(
                spanStart = 2,
                spanEnd = 2,
                lineStart = 2,
                lineEndExclusive = 8
            )
        )
        assertTrue(
            MarkdownEditorPolicy.shouldRemoveSpanOnLineRefresh(
                spanStart = 8,
                spanEnd = 8,
                lineStart = 2,
                lineEndExclusive = 8
            )
        )
    }

    @Test
    fun lineRefreshKeepsNonOverlappingSpansOnAdjacentLines() {
        assertFalse(
            MarkdownEditorPolicy.shouldRemoveSpanOnLineRefresh(
                spanStart = 0,
                spanEnd = 2,
                lineStart = 2,
                lineEndExclusive = 8
            )
        )
        assertFalse(
            MarkdownEditorPolicy.shouldRemoveSpanOnLineRefresh(
                spanStart = 8,
                spanEnd = 10,
                lineStart = 2,
                lineEndExclusive = 8
            )
        )
    }

    @Test
    fun selectionClampingPreservesBothEndpoints() {
        assertEquals(
            MarkdownEditorPolicy.Selection(start = 2, end = 5),
            MarkdownEditorPolicy.clampSelection(start = 2, end = 8, textLength = 5)
        )
    }

    @Test
    fun selectionClampingPreservesReversedSelectionDirection() {
        assertEquals(
            MarkdownEditorPolicy.Selection(start = 5, end = 1),
            MarkdownEditorPolicy.clampSelection(start = 8, end = 1, textLength = 5)
        )
    }

    @Test
    fun ordinarySingleLineTypingUsesIncrementalSpanRefresh() {
        assertFalse(
            MarkdownEditorPolicy.requiresFullSpanRefresh(
                removedText = "",
                insertedText = "x",
                beforeLine = "abc",
                afterLine = "abxc"
            )
        )
    }

    @Test
    fun newlineInsertionAndRemovalRequireFullSpanRefresh() {
        assertTrue(
            MarkdownEditorPolicy.requiresFullSpanRefresh(
                removedText = "",
                insertedText = "\n",
                beforeLine = "abc",
                afterLine = "abc"
            )
        )
        assertTrue(
            MarkdownEditorPolicy.requiresFullSpanRefresh(
                removedText = "\n",
                insertedText = "",
                beforeLine = "abc",
                afterLine = "abc"
            )
        )
    }

    @Test
    fun fencedCodeBoundaryChangeRequiresFullSpanRefresh() {
        assertTrue(
            MarkdownEditorPolicy.requiresFullSpanRefresh(
                removedText = "`",
                insertedText = "",
                beforeLine = "```kotlin",
                afterLine = "``kotlin"
            )
        )
    }

    @Test
    fun editingFenceInfoStringKeepsLineScopedRefresh() {
        assertFalse(
            MarkdownEditorPolicy.requiresFullSpanRefresh(
                removedText = "n",
                insertedText = "x",
                beforeLine = "```kotlin",
                afterLine = "```kotlix"
            )
        )
    }
}

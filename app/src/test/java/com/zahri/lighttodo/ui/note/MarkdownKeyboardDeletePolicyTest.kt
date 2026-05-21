package com.zahri.lighttodo.ui.note

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownKeyboardDeletePolicyTest {

    @Test
    fun shouldRequestPreviousMediaDelete_whenCursorIsAtTextBlockStart() {
        assertTrue(
            MarkdownKeyboardDeletePolicy.shouldRequestPreviousMediaDelete(
                text = "",
                selectionStart = 0,
                selectionEnd = 0
            )
        )
    }

    @Test
    fun shouldNotRequestPreviousMediaDelete_whenCursorIsAfterBlankLineNewline() {
        assertFalse(
            MarkdownKeyboardDeletePolicy.shouldRequestPreviousMediaDelete(
                text = "\n",
                selectionStart = 1,
                selectionEnd = 1
            )
        )
    }

    @Test
    fun shouldNotRequestPreviousMediaDelete_whenTextSelectionIsExpanded() {
        assertFalse(
            MarkdownKeyboardDeletePolicy.shouldRequestPreviousMediaDelete(
                text = "abc",
                selectionStart = 0,
                selectionEnd = 2
            )
        )
    }
}

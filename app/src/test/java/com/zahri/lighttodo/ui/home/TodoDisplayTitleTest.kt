package com.zahri.lighttodo.feature.home

import com.zahri.lighttodo.domain.todo.TodoDisplayText
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoDisplayTitleTest {

    @Test
    fun displayTitleUsesExplicitTitleFirst() {
        assertEquals("Title", TodoDisplayText.title(title = "Title", note = "Note", fallback = "Untitled"))
    }

    @Test
    fun displayTitleFallsBackToFirstNoteLine() {
        assertEquals("Note line", TodoDisplayText.title(title = " ", note = "Note line\nMore", fallback = "Untitled"))
    }

    @Test
    fun displayTitleUsesProvidedFallbackForBlankTodo() {
        assertEquals("Untitled", TodoDisplayText.title(title = " ", note = "", fallback = "Untitled"))
    }
}

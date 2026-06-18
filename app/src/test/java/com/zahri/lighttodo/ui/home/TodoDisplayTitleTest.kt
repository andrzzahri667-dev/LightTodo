package com.zahri.lighttodo.feature.home

import com.zahri.lighttodo.data.TodoEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoDisplayTitleTest {

    @Test
    fun displayTitleUsesExplicitTitleFirst() {
        val todo = TodoEntity(title = "Title", note = "Note")

        assertEquals("Title", todo.displayTitle(fallback = "Untitled"))
    }

    @Test
    fun displayTitleFallsBackToFirstNoteLine() {
        val todo = TodoEntity(title = " ", note = "Note line\nMore")

        assertEquals("Note line", todo.displayTitle(fallback = "Untitled"))
    }

    @Test
    fun displayTitleUsesProvidedFallbackForBlankTodo() {
        val todo = TodoEntity(title = " ", note = "")

        assertEquals("Untitled", todo.displayTitle(fallback = "Untitled"))
    }
}

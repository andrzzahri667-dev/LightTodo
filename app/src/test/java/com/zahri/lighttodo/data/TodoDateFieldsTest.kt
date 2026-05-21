package com.zahri.lighttodo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodoDateFieldsTest {

    @Test
    fun none_representsUndatedTodo() {
        val fields = TodoDateFields.None

        assertNull(fields.date)
        assertNull(fields.dateMillis)
    }

    @Test
    fun fromParts_requiresCompleteDateParts() {
        val fields = TodoDateFields.fromParts(year = 2026, month = null, day = 21)

        assertEquals(TodoDateFields.None, fields)
    }

    @Test
    fun fromStored_rejectsMismatchedNullability() {
        try {
            TodoDateFields.fromStored(date = 20260521, dateMillis = null)
        } catch (e: IllegalArgumentException) {
            assertEquals("Stored todo date fields must be both null or both non-null.", e.message)
            return
        }

        error("Expected mismatched date fields to be rejected")
    }
}

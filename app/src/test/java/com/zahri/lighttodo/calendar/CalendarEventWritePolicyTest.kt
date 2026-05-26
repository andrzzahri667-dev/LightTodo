package com.zahri.lighttodo.calendar

import com.zahri.lighttodo.data.TodoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarEventWritePolicyTest {
    @Test
    fun draftFor_returnsNullForUndatedTodos() {
        assertNull(
            CalendarEventWritePolicy.draftFor(
                TodoEntity(title = "No date", dateMillis = null)
            )
        )
    }

    @Test
    fun draftFor_usesExclusiveNextDayEndForAllDayTodos() {
        val draft = CalendarEventWritePolicy.draftFor(
            TodoEntity(
                title = "All day",
                note = "description",
                dateMillis = 10_000L
            )
        )

        requireNotNull(draft)
        assertEquals("All day", draft.title)
        assertEquals("description", draft.description)
        assertEquals(10_000L, draft.startMillis)
        assertEquals(10_000L + CalendarEventWritePolicy.DayMillis, draft.endMillis)
        assertTrue(draft.allDay)
        assertFalse(draft.completed)
    }

    @Test
    fun draftFor_usesTodoStartAndDeadlineForTimedTodos() {
        val draft = CalendarEventWritePolicy.draftFor(
            TodoEntity(
                title = "Timed",
                dateMillis = 10_000L,
                startHour = 9,
                startMinute = 30,
                deadlineHour = 10,
                deadlineMinute = 45,
                done = true
            )
        )

        requireNotNull(draft)
        assertEquals(10_000L + 9 * 3_600_000L + 30 * 60_000L, draft.startMillis)
        assertEquals(10_000L + 10 * 3_600_000L + 45 * 60_000L, draft.endMillis)
        assertFalse(draft.allDay)
        assertTrue(draft.completed)
    }
}

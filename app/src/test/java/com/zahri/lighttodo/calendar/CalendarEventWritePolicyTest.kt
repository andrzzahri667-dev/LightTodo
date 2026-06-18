package com.zahri.lighttodo.domain.calendar

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
                calendarEventTodo(title = "No date", dateMillis = null)
            )
        )
    }

    @Test
    fun draftFor_usesExclusiveNextDayEndForAllDayTodos() {
        val draft = CalendarEventWritePolicy.draftFor(
            calendarEventTodo(
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
            calendarEventTodo(
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

    private fun calendarEventTodo(
        title: String? = null,
        note: String? = null,
        dateMillis: Long? = null,
        startHour: Int? = null,
        startMinute: Int? = null,
        deadlineHour: Int? = null,
        deadlineMinute: Int? = null,
        done: Boolean = false
    ): CalendarEventTodo =
        CalendarEventTodo(
            title = title,
            note = note,
            dateMillis = dateMillis,
            startHour = startHour,
            startMinute = startMinute,
            deadlineHour = deadlineHour,
            deadlineMinute = deadlineMinute,
            done = done
        )
}

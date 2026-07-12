package com.zahri.lighttodo.domain.calendar

import com.zahri.lighttodo.test.sourceFile
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarEventWritePolicyTest {
    private val newYork = ZoneId.of("America/New_York")

    @Test
    fun draftFor_returnsNullForUndatedTodos() {
        assertNull(CalendarEventWritePolicy.draftFor(calendarEventTodo(date = null), newYork))
    }

    @Test
    fun draftFor_usesUtcMidnightsForAllDayTodos() {
        val draft = CalendarEventWritePolicy.draftFor(
            calendarEventTodo(
                title = "All day",
                note = "description",
                date = 20260308
            ),
            newYork
        )

        requireNotNull(draft)
        assertEquals("All day", draft.title)
        assertEquals("description", draft.description)
        assertEquals(Instant.parse("2026-03-08T00:00:00Z").toEpochMilli(), draft.startMillis)
        assertEquals(Instant.parse("2026-03-09T00:00:00Z").toEpochMilli(), draft.endMillis)
        assertTrue(draft.allDay)
        assertEquals("UTC", draft.timezoneId)
        assertFalse(draft.completed)
    }

    @Test
    fun draftFor_appliesNewYorkSpringDstOffsetToTimedTodos() {
        val draft = CalendarEventWritePolicy.draftFor(
            calendarEventTodo(
                title = "Spring",
                date = 20260308,
                startHour = 9,
                startMinute = 30,
                deadlineHour = 10,
                deadlineMinute = 45
            ),
            newYork
        )

        requireNotNull(draft)
        assertEquals(Instant.parse("2026-03-08T13:30:00Z").toEpochMilli(), draft.startMillis)
        assertEquals(Instant.parse("2026-03-08T14:45:00Z").toEpochMilli(), draft.endMillis)
        assertFalse(draft.allDay)
        assertEquals("America/New_York", draft.timezoneId)
    }

    @Test
    fun draftFor_appliesNewYorkFallDstOffsetToTimedTodos() {
        val draft = CalendarEventWritePolicy.draftFor(
            calendarEventTodo(
                title = "Fall",
                date = 20261101,
                startHour = 9,
                startMinute = 30,
                deadlineHour = 10,
                deadlineMinute = 45,
                done = true
            ),
            newYork
        )

        requireNotNull(draft)
        assertEquals(Instant.parse("2026-11-01T14:30:00Z").toEpochMilli(), draft.startMillis)
        assertEquals(Instant.parse("2026-11-01T15:45:00Z").toEpochMilli(), draft.endMillis)
        assertEquals("America/New_York", draft.timezoneId)
        assertTrue(draft.completed)
    }

    @Test
    fun calendarWriterUsesTimezoneFromDraft() {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/integration/calendar/CalendarEventWriter.kt"
        ).readText()

        assertTrue(source.contains("CalendarContract.Events.EVENT_TIMEZONE, draft.timezoneId"))
        assertTrue(source.contains("CalendarContract.Events.EVENT_END_TIMEZONE, draft.timezoneId"))
        assertFalse(source.contains("TimeZone.getDefault()"))
        assertTrue(source.contains("date = date"))
    }

    private fun calendarEventTodo(
        title: String? = null,
        note: String? = null,
        date: Int?,
        startHour: Int? = null,
        startMinute: Int? = null,
        deadlineHour: Int? = null,
        deadlineMinute: Int? = null,
        done: Boolean = false
    ): CalendarEventTodo =
        CalendarEventTodo(
            title = title,
            note = note,
            date = date,
            startHour = startHour,
            startMinute = startMinute,
            deadlineHour = deadlineHour,
            deadlineMinute = deadlineMinute,
            done = done
        )
}

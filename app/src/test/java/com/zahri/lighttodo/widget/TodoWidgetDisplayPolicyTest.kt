package com.zahri.lighttodo.integration.widget

import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.util.DateUtils
import java.time.LocalDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoWidgetDisplayPolicyTest {

    @Test
    fun subtitleOverdue_marksTodayTimeRangeAfterDeadlinePasses() {
        val now = LocalDateTime.of(2026, 5, 24, 11, 0)
        val todo = TodoEntity(
            date = DateUtils.toDayKey(now.toLocalDate()),
            startHour = 10,
            startMinute = 0,
            deadlineHour = 10,
            deadlineMinute = 30
        )

        assertTrue(TodoWidgetDisplayPolicy.isSubtitleOverdue(todo, now))
    }

    @Test
    fun subtitleOverdue_keepsFutureTimeRangeSecondary() {
        val now = LocalDateTime.of(2026, 5, 24, 9, 0)
        val todo = TodoEntity(
            date = DateUtils.toDayKey(now.toLocalDate()),
            startHour = 10,
            startMinute = 0,
            deadlineHour = 10,
            deadlineMinute = 30
        )

        assertFalse(TodoWidgetDisplayPolicy.isSubtitleOverdue(todo, now))
    }

    @Test
    fun subtitleOverdue_keepsAllDayTodaySecondary() {
        val now = LocalDateTime.of(2026, 5, 24, 23, 0)
        val todo = TodoEntity(date = DateUtils.toDayKey(now.toLocalDate()))

        assertFalse(TodoWidgetDisplayPolicy.isSubtitleOverdue(todo, now))
    }

    @Test
    fun subtitleOverdue_keepsAllDayPastDateSecondary() {
        val now = LocalDateTime.of(2026, 5, 24, 9, 0)
        val todo = TodoEntity(date = DateUtils.toDayKey(now.toLocalDate().minusDays(1)))

        assertFalse(TodoWidgetDisplayPolicy.isSubtitleOverdue(todo, now))
    }

    @Test
    fun subtitleOverdue_keepsUndatedTodoSecondary() {
        val now = LocalDateTime.of(2026, 5, 24, 11, 0)
        val todo = TodoEntity(date = null)

        assertFalse(TodoWidgetDisplayPolicy.isSubtitleOverdue(todo, now))
    }

    @Test
    fun subtitleOverdue_marksPastDateWithTimeRangeOverdue() {
        val now = LocalDateTime.of(2026, 5, 24, 9, 0)
        val todo = TodoEntity(
            date = DateUtils.toDayKey(now.toLocalDate().minusDays(1)),
            startHour = 10,
            startMinute = 0,
            deadlineHour = 10,
            deadlineMinute = 30
        )

        assertTrue(TodoWidgetDisplayPolicy.isSubtitleOverdue(todo, now))
    }

    @Test
    fun subtitleOverdue_ignoresDoneTodo() {
        val now = LocalDateTime.of(2026, 5, 24, 11, 0)
        val todo = TodoEntity(
            date = DateUtils.toDayKey(now.toLocalDate()),
            startHour = 10,
            startMinute = 0,
            deadlineHour = 10,
            deadlineMinute = 30,
            done = true
        )

        assertFalse(TodoWidgetDisplayPolicy.isSubtitleOverdue(todo, now))
    }

    @Test
    fun deadlineSuffix_formatsTimeRangeOnly() {
        val now = LocalDateTime.of(2026, 5, 24, 11, 0)
        val ranged = TodoEntity(startHour = 9, startMinute = 0, deadlineHour = 10, deadlineMinute = 30)
        val allDay = TodoEntity(date = DateUtils.toDayKey(now.toLocalDate()))
        val undated = TodoEntity(date = null)

        assertTrue(TodoWidgetDisplayPolicy.deadlineSuffix(ranged).contains("09:00-10:30"))
        assertTrue(TodoWidgetDisplayPolicy.deadlineSuffix(allDay).isEmpty())
        assertTrue(TodoWidgetDisplayPolicy.deadlineSuffix(undated).isEmpty())
    }

    @Test
    fun deadlineSuffix_usesAsciiDigitsInEveryLocale() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            val todo = TodoEntity(startHour = 9, startMinute = 5, deadlineHour = 10, deadlineMinute = 30)

            assertEquals(" 09:05-10:30", TodoWidgetDisplayPolicy.deadlineSuffix(todo))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}

package com.zahri.lighttodo.data.todo

import com.zahri.lighttodo.data.local.TagDao
import com.zahri.lighttodo.data.local.TodoDao
import com.zahri.lighttodo.domain.todo.TodoInput
import com.zahri.lighttodo.usecase.todo.TodoPreferencesSnapshot
import java.lang.reflect.Proxy
import java.time.Instant
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoInputRecordBuilderDstTest {
    @Test
    fun build_springForwardReminderKeepsRequestedWallClockTime() {
        val record = buildAtNewYorkDate(month = 3, day = 8)
        val expected = Instant.parse("2026-03-08T13:30:00Z").toEpochMilli()

        assertEquals(expected, record.remindStartAtMillis)
        assertEquals(expected, record.remindAtMillis)
    }

    @Test
    fun build_fallBackReminderKeepsRequestedWallClockTime() {
        val record = buildAtNewYorkDate(month = 11, day = 1)
        val expected = Instant.parse("2026-11-01T14:30:00Z").toEpochMilli()

        assertEquals(expected, record.remindStartAtMillis)
        assertEquals(expected, record.remindAtMillis)
    }

    private fun buildAtNewYorkDate(month: Int, day: Int) = withDefaultTimeZone("America/New_York") {
        runBlocking {
            TodoInputRecordBuilder(
                todoDao = unusedProxy(),
                tagDao = unusedProxy()
            ).build(
                input = TodoInput(
                    title = "DST",
                    note = null,
                    year = 2026,
                    month = month,
                    day = day,
                    startHour = 9,
                    startMinute = 30,
                    deadlineHour = 9,
                    deadlineMinute = 30,
                    customHoursBefore = 0
                ),
                prefsSnapshot = TodoPreferencesSnapshot(),
                now = 1L
            )
        }
    }

    private inline fun <T> withDefaultTimeZone(id: String, block: () -> T): T {
        val previous = TimeZone.getDefault()
        return try {
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            block()
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> unusedProxy(): T =
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java)
        ) { _, method, _ -> error("Unexpected DAO call: ${method.name}") } as T
}

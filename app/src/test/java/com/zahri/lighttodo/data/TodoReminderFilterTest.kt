package com.zahri.lighttodo.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoReminderFilterTest {

    @Test
    fun hasAnyReminder_acceptsStartOnlyReminders() {
        val todo = TodoEntity(remindStartAtMillis = 1_000L, remindAtMillis = null)

        assertTrue(todo.hasAnyReminder())
    }

    @Test
    fun hasAnyReminder_acceptsDeadlineOnlyReminders() {
        val todo = TodoEntity(remindStartAtMillis = null, remindAtMillis = 2_000L)

        assertTrue(todo.hasAnyReminder())
    }

    @Test
    fun hasAnyReminder_rejectsTodosWithoutReminders() {
        val todo = TodoEntity(remindStartAtMillis = null, remindAtMillis = null)

        assertFalse(todo.hasAnyReminder())
    }
}

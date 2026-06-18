package com.zahri.lighttodo.domain.todo

import org.junit.Assert.assertEquals
import org.junit.Test

class TodoReminderDefaultsTest {

    @Test
    fun effectiveHoursBefore_usesDefaultWhenTodoHasNoOverride() {
        assertEquals(2, TodoReminderDefaults.effectiveHoursBefore(customHoursBefore = null, defaultHoursBefore = 2))
    }

    @Test
    fun effectiveHoursBefore_keepsExplicitOnTimeOverride() {
        assertEquals(0, TodoReminderDefaults.effectiveHoursBefore(customHoursBefore = 0, defaultHoursBefore = 2))
    }

    @Test
    fun adjustHoursBefore_startsFromDefaultWhenTodoHasNoOverride() {
        assertEquals(3, TodoReminderDefaults.adjustHoursBefore(customHoursBefore = null, defaultHoursBefore = 2, delta = 1))
        assertEquals(1, TodoReminderDefaults.adjustHoursBefore(customHoursBefore = null, defaultHoursBefore = 2, delta = -1))
    }

    @Test
    fun adjustHoursBefore_clampsToSupportedRange() {
        assertEquals(0, TodoReminderDefaults.adjustHoursBefore(customHoursBefore = 0, defaultHoursBefore = 2, delta = -1))
        assertEquals(72, TodoReminderDefaults.adjustHoursBefore(customHoursBefore = 72, defaultHoursBefore = 2, delta = 1))
    }
}

package com.zahri.lighttodo.notify

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderRequestCodePolicyTest {
    @Test
    fun requestCodeFor_largeIdsStaysNonNegative() {
        val start = ReminderRequestCodePolicy.requestCodeFor(id = 300_000_000L, isStart = true)
        val end = ReminderRequestCodePolicy.requestCodeFor(id = 300_000_000L, isStart = false)

        assertTrue(start >= 0)
        assertTrue(end >= 0)
        assertNotEquals(start, end)
    }

    @Test
    fun requestCodeFor_foldsLongIdsWithoutThrowing() {
        val start = ReminderRequestCodePolicy.requestCodeFor(id = Long.MAX_VALUE, isStart = true)
        val end = ReminderRequestCodePolicy.requestCodeFor(id = Long.MAX_VALUE, isStart = false)

        assertTrue(start >= 0)
        assertTrue(end >= 0)
        assertNotEquals(start, end)
    }
}

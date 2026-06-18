package com.zahri.lighttodo.domain.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarSyncPolicyTest {
    @Test
    fun matchesAccount_withoutUserFilterMatchesAllNonHolidayCalendars() {
        // No user filter → match all (holiday exclusion is separate)
        assertTrue(CalendarSyncPolicy.matchesAccount("xiaomi_user", acctType = "com.android.exchange", userFilter = ""))
        assertTrue(CalendarSyncPolicy.matchesAccount("user@gmail.com", acctType = "com.google", userFilter = ""))
        assertTrue(CalendarSyncPolicy.matchesAccount("13800138000", acctType = "local", userFilter = ""))
    }

    @Test
    fun matchesAccount_withUserFilterUsesExplicitContainsMatch() {
        assertTrue(CalendarSyncPolicy.matchesAccount("work-calendar", acctType = "local", userFilter = "work"))
        assertTrue(CalendarSyncPolicy.matchesAccount("work-calendar", acctType = "local", userFilter = "WORK-CALENDAR"))
        assertFalse(CalendarSyncPolicy.matchesAccount("personal", acctType = "local", userFilter = "work"))
    }

    @Test
    fun shouldExcludeCalendarName_filtersHolidayCalendars() {
        assertTrue(CalendarSyncPolicy.shouldExcludeCalendarName("中国节假日"))
        assertTrue(CalendarSyncPolicy.shouldExcludeCalendarName("US Holidays"))
        assertFalse(CalendarSyncPolicy.shouldExcludeCalendarName("MI Account"))
    }

    @Test
    fun orphanEventIds_returnsImportedCalendarEventsMissingFromProviderSnapshot() {
        assertEquals(
            listOf(1L, 4L),
            CalendarSyncPolicy.orphanEventIds(
                importedEventIds = listOf(1L, 2L, 3L, 4L),
                providerEventIds = listOf(2L, 3L)
            )
        )
    }

    @Test
    fun mergeDoneState_marksTodoDoneWhenProviderEventIsCanceled() {
        val result = CalendarSyncPolicy.mergeDoneState(
            providerCanceled = true,
            existingDone = false,
            existingDoneAtMillis = null,
            nowMillis = 1234L
        )

        assertTrue(result.done)
        assertEquals(1234L, result.doneAtMillis)
    }

    @Test
    fun mergeDoneState_preservesExistingDoneWhenProviderEventIsActive() {
        val result = CalendarSyncPolicy.mergeDoneState(
            providerCanceled = false,
            existingDone = true,
            existingDoneAtMillis = 42L,
            nowMillis = 1234L
        )

        assertTrue(result.done)
        assertEquals(42L, result.doneAtMillis)
    }
}

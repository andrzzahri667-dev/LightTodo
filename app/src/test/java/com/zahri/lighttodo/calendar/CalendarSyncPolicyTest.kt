package com.zahri.lighttodo.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarSyncPolicyTest {
    @Test
    fun matchesAccount_withoutUserFilterOnlyAllowsXiaomiAccounts() {
        assertTrue(CalendarSyncPolicy.matchesAccount("xiaomi_user", userFilter = ""))
        assertTrue(CalendarSyncPolicy.matchesAccount("MI Account", userFilter = ""))
        assertTrue(CalendarSyncPolicy.matchesAccount("小米日历", userFilter = ""))

        assertFalse(CalendarSyncPolicy.matchesAccount("user@gmail.com", userFilter = ""))
        assertFalse(CalendarSyncPolicy.matchesAccount("outlook", userFilter = ""))
        assertFalse(CalendarSyncPolicy.matchesAccount("microsoft", userFilter = ""))
    }

    @Test
    fun matchesAccount_withUserFilterUsesExplicitContainsMatch() {
        assertTrue(CalendarSyncPolicy.matchesAccount("work-calendar", userFilter = "work"))
        assertTrue(CalendarSyncPolicy.matchesAccount("work-calendar", userFilter = "WORK-CALENDAR"))
        assertFalse(CalendarSyncPolicy.matchesAccount("personal", userFilter = "work"))
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
}

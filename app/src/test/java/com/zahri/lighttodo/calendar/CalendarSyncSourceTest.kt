package com.zahri.lighttodo.calendar

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarSyncSourceTest {

    @Test
    fun orphanCleanupUsesTodoDayKeysInsideSyncWindow() {
        val syncSource = sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/calendar/SyncCalendarUseCase.kt")
            .readText()
        val daoSource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/local/Daos.kt")
            .readText()

        assertTrue(syncSource.contains("listUndoneCalendarEventIdsInDateRange("))
        assertTrue(daoSource.contains("done = 0"))
        assertTrue(daoSource.contains("date >= :fromDayKey"))
        assertTrue(daoSource.contains("date <= :toDayKey"))
        assertFalse(daoSource.contains("dateMillis >= :fromMillis"))
        assertFalse(daoSource.contains("dateMillis <= :toMillis"))
    }

    @Test
    fun providerQueryUsesSeparateLocalTimedAndUtcAllDayWindows() {
        val gatewaySource = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/integration/calendar/AndroidCalendarSyncGateway.kt"
        ).readText()

        assertTrue(gatewaySource.contains("CalendarContract.Events.ALL_DAY"))
        assertTrue(gatewaySource.contains("window.timedFromMillis.toString()"))
        assertTrue(gatewaySource.contains("window.timedToExclusiveMillis.toString()"))
        assertTrue(gatewaySource.contains("window.allDayFromMillis.toString()"))
        assertTrue(gatewaySource.contains("window.allDayToExclusiveMillis.toString()"))
        assertTrue(gatewaySource.contains("DTSTART} < ?"))
    }

    @Test
    fun orphanCleanupUnlinksAppCreatedTodosInsteadOfDeletingThem() {
        val syncSource = sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/calendar/SyncCalendarUseCase.kt")
            .readText()

        assertTrue(syncSource.contains("val orphanTodos = repository.findTodosByCalendarEventIds(orphanEventIds)"))
        assertTrue(syncSource.contains(".filter { it.calendarCreatedByApp }"))
        assertTrue(syncSource.contains("repository.setTodoCalendarLink("))
        assertTrue(syncSource.contains("eventId = null"))
        assertTrue(syncSource.contains("createdByApp = false"))
        assertTrue(syncSource.contains(".filterNot { it.calendarCreatedByApp }"))
        assertTrue(syncSource.contains("repository.deleteLocalTodos(it)"))
    }
}

package com.zahri.lighttodo.calendar

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarSyncSourceTest {

    @Test
    fun orphanCleanupOnlyConsidersUndoneImportedEventsInsideSyncWindow() {
        val syncSource = sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/calendar/SyncCalendarUseCase.kt")
            .readText()
        val daoSource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/local/Daos.kt")
            .readText()

        assertTrue(syncSource.contains("listUndoneCalendarEventIdsInWindow(from, to)"))
        assertFalse(syncSource.contains("listCalendarEventIds()"))
        assertTrue(daoSource.contains("done = 0"))
        assertTrue(daoSource.contains("dateMillis >= :fromMillis"))
        assertTrue(daoSource.contains("dateMillis <= :toMillis"))
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

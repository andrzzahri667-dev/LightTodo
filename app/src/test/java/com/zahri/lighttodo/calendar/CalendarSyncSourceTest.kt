package com.zahri.lighttodo.integration.calendar

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarSyncSourceTest {

    @Test
    fun orphanCleanupOnlyConsidersUndoneImportedEventsInsideSyncWindow() {
        val syncSource = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/calendar/CalendarSync.kt")
            .readText()
        val daoSource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/Daos.kt")
            .readText()

        assertTrue(syncSource.contains("listUndoneCalendarEventIdsInWindow(from, to)"))
        assertFalse(syncSource.contains("listCalendarEventIds()"))
        assertTrue(daoSource.contains("done = 0"))
        assertTrue(daoSource.contains("dateMillis >= :fromMillis"))
        assertTrue(daoSource.contains("dateMillis <= :toMillis"))
    }

    @Test
    fun orphanCleanupUnlinksAppCreatedTodosInsteadOfDeletingThem() {
        val syncSource = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/calendar/CalendarSync.kt")
            .readText()

        assertTrue(syncSource.contains("val orphanTodos = app.db.todoDao().findByCalendarEventIds(orphanEventIds)"))
        assertTrue(syncSource.contains(".filter { it.calendarCreatedByApp }"))
        assertTrue(syncSource.contains("setCalendarLink(it.id, eventId = null, createdByApp = false)"))
        assertTrue(syncSource.contains(".filterNot { it.calendarCreatedByApp }"))
        assertTrue(syncSource.contains("deleteByIds(it)"))
    }
}

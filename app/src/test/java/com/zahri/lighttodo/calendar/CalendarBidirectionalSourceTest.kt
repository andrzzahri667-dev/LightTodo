package com.zahri.lighttodo.integration.calendar

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarBidirectionalSourceTest {
    @Test
    fun repositoryMirrorsLocalTodoMutationsToCalendarProvider() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/Repository.kt")
            .readText()

        assertTrue(source.contains("CalendarEventWriter.upsertFromTodo"))
        assertTrue(source.contains("CalendarEventWriter.setCompleted"))
        assertTrue(source.contains("CalendarEventWriter.deleteEvent"))
    }

    @Test
    fun repositoryPerformsCalendarProviderIoOnIoDispatcher() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/Repository.kt")
            .readText()

        assertTrue(source.contains("import kotlinx.coroutines.Dispatchers"))
        assertTrue(source.contains("import kotlinx.coroutines.withContext"))
        assertTrue(source.contains("private suspend fun mirrorTodoToCalendar"))
        assertTrue(source.contains("withContext(Dispatchers.IO)"))
    }

    @Test
    fun appCreatedCalendarTodosRemainEditable() {
        val entitySource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/Entities.kt")
            .readText()
        val editSource = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/todoedit/EditViewModel.kt")
            .readText()
        val repositorySource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/Repository.kt")
            .readText()

        assertTrue(entitySource.contains("calendarCreatedByApp"))
        assertTrue(editSource.contains("t.calendarEventId != null && !t.calendarCreatedByApp"))
        assertTrue(repositorySource.contains("calendarCreatedByApp = existing?.calendarCreatedByApp ?: false"))
    }
}

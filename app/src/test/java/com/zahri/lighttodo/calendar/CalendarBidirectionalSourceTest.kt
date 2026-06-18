package com.zahri.lighttodo.calendar

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarBidirectionalSourceTest {
    @Test
    fun todoUseCasesMirrorLocalTodoMutationsToCalendarProvider() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/todo/TodoUseCases.kt")
            .readText()

        assertTrue(source.contains("calendarGateway.upsertFromTodo"))
        assertTrue(source.contains("calendarGateway.setCompleted"))
        assertTrue(source.contains("calendarGateway.deleteEvent"))
        assertFalse(source.contains("CalendarEventWriter."))
    }

    @Test
    fun androidCalendarGatewayPerformsCalendarProviderIoOnIoDispatcher() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/calendar/AndroidCalendarGateway.kt")
            .readText()

        assertTrue(source.contains("import kotlinx.coroutines.Dispatchers"))
        assertTrue(source.contains("import kotlinx.coroutines.withContext"))
        assertTrue(source.contains("CalendarEventWriter.upsertFromTodo"))
        assertTrue(source.contains("CalendarEventWriter.setCompleted"))
        assertTrue(source.contains("CalendarEventWriter.deleteEvent"))
        assertTrue(source.contains("withContext(Dispatchers.IO)"))
    }

    @Test
    fun appCreatedCalendarTodosRemainEditable() {
        val entitySource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/local/Entities.kt")
            .readText()
        val editSource = sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/todo/TodoUseCases.kt")
            .readText()
        val repositorySource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/todo/TodoInputRecordBuilder.kt")
            .readText()

        assertTrue(entitySource.contains("calendarCreatedByApp"))
        assertTrue(editSource.contains("todo.calendarEventId != null && !todo.calendarCreatedByApp"))
        assertTrue(repositorySource.contains("calendarCreatedByApp = existing?.calendarCreatedByApp ?: false"))
    }
}

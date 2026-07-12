package com.zahri.lighttodo.usecase.todo

import com.zahri.lighttodo.domain.todo.TodoInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarEventOwnershipUseCaseTest {

    @Test
    fun deleteDoesNotDeleteExternalCalendarEvent() = runBlocking {
        val fixture = Fixture(TodoRecord(id = 1L, calendarEventId = 101L))

        fixture.deleteTodo.delete(1L)

        assertEquals(emptyList<Long>(), fixture.calendarGateway.deletedEventIds)
    }

    @Test
    fun deleteDeletesCalendarEventCreatedByApp() = runBlocking {
        val fixture = Fixture(
            TodoRecord(id = 1L, calendarEventId = 101L, calendarCreatedByApp = true)
        )

        fixture.deleteTodo.delete(1L)

        assertEquals(listOf(101L), fixture.calendarGateway.deletedEventIds)
    }

    @Test
    fun deleteManyOnlyDeletesCalendarEventsCreatedByApp() = runBlocking {
        val fixture = Fixture(
            TodoRecord(id = 1L, calendarEventId = 101L),
            TodoRecord(id = 2L, calendarEventId = 102L, calendarCreatedByApp = true)
        )

        fixture.deleteTodo.deleteMany(listOf(1L, 2L))

        assertEquals(listOf(102L), fixture.calendarGateway.deletedEventIds)
    }

    @Test
    fun clearDoneOnlyDeletesCalendarEventsCreatedByApp() = runBlocking {
        val fixture = Fixture(
            TodoRecord(id = 1L, done = true, calendarEventId = 101L),
            TodoRecord(
                id = 2L,
                done = true,
                calendarEventId = 102L,
                calendarCreatedByApp = true
            )
        )

        fixture.deleteTodo.clearDone()

        assertEquals(listOf(102L), fixture.calendarGateway.deletedEventIds)
    }

    @Test
    fun saveWithoutDateDoesNotDeleteExternalCalendarEvent() = runBlocking {
        val fixture = Fixture()
        fixture.repository.recordToBuild = TodoRecord(id = 1L, calendarEventId = 101L)

        fixture.saveTodo(TodoInput(id = 1L, title = "External", note = null))

        assertEquals(emptyList<Long>(), fixture.calendarGateway.deletedEventIds)
    }

    @Test
    fun saveWithoutDateDeletesCalendarEventCreatedByApp() = runBlocking {
        val fixture = Fixture()
        fixture.repository.recordToBuild = TodoRecord(
            id = 1L,
            calendarEventId = 101L,
            calendarCreatedByApp = true
        )

        fixture.saveTodo(TodoInput(id = 1L, title = "Owned", note = null))

        assertEquals(listOf(101L), fixture.calendarGateway.deletedEventIds)
    }

    @Test
    fun saveWithDateDoesNotUpdateExternalCalendarEvent() = runBlocking {
        val fixture = Fixture()
        fixture.repository.recordToBuild = TodoRecord(
            id = 1L,
            dateMillis = 1_000L,
            calendarEventId = 101L
        )

        fixture.saveTodo(TodoInput(id = 1L, title = "External", note = null))

        assertEquals(emptyList<Long>(), fixture.calendarGateway.upsertedTodoIds)
    }

    @Test
    fun saveWithDateStillUpdatesCalendarEventCreatedByApp() = runBlocking {
        val fixture = Fixture()
        fixture.repository.recordToBuild = TodoRecord(
            id = 1L,
            dateMillis = 1_000L,
            calendarEventId = 101L,
            calendarCreatedByApp = true
        )

        fixture.saveTodo(TodoInput(id = 1L, title = "Owned", note = null))

        assertEquals(listOf(1L), fixture.calendarGateway.upsertedTodoIds)
    }

    @Test
    fun completeAndReopenDoNotUpdateExternalCalendarEvent() = runBlocking {
        val fixture = Fixture(
            TodoRecord(id = 1L, dateMillis = 1_000L, calendarEventId = 101L)
        )

        fixture.completeTodo(1L, true)
        fixture.completeTodo(1L, false)

        assertEquals(emptyList<Pair<Long, Boolean>>(), fixture.calendarGateway.completedEvents)
        assertEquals(emptyList<Long>(), fixture.calendarGateway.upsertedTodoIds)
    }

    @Test
    fun completeAndReopenStillUpdateCalendarEventCreatedByApp() = runBlocking {
        val fixture = Fixture(
            TodoRecord(
                id = 1L,
                dateMillis = 1_000L,
                calendarEventId = 101L,
                calendarCreatedByApp = true
            )
        )

        fixture.completeTodo(1L, true)
        fixture.completeTodo(1L, false)

        assertEquals(listOf(101L to true), fixture.calendarGateway.completedEvents)
        assertEquals(listOf(1L), fixture.calendarGateway.upsertedTodoIds)
    }

    private class Fixture(vararg todos: TodoRecord) {
        val repository = FakeTodoRepository(todos.toList())
        val reminderGateway = FakeReminderGateway()
        val calendarGateway = FakeCalendarGateway()
        private val widgetUpdater = FakeWidgetUpdater()
        val deleteTodo = DeleteTodoUseCase(
            repository = repository,
            reminderGateway = reminderGateway,
            calendarGateway = calendarGateway,
            widgetUpdater = widgetUpdater
        )
        val saveTodo = SaveTodoUseCase(
            repository = repository,
            reminderGateway = reminderGateway,
            calendarGateway = calendarGateway,
            widgetUpdater = widgetUpdater
        )
        val completeTodo = CompleteTodoUseCase(
            repository = repository,
            reminderGateway = reminderGateway,
            calendarGateway = calendarGateway,
            widgetUpdater = widgetUpdater
        )
    }

    private class FakeTodoRepository(todos: List<TodoRecord>) : TodoRepository {
        private val todos = todos.associateByTo(linkedMapOf()) { it.id }
        var recordToBuild: TodoRecord? = null

        override fun observeHome(): Flow<HomeTodoSnapshot> = throw NotImplementedError()

        override suspend fun prefsSnapshot(): TodoPreferencesSnapshot =
            TodoPreferencesSnapshot(calendarSyncEnabled = true)

        override suspend fun listTags(): List<TodoTag> = emptyList()

        override suspend fun buildTodo(
            input: TodoInput,
            prefsSnapshot: TodoPreferencesSnapshot,
            now: Long
        ): TodoRecord = requireNotNull(recordToBuild)

        override suspend fun upsertTodo(record: TodoRecord, inputId: Long?): TodoRecord {
            val saved = record.copy(id = inputId ?: record.id)
            todos[saved.id] = saved
            return saved
        }

        override suspend fun setDoneLocal(id: Long, done: Boolean, now: Long): TodoRecord? =
            todos[id]?.copy(done = done)?.also { todos[id] = it }

        override suspend fun findTodoById(id: Long): TodoRecord? = todos[id]

        override suspend fun findTodosByIds(ids: List<Long>): List<TodoRecord> =
            ids.mapNotNull(todos::get)

        override suspend fun listAllTodos(): List<TodoRecord> = todos.values.toList()

        override fun listWidgetTodos(nowMillis: Long, limit: Int): List<TodoRecord> =
            todos.values.take(limit)

        override suspend fun listDoneTodos(): List<TodoRecord> = todos.values.filter { it.done }

        override suspend fun listDoneTodoIdsWithReminders(): List<Long> = emptyList()

        override suspend fun setTodoCalendarLink(id: Long, eventId: Long?, createdByApp: Boolean) {
            todos[id] = requireNotNull(todos[id]).copy(
                calendarEventId = eventId,
                calendarCreatedByApp = createdByApp
            )
        }

        override suspend fun deleteLocalTodo(id: Long) {
            todos.remove(id)
        }

        override suspend fun deleteLocalTodos(ids: List<Long>) {
            ids.forEach(todos::remove)
        }

        override suspend fun deleteAllDoneTodos() {
            todos.values.removeAll { it.done }
        }
    }

    private class FakeCalendarGateway : CalendarGateway {
        val deletedEventIds = mutableListOf<Long>()
        val completedEvents = mutableListOf<Pair<Long, Boolean>>()
        val upsertedTodoIds = mutableListOf<Long>()

        override suspend fun <T> withSyncLock(block: suspend () -> T): T = block()

        override suspend fun setCompleted(eventId: Long, done: Boolean) {
            completedEvents += eventId to done
        }

        override suspend fun upsertFromTodo(todo: TodoRecord, accountName: String): Long? {
            upsertedTodoIds += todo.id
            return todo.calendarEventId
        }

        override suspend fun deleteEvent(eventId: Long) {
            deletedEventIds += eventId
        }
    }

    private class FakeReminderGateway : ReminderGateway {
        override fun schedule(todo: TodoRecord, isStart: Boolean) = Unit
        override fun cancel(todoId: Long) = Unit
    }

    private class FakeWidgetUpdater : WidgetUpdater {
        override fun notifyTodosChanged() = Unit
    }
}

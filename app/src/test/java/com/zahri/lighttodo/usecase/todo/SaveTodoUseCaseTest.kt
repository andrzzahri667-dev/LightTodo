package com.zahri.lighttodo.usecase.todo

import com.zahri.lighttodo.domain.todo.TodoInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveTodoUseCaseTest {

    @Test
    fun savePersistsLocalTodoBeforeCreatingCalendarEvent() = runBlocking {
        val repository = FakeTodoRepository()
        val reminderGateway = FakeReminderGateway()
        val calendarGateway = FakeCalendarGateway(repository, reminderGateway)
        val widgetUpdater = FakeWidgetUpdater()
        val useCase = SaveTodoUseCase(
            repository = repository,
            reminderGateway = reminderGateway,
            calendarGateway = calendarGateway,
            widgetUpdater = widgetUpdater
        )

        val savedId = useCase(
            TodoInput(
                title = "去吃",
                note = null,
                year = 2026,
                month = 6,
                day = 18,
                startHour = 16,
                startMinute = 30,
                deadlineHour = 17,
                deadlineMinute = 30
            )
        )

        assertEquals(FakeTodoRepository.LocalId, savedId)
        assertEquals(1, repository.upsertedBeforeCalendarCreate)
        assertEquals(FakeTodoRepository.LocalId, calendarGateway.createdTodoId)
        assertEquals(
            listOf(FakeTodoRepository.LocalId to true, FakeTodoRepository.LocalId to false),
            calendarGateway.scheduledBeforeCalendarCreate
        )
        assertEquals(FakeCalendarGateway.EventId, repository.linkedEventId)
        assertTrue(repository.linkedCreatedByApp)
        assertEquals(listOf(FakeTodoRepository.LocalId), reminderGateway.cancelled)
        assertEquals(
            listOf(FakeTodoRepository.LocalId to true, FakeTodoRepository.LocalId to false),
            reminderGateway.scheduled
        )
        assertEquals(1, widgetUpdater.notifyCount)
    }

    private class FakeTodoRepository : TodoRepository {
        var upsertCount = 0
        var upsertedBeforeCalendarCreate = 0
        var linkedEventId: Long? = null
        var linkedCreatedByApp: Boolean = false

        override suspend fun prefsSnapshot(): TodoPreferencesSnapshot =
            TodoPreferencesSnapshot(calendarSyncEnabled = true)

        override suspend fun buildTodo(
            input: TodoInput,
            prefsSnapshot: TodoPreferencesSnapshot,
            now: Long
        ): TodoRecord =
            TodoRecord(
                title = input.title,
                note = input.note,
                date = 20260618,
                dateMillis = 1781712000000L,
                startHour = input.startHour,
                startMinute = input.startMinute,
                deadlineHour = input.deadlineHour,
                deadlineMinute = input.deadlineMinute,
                remindStartAtMillis = FutureStartReminder,
                remindAtMillis = FutureEndReminder,
                createdAtMillis = now
            )

        override suspend fun upsertTodo(record: TodoRecord, inputId: Long?): TodoRecord {
            upsertCount += 1
            return record.copy(id = inputId ?: LocalId)
        }

        override suspend fun setTodoCalendarLink(id: Long, eventId: Long?, createdByApp: Boolean) {
            linkedEventId = eventId
            linkedCreatedByApp = createdByApp
        }

        override fun observeHome(): Flow<HomeTodoSnapshot> = throw NotImplementedError()
        override suspend fun listTags(): List<TodoTag> = throw NotImplementedError()
        override suspend fun setDoneLocal(id: Long, done: Boolean, now: Long): TodoRecord? = throw NotImplementedError()
        override suspend fun findTodoById(id: Long): TodoRecord? = throw NotImplementedError()
        override suspend fun findTodosByIds(ids: List<Long>): List<TodoRecord> = throw NotImplementedError()
        override suspend fun listAllTodos(): List<TodoRecord> = throw NotImplementedError()
        override fun listWidgetTodos(nowMillis: Long, limit: Int): List<TodoRecord> = throw NotImplementedError()
        override suspend fun listDoneTodos(): List<TodoRecord> = throw NotImplementedError()
        override suspend fun listDoneTodoIdsWithReminders(): List<Long> = throw NotImplementedError()
        override suspend fun deleteLocalTodo(id: Long) = throw NotImplementedError()
        override suspend fun deleteLocalTodos(ids: List<Long>) = throw NotImplementedError()
        override suspend fun deleteAllDoneTodos() = throw NotImplementedError()

        companion object {
            const val LocalId = 101L
            const val FutureStartReminder = 9_999_999_990_000L
            const val FutureEndReminder = 9_999_999_999_000L
        }
    }

    private class FakeCalendarGateway(
        private val repository: FakeTodoRepository,
        private val reminderGateway: FakeReminderGateway
    ) : CalendarGateway {
        var createdTodoId: Long? = null
        val scheduledBeforeCalendarCreate = mutableListOf<Pair<Long, Boolean>>()

        override suspend fun <T> withSyncLock(block: suspend () -> T): T = block()

        override suspend fun upsertFromTodo(todo: TodoRecord, accountName: String): Long? {
            repository.upsertedBeforeCalendarCreate = repository.upsertCount
            createdTodoId = todo.id
            scheduledBeforeCalendarCreate += reminderGateway.scheduled
            return EventId
        }

        override suspend fun setCompleted(eventId: Long, done: Boolean) = Unit
        override suspend fun deleteEvent(eventId: Long) = Unit

        companion object {
            const val EventId = 2029L
        }
    }

    private class FakeReminderGateway : ReminderGateway {
        val cancelled = mutableListOf<Long>()
        val scheduled = mutableListOf<Pair<Long, Boolean>>()

        override fun cancel(todoId: Long) {
            cancelled += todoId
        }

        override fun schedule(todo: TodoRecord, isStart: Boolean) {
            scheduled += todo.id to isStart
        }
    }

    private class FakeWidgetUpdater : WidgetUpdater {
        var notifyCount = 0

        override fun notifyTodosChanged() {
            notifyCount += 1
        }
    }
}

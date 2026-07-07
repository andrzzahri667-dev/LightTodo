package com.zahri.lighttodo.calendar

import com.zahri.lighttodo.usecase.calendar.CalendarProviderEvent
import com.zahri.lighttodo.usecase.calendar.CalendarProviderEvents
import com.zahri.lighttodo.usecase.calendar.CalendarSyncGateway
import com.zahri.lighttodo.usecase.calendar.CalendarSyncPreferences
import com.zahri.lighttodo.usecase.calendar.CalendarSyncPreferencesRepository
import com.zahri.lighttodo.usecase.calendar.CalendarSyncRepository
import com.zahri.lighttodo.usecase.calendar.SyncCalendarUseCase
import com.zahri.lighttodo.usecase.todo.ReminderGateway
import com.zahri.lighttodo.usecase.todo.TodoRecord
import com.zahri.lighttodo.usecase.todo.WidgetUpdater
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarSyncReminderPreservationTest {

    @Test
    fun syncPreservesExistingReminderFieldsForAppCreatedCalendarTodos() = runBlocking {
        val eventId = 100L
        val existing = TodoRecord(
            id = 7L,
            title = "去吃",
            date = 20260618,
            dateMillis = 1781712000000L,
            startHour = 15,
            startMinute = 23,
            deadlineHour = 16,
            deadlineMinute = 23,
            remindStartAtMillis = 1781767380000L,
            remindAtMillis = 1781770980000L,
            customRemindHoursBefore = 0,
            createdAtMillis = 1781767276400L,
            calendarEventId = eventId,
            calendarCreatedByApp = true
        )
        val repository = FakeCalendarSyncRepository(existing)
        val useCase = SyncCalendarUseCase(
            prefs = FakeCalendarSyncPreferencesRepository(),
            repository = repository,
            calendarSyncGateway = FakeCalendarSyncGateway(
                CalendarProviderEvent(
                    id = eventId,
                    title = "去吃",
                    description = null,
                    startMillis = 1781767380000L,
                    endMillis = 1781770980000L,
                    allDay = false,
                    canceled = false
                )
            ),
            widgetUpdater = FakeWidgetUpdater(),
            reminderGateway = FakeReminderGateway()
        )

        useCase(force = true, noTitleFallback = "无标题")

        val synced = repository.upserted.single()
        assertEquals(existing.id, synced.id)
        assertEquals(existing.remindStartAtMillis, synced.remindStartAtMillis)
        assertEquals(existing.remindAtMillis, synced.remindAtMillis)
        assertEquals(existing.customRemindHoursBefore, synced.customRemindHoursBefore)
        assertTrue(synced.calendarCreatedByApp)
    }

    @Test
    fun syncKeepsUnchangedFutureReminderScheduledWhenOtherReminderTimeChanges() = runBlocking {
        val eventId = 101L
        val existing = TodoRecord(
            id = 8L,
            title = "测试",
            date = 20990101,
            dateMillis = FutureStart,
            startHour = 9,
            startMinute = 0,
            deadlineHour = 10,
            deadlineMinute = 0,
            remindStartAtMillis = FutureStart,
            remindAtMillis = OldFutureEnd,
            createdAtMillis = 1781767276400L,
            calendarEventId = eventId,
            calendarCreatedByApp = true
        )
        val reminderGateway = FakeReminderGateway()
        val repository = FakeCalendarSyncRepository(existing)
        val useCase = SyncCalendarUseCase(
            prefs = FakeCalendarSyncPreferencesRepository(),
            repository = repository,
            calendarSyncGateway = FakeCalendarSyncGateway(
                CalendarProviderEvent(
                    id = eventId,
                    title = "测试",
                    description = null,
                    startMillis = FutureStart,
                    endMillis = NewFutureEnd,
                    allDay = false,
                    canceled = false
                )
            ),
            widgetUpdater = FakeWidgetUpdater(),
            reminderGateway = reminderGateway
        )

        useCase(force = true, noTitleFallback = "无标题")

        assertEquals(listOf(existing.id), reminderGateway.cancelled)
        assertEquals(
            listOf(existing.id to true, existing.id to false),
            reminderGateway.scheduled
        )
    }

    @Test
    fun syncSchedulesFutureRemindersForNewImportedCalendarEvent() = runBlocking {
        val eventId = 102L
        val reminderGateway = FakeReminderGateway()
        val repository = FakeCalendarSyncRepository(existing = null, generatedId = 60L)
        val useCase = SyncCalendarUseCase(
            prefs = FakeCalendarSyncPreferencesRepository(),
            repository = repository,
            calendarSyncGateway = FakeCalendarSyncGateway(
                CalendarProviderEvent(
                    id = eventId,
                    title = "出去吃",
                    description = null,
                    startMillis = FutureStart,
                    endMillis = NewFutureEnd,
                    allDay = false,
                    canceled = false
                )
            ),
            widgetUpdater = FakeWidgetUpdater(),
            reminderGateway = reminderGateway
        )

        useCase(force = true, noTitleFallback = "无标题")

        assertEquals(emptyList<Long>(), reminderGateway.cancelled)
        assertEquals(
            listOf(60L to true, 60L to false),
            reminderGateway.scheduled
        )
    }

    @Test
    fun syncRefreshesExistingFutureRemindersWithoutCancellingWhenTimesAreUnchanged() = runBlocking {
        val eventId = 103L
        val existing = TodoRecord(
            id = 61L,
            title = "出去吃",
            date = 20990101,
            dateMillis = FutureStart,
            startHour = 9,
            startMinute = 0,
            deadlineHour = 10,
            deadlineMinute = 0,
            remindStartAtMillis = FutureStart,
            remindAtMillis = NewFutureEnd,
            createdAtMillis = 1781833022000L,
            calendarEventId = eventId,
            calendarCreatedByApp = false
        )
        val reminderGateway = FakeReminderGateway()
        val repository = FakeCalendarSyncRepository(existing = existing)
        val useCase = SyncCalendarUseCase(
            prefs = FakeCalendarSyncPreferencesRepository(),
            repository = repository,
            calendarSyncGateway = FakeCalendarSyncGateway(
                CalendarProviderEvent(
                    id = eventId,
                    title = "出去吃",
                    description = null,
                    startMillis = FutureStart,
                    endMillis = NewFutureEnd,
                    allDay = false,
                    canceled = false
                )
            ),
            widgetUpdater = FakeWidgetUpdater(),
            reminderGateway = reminderGateway
        )

        useCase(force = true, noTitleFallback = "无标题")

        assertEquals(emptyList<Long>(), reminderGateway.cancelled)
        assertEquals(
            listOf(existing.id to true, existing.id to false),
            reminderGateway.scheduled
        )
    }

    private class FakeCalendarSyncPreferencesRepository : CalendarSyncPreferencesRepository {
        override suspend fun calendarSyncPreferences(): CalendarSyncPreferences =
            CalendarSyncPreferences(calendarSyncEnabled = true)
    }

    private class FakeCalendarSyncGateway(
        private val event: CalendarProviderEvent
    ) : CalendarSyncGateway {
        override fun hasReadCalendarPermission(): Boolean = true

        override fun queryEvents(
            userFilter: String,
            fromMillis: Long,
            toMillis: Long
        ): CalendarProviderEvents =
            CalendarProviderEvents(hasCalendars = true, events = listOf(event))
    }

    private class FakeCalendarSyncRepository(
        private val existing: TodoRecord?,
        private val generatedId: Long = existing?.id ?: 1L
    ) : CalendarSyncRepository {
        val upserted = mutableListOf<TodoRecord>()

        override suspend fun listUndoneCalendarEventIdsInWindow(
            fromMillis: Long,
            toMillis: Long
        ): List<Long> = existing?.calendarEventId?.let { listOf(it) } ?: emptyList()

        override suspend fun findTodosByCalendarEventIds(eventIds: List<Long>): List<TodoRecord> =
            if (existing?.calendarEventId in eventIds) listOf(requireNotNull(existing)) else emptyList()

        override suspend fun upsertTodos(todos: List<TodoRecord>): List<TodoRecord> {
            upserted += todos
            return todos.map { todo ->
                if (todo.id == 0L) todo.copy(id = generatedId) else todo
            }
        }

        override suspend fun setTodoCalendarLink(id: Long, eventId: Long?, createdByApp: Boolean) = Unit

        override suspend fun deleteLocalTodos(ids: List<Long>) = Unit
    }

    private class FakeWidgetUpdater : WidgetUpdater {
        override fun notifyTodosChanged() = Unit
    }

    private class FakeReminderGateway : ReminderGateway {
        val cancelled = mutableListOf<Long>()
        val scheduled = mutableListOf<Pair<Long, Boolean>>()

        override fun schedule(todo: TodoRecord, isStart: Boolean) {
            scheduled += todo.id to isStart
        }

        override fun cancel(todoId: Long) {
            cancelled += todoId
        }
    }

    private companion object {
        const val FutureStart = 4_071_758_400_000L
        const val OldFutureEnd = 4_071_762_000_000L
        const val NewFutureEnd = 4_071_765_600_000L
    }
}

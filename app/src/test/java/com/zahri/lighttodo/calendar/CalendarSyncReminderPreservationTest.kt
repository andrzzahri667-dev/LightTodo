package com.zahri.lighttodo.calendar

import com.zahri.lighttodo.usecase.calendar.CalendarProviderEvent
import com.zahri.lighttodo.usecase.calendar.CalendarProviderEvents
import com.zahri.lighttodo.usecase.calendar.CalendarSyncGateway
import com.zahri.lighttodo.usecase.calendar.CalendarSyncPreferences
import com.zahri.lighttodo.usecase.calendar.CalendarSyncPreferencesRepository
import com.zahri.lighttodo.usecase.calendar.CalendarSyncRepository
import com.zahri.lighttodo.usecase.calendar.CalendarSyncWindow
import com.zahri.lighttodo.usecase.calendar.SyncCalendarUseCase
import com.zahri.lighttodo.usecase.todo.ReminderGateway
import com.zahri.lighttodo.usecase.todo.TodoRecord
import com.zahri.lighttodo.usecase.todo.WidgetUpdater
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarSyncReminderPreservationTest {

    @Test
    fun syncPreservesExistingReminderFieldsForAppCreatedCalendarTodos() = runBlocking {
        val eventId = 100L
        val times = futureTimes()
        val existing = TodoRecord(
            id = 7L,
            title = "去吃",
            date = times.dayKey,
            dateMillis = times.startOfDayMillis,
            startHour = 15,
            startMinute = 23,
            deadlineHour = 16,
            deadlineMinute = 23,
            remindStartAtMillis = times.startMillis,
            remindAtMillis = times.oldEndMillis,
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
                    startMillis = times.startMillis,
                    endMillis = times.oldEndMillis,
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
        val times = futureTimes()
        val existing = TodoRecord(
            id = 8L,
            title = "测试",
            date = times.dayKey,
            dateMillis = times.startOfDayMillis,
            startHour = 9,
            startMinute = 0,
            deadlineHour = 10,
            deadlineMinute = 0,
            remindStartAtMillis = times.startMillis,
            remindAtMillis = times.oldEndMillis,
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
                    startMillis = times.startMillis,
                    endMillis = times.newEndMillis,
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
        val times = futureTimes()
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
                    startMillis = times.startMillis,
                    endMillis = times.newEndMillis,
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
        val times = futureTimes()
        val existing = TodoRecord(
            id = 61L,
            title = "出去吃",
            date = times.dayKey,
            dateMillis = times.startOfDayMillis,
            startHour = 9,
            startMinute = 0,
            deadlineHour = 10,
            deadlineMinute = 0,
            remindStartAtMillis = times.startMillis,
            remindAtMillis = times.newEndMillis,
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
                    startMillis = times.startMillis,
                    endMillis = times.newEndMillis,
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
            window: CalendarSyncWindow
        ): CalendarProviderEvents =
            CalendarProviderEvents(hasCalendars = true, events = listOf(event))
    }

    private class FakeCalendarSyncRepository(
        private val existing: TodoRecord?,
        private val generatedId: Long = existing?.id ?: 1L
    ) : CalendarSyncRepository {
        val upserted = mutableListOf<TodoRecord>()

        override suspend fun listUndoneCalendarEventIdsInDateRange(
            fromDayKey: Int,
            toDayKey: Int
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

    private fun futureTimes(): FutureTimes {
        val zoneId = ZoneId.systemDefault()
        val date = LocalDate.now(zoneId).plusDays(1)
        return FutureTimes(
            dayKey = date.year * 10_000 + date.monthValue * 100 + date.dayOfMonth,
            startOfDayMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            startMillis = date.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli(),
            oldEndMillis = date.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli(),
            newEndMillis = date.atTime(11, 0).atZone(zoneId).toInstant().toEpochMilli()
        )
    }

    private data class FutureTimes(
        val dayKey: Int,
        val startOfDayMillis: Long,
        val startMillis: Long,
        val oldEndMillis: Long,
        val newEndMillis: Long
    )
}

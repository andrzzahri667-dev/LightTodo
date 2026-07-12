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
import java.time.ZoneOffset
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarSyncDateSemanticsTest {
    @Test
    fun syncImportsLosAngelesAllDayEventOnItsUtcDateWithoutDeletingExistingTodo() =
        withDefaultTimeZone(LosAngelesZone.id) {
            runBlocking {
                val date = LocalDate.now(LosAngelesZone)
                val eventId = 401L
                val event = allDayEvent(eventId, date)
                val existing = TodoRecord(
                    id = 41L,
                    title = "All day",
                    date = dayKey(date),
                    dateMillis = date.atStartOfDay(LosAngelesZone).toInstant().toEpochMilli(),
                    calendarEventId = eventId,
                    calendarCreatedByApp = false
                )
                val repository = RecordingCalendarSyncRepository(existing)
                val useCase = useCase(
                    repository = repository,
                    gateway = FilteringCalendarSyncGateway(listOf(event))
                )

                val count = useCase(force = true, noTitleFallback = "No title")

                assertEquals(1, count)
                assertTrue(repository.deletedIds.isEmpty())
                val synced = repository.upserted.single()
                assertEquals(dayKey(date), synced.date)
                assertEquals(
                    date.atStartOfDay(LosAngelesZone).toInstant().toEpochMilli(),
                    synced.dateMillis
                )
            }
        }

    @Test
    fun syncDoesNotImportAllDayEventImmediatelyAfterTargetDateWindow() =
        withDefaultTimeZone(LosAngelesZone.id) {
            runBlocking {
                val firstDate = LocalDate.now(LosAngelesZone)
                val outsideDate = firstDate.plusDays(61)
                val repository = RecordingCalendarSyncRepository(existing = null)
                val useCase = useCase(
                    repository = repository,
                    gateway = FilteringCalendarSyncGateway(
                        listOf(allDayEvent(id = 402L, date = outsideDate))
                    )
                )

                val count = useCase(force = true, noTitleFallback = "No title")

                assertEquals(0, count)
                assertTrue(repository.upserted.isEmpty())
            }
        }

    private fun useCase(
        repository: RecordingCalendarSyncRepository,
        gateway: CalendarSyncGateway
    ): SyncCalendarUseCase =
        SyncCalendarUseCase(
            prefs = EnabledCalendarSyncPreferences,
            repository = repository,
            calendarSyncGateway = gateway,
            widgetUpdater = NoopWidgetUpdater,
            reminderGateway = NoopReminderGateway
        )

    private fun allDayEvent(id: Long, date: LocalDate): CalendarProviderEvent =
        CalendarProviderEvent(
            id = id,
            title = "All day",
            description = null,
            startMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            endMillis = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            allDay = true,
            canceled = false
        )

    private fun dayKey(date: LocalDate): Int =
        date.year * 10_000 + date.monthValue * 100 + date.dayOfMonth

    private inline fun <T> withDefaultTimeZone(id: String, block: () -> T): T {
        val previous = TimeZone.getDefault()
        return try {
            TimeZone.setDefault(TimeZone.getTimeZone(id))
            block()
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    private object EnabledCalendarSyncPreferences : CalendarSyncPreferencesRepository {
        override suspend fun calendarSyncPreferences(): CalendarSyncPreferences =
            CalendarSyncPreferences(calendarSyncEnabled = true)
    }

    private class FilteringCalendarSyncGateway(
        private val events: List<CalendarProviderEvent>
    ) : CalendarSyncGateway {
        override fun hasReadCalendarPermission(): Boolean = true

        override fun queryEvents(
            userFilter: String,
            window: CalendarSyncWindow
        ): CalendarProviderEvents =
            CalendarProviderEvents(
                hasCalendars = true,
                events = events.filter { event ->
                    if (event.allDay) {
                        event.startMillis >= window.allDayFromMillis &&
                            event.startMillis < window.allDayToExclusiveMillis
                    } else {
                        event.startMillis >= window.timedFromMillis &&
                            event.startMillis < window.timedToExclusiveMillis
                    }
                }
            )
    }

    private class RecordingCalendarSyncRepository(
        private val existing: TodoRecord?
    ) : CalendarSyncRepository {
        val upserted = mutableListOf<TodoRecord>()
        val deletedIds = mutableListOf<Long>()

        override suspend fun listUndoneCalendarEventIdsInDateRange(
            fromDayKey: Int,
            toDayKey: Int
        ): List<Long> = existing?.calendarEventId?.let(::listOf).orEmpty()

        override suspend fun findTodosByCalendarEventIds(eventIds: List<Long>): List<TodoRecord> =
            existing?.takeIf { it.calendarEventId in eventIds }?.let(::listOf).orEmpty()

        override suspend fun upsertTodos(todos: List<TodoRecord>): List<TodoRecord> {
            upserted += todos
            return todos.map { todo -> if (todo.id == 0L) todo.copy(id = 99L) else todo }
        }

        override suspend fun setTodoCalendarLink(id: Long, eventId: Long?, createdByApp: Boolean) = Unit

        override suspend fun deleteLocalTodos(ids: List<Long>) {
            deletedIds += ids
        }
    }

    private object NoopWidgetUpdater : WidgetUpdater {
        override fun notifyTodosChanged() = Unit
    }

    private object NoopReminderGateway : ReminderGateway {
        override fun schedule(todo: TodoRecord, isStart: Boolean) = Unit
        override fun cancel(todoId: Long) = Unit
    }

    private companion object {
        val LosAngelesZone: ZoneId = ZoneId.of("America/Los_Angeles")
    }
}

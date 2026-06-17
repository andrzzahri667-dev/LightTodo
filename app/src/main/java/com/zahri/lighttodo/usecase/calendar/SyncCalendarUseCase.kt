package com.zahri.lighttodo.usecase.calendar

import com.zahri.lighttodo.domain.calendar.CalendarSyncPolicy
import com.zahri.lighttodo.domain.todo.TodoDateFields
import com.zahri.lighttodo.usecase.todo.TodoRecord
import com.zahri.lighttodo.usecase.todo.WidgetUpdater
import com.zahri.lighttodo.util.DateUtils
import java.time.LocalDate
import java.util.Calendar

interface CalendarSyncGateway {
    fun hasReadCalendarPermission(): Boolean
    fun queryEvents(userFilter: String, fromMillis: Long, toMillis: Long): CalendarProviderEvents
}

interface CalendarSyncRepository {
    suspend fun listUndoneCalendarEventIdsInWindow(fromMillis: Long, toMillis: Long): List<Long>
    suspend fun findTodosByCalendarEventIds(eventIds: List<Long>): List<TodoRecord>
    suspend fun upsertTodos(todos: List<TodoRecord>)
    suspend fun setTodoCalendarLink(id: Long, eventId: Long?, createdByApp: Boolean)
    suspend fun deleteLocalTodos(ids: List<Long>)
}

interface CalendarSyncPreferencesRepository {
    suspend fun calendarSyncPreferences(): CalendarSyncPreferences
}

data class CalendarSyncPreferences(
    val calendarSyncEnabled: Boolean = false,
    val calendarAccountName: String = ""
)

data class CalendarProviderEvents(
    val hasCalendars: Boolean,
    val events: List<CalendarProviderEvent>
)

data class CalendarProviderEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val startMillis: Long,
    val endMillis: Long?,
    val allDay: Boolean,
    val canceled: Boolean
)

class SyncCalendarUseCase(
    private val prefs: CalendarSyncPreferencesRepository,
    private val repository: CalendarSyncRepository,
    private val calendarSyncGateway: CalendarSyncGateway,
    private val widgetUpdater: WidgetUpdater
) {
    suspend operator fun invoke(force: Boolean, noTitleFallback: String): Int {
        val prefsSnapshot = prefs.calendarSyncPreferences()
        if (!force && !prefsSnapshot.calendarSyncEnabled) return -1
        if (!calendarSyncGateway.hasReadCalendarPermission()) return -1

        val now = LocalDate.now()
        val from = DateUtils.startOfDayMillis(now)
        val to = DateUtils.endOfDayMillis(now.plusDays(60))
        val providerEvents = calendarSyncGateway.queryEvents(
            userFilter = prefsSnapshot.calendarAccountName,
            fromMillis = from,
            toMillis = to
        )
        if (!providerEvents.hasCalendars) return 0

        val events = providerEvents.events
        val eventIds = events.map { it.id }
        val orphanEventIds = CalendarSyncPolicy.orphanEventIds(
            importedEventIds = repository.listUndoneCalendarEventIdsInWindow(from, to),
            providerEventIds = eventIds
        )
        unlinkOrDeleteOrphanTodos(orphanEventIds)

        val existingEntities = if (eventIds.isEmpty()) {
            emptyList()
        } else {
            repository.findTodosByCalendarEventIds(eventIds)
        }
        val existingMap = existingEntities.associateBy { it.calendarEventId }
        val nowMillis = System.currentTimeMillis()

        val toUpsert = events.map { event ->
            event.toTodoRecord(
                existing = existingMap[event.id],
                nowMillis = nowMillis,
                noTitleFallback = noTitleFallback
            )
        }
        if (toUpsert.isNotEmpty()) {
            repository.upsertTodos(toUpsert)
        }

        if (toUpsert.isNotEmpty() || orphanEventIds.isNotEmpty()) {
            widgetUpdater.notifyTodosChanged()
        }
        return toUpsert.size
    }

    private suspend fun unlinkOrDeleteOrphanTodos(orphanEventIds: List<Long>) {
        if (orphanEventIds.isEmpty()) return
        val orphanTodos = repository.findTodosByCalendarEventIds(orphanEventIds)
        orphanTodos
            .filter { it.calendarCreatedByApp }
            .forEach {
                repository.setTodoCalendarLink(
                    id = it.id,
                    eventId = null,
                    createdByApp = false
                )
            }
        orphanTodos
            .filterNot { it.calendarCreatedByApp }
            .map { it.id }
            .takeIf { it.isNotEmpty() }
            ?.let { repository.deleteLocalTodos(it) }
    }

    private fun CalendarProviderEvent.toTodoRecord(
        existing: TodoRecord?,
        nowMillis: Long,
        noTitleFallback: String
    ): TodoRecord {
        val dateFields = TodoDateFields.fromEpochMillis(startMillis)
        val (startH, startM) = if (allDay) null to null else hourMinuteOf(startMillis)
        val (endH, endM) = if (allDay || endMillis == null) null to null else hourMinuteOf(endMillis)
        val doneMerge = CalendarSyncPolicy.mergeDoneState(
            providerCanceled = canceled,
            existingDone = existing?.done,
            existingDoneAtMillis = existing?.doneAtMillis,
            nowMillis = nowMillis
        )
        return TodoRecord(
            id = existing?.id ?: 0L,
            title = title.ifBlank { noTitleFallback },
            note = description?.takeIf { it.isNotBlank() },
            date = dateFields.date,
            dateMillis = dateFields.dateMillis,
            startHour = startH,
            startMinute = startM,
            deadlineHour = endH ?: startH,
            deadlineMinute = endM ?: startM,
            remindStartAtMillis = null,
            remindAtMillis = null,
            customRemindHoursBefore = null,
            tagId = existing?.tagId,
            done = doneMerge.done,
            doneAtMillis = doneMerge.doneAtMillis,
            createdAtMillis = existing?.createdAtMillis ?: nowMillis,
            calendarEventId = id,
            calendarCreatedByApp = existing?.calendarCreatedByApp ?: false
        )
    }

    private fun hourMinuteOf(millis: Long): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
    }
}

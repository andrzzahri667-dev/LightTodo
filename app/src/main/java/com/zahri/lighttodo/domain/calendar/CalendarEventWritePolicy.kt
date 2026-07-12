package com.zahri.lighttodo.domain.calendar

import com.zahri.lighttodo.util.DateUtils
import java.time.ZoneId

internal data class CalendarEventDraft(
    val title: String,
    val description: String?,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val timezoneId: String,
    val completed: Boolean
)

data class CalendarEventTodo(
    val title: String?,
    val note: String?,
    val date: Int?,
    val startHour: Int?,
    val startMinute: Int?,
    val deadlineHour: Int?,
    val deadlineMinute: Int?,
    val done: Boolean
)

internal object CalendarEventWritePolicy {
    private const val MinimumTimedDurationMillis = 15 * 60_000L
    private val Utc = ZoneId.of("UTC")

    fun draftFor(
        todo: CalendarEventTodo,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): CalendarEventDraft? {
        val dayKey = todo.date ?: return null
        val date = DateUtils.fromDayKey(dayKey)
        val allDay = todo.startHour == null || todo.startMinute == null ||
            todo.deadlineHour == null || todo.deadlineMinute == null
        val startMillis = if (allDay) {
            date.atStartOfDay(Utc).toInstant().toEpochMilli()
        } else {
            DateUtils.timeOnDayMillis(
                dayKey,
                requireNotNull(todo.startHour),
                requireNotNull(todo.startMinute),
                zoneId
            )
        }
        val endMillis = if (allDay) {
            date.plusDays(1).atStartOfDay(Utc).toInstant().toEpochMilli()
        } else {
            val rawEnd = DateUtils.timeOnDayMillis(
                dayKey,
                requireNotNull(todo.deadlineHour),
                requireNotNull(todo.deadlineMinute),
                zoneId
            )
            if (rawEnd > startMillis) rawEnd else startMillis + MinimumTimedDurationMillis
        }
        return CalendarEventDraft(
            title = todo.title?.takeIf { it.isNotBlank() }.orEmpty(),
            description = todo.note?.takeIf { it.isNotBlank() },
            startMillis = startMillis,
            endMillis = endMillis,
            allDay = allDay,
            timezoneId = if (allDay) Utc.id else zoneId.id,
            completed = todo.done
        )
    }
}

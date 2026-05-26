package com.zahri.lighttodo.calendar

import com.zahri.lighttodo.data.TodoEntity

internal data class CalendarEventDraft(
    val title: String,
    val description: String?,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val completed: Boolean
)

internal object CalendarEventWritePolicy {
    const val DayMillis = 86_400_000L
    private const val MinimumTimedDurationMillis = 15 * 60_000L

    fun draftFor(todo: TodoEntity): CalendarEventDraft? {
        val dateMillis = todo.dateMillis ?: return null
        val allDay = todo.startHour == null || todo.startMinute == null ||
            todo.deadlineHour == null || todo.deadlineMinute == null
        val startMillis = if (allDay) {
            dateMillis
        } else {
            millisAt(dateMillis, todo.startHour, todo.startMinute)
        }
        val endMillis = if (allDay) {
            dateMillis + DayMillis
        } else {
            val rawEnd = millisAt(dateMillis, todo.deadlineHour, todo.deadlineMinute)
            if (rawEnd > startMillis) rawEnd else startMillis + MinimumTimedDurationMillis
        }
        return CalendarEventDraft(
            title = todo.title?.takeIf { it.isNotBlank() }.orEmpty(),
            description = todo.note?.takeIf { it.isNotBlank() },
            startMillis = startMillis,
            endMillis = endMillis,
            allDay = allDay,
            completed = todo.done
        )
    }

    private fun millisAt(dateMillis: Long, hour: Int?, minute: Int?): Long {
        val h = requireNotNull(hour)
        val m = requireNotNull(minute)
        return dateMillis + h * 3_600_000L + m * 60_000L
    }
}

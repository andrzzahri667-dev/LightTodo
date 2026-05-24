package com.zahri.lighttodo.widget

import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.util.DateUtils
import java.time.LocalDateTime

object TodoWidgetDisplayPolicy {
    data class Clock(
        val todayKey: Int,
        val minuteOfDay: Int
    )

    fun clockAt(now: LocalDateTime = DateUtils.nowDateTime()): Clock =
        Clock(
            todayKey = DateUtils.toDayKey(now.toLocalDate()),
            minuteOfDay = now.hour * 60 + now.minute
        )

    fun isSubtitleOverdue(item: TodoEntity, now: LocalDateTime): Boolean =
        isSubtitleOverdue(item, clockAt(now))

    fun isSubtitleOverdue(item: TodoEntity, clock: Clock = clockAt()): Boolean {
        if (item.done) return false
        val itemDate = item.date ?: return false
        val timeRange = item.timeRange() ?: return false

        if (itemDate < clock.todayKey) return true
        if (itemDate > clock.todayKey) return false

        return timeRange.endMinuteOfDay < clock.minuteOfDay
    }

    fun deadlineSuffix(item: TodoEntity): String =
        item.timeRange()?.let { range ->
            " %02d:%02d-%02d:%02d".format(range.startHour, range.startMinute, range.endHour, range.endMinute)
        }.orEmpty()

    private fun TodoEntity.timeRange(): TimeRange? {
        val startHour = startHour ?: return null
        val startMinute = startMinute ?: return null
        val endHour = deadlineHour ?: return null
        val endMinute = deadlineMinute ?: return null
        return TimeRange(startHour, startMinute, endHour, endMinute)
    }

    private data class TimeRange(
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int
    ) {
        val endMinuteOfDay: Int = endHour * 60 + endMinute
    }
}

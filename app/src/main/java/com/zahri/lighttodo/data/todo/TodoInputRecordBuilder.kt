package com.zahri.lighttodo.data.todo

import com.zahri.lighttodo.data.local.TagDao
import com.zahri.lighttodo.data.local.TagEntity
import com.zahri.lighttodo.data.local.TodoDao
import com.zahri.lighttodo.domain.todo.TodoDateFields
import com.zahri.lighttodo.domain.todo.TodoInput
import com.zahri.lighttodo.domain.todo.TodoReminderDefaults
import com.zahri.lighttodo.usecase.todo.TodoPreferencesSnapshot
import com.zahri.lighttodo.usecase.todo.TodoRecord
import com.zahri.lighttodo.util.DateUtils

internal class TodoInputRecordBuilder(
    private val todoDao: TodoDao,
    private val tagDao: TagDao
) {
    suspend fun build(
        input: TodoInput,
        prefsSnapshot: TodoPreferencesSnapshot,
        now: Long
    ): TodoRecord {
        val tagId = resolveTagId(input.tagName)
        val dateFields = TodoDateFields.fromParts(input.year, input.month, input.day)
        val timeFields = if (dateFields is TodoDateFields.Dated) {
            datedTimeFields(input, dateFields, prefsSnapshot)
        } else {
            TodoTimeFields()
        }
        val existing = input.id?.let { todoDao.findById(it) }
        return TodoRecord(
            id = input.id ?: 0L,
            title = input.title?.takeIf { it.isNotBlank() },
            note = input.note?.takeIf { it.isNotBlank() },
            date = dateFields.date,
            dateMillis = dateFields.dateMillis,
            startHour = timeFields.startHour,
            startMinute = timeFields.startMinute,
            deadlineHour = timeFields.deadlineHour,
            deadlineMinute = timeFields.deadlineMinute,
            remindStartAtMillis = timeFields.remindStartAtMillis,
            remindAtMillis = timeFields.remindAtMillis,
            customRemindHoursBefore = timeFields.customHoursBefore,
            tagId = tagId,
            done = existing?.done ?: false,
            doneAtMillis = existing?.doneAtMillis,
            createdAtMillis = existing?.createdAtMillis ?: now,
            calendarEventId = existing?.calendarEventId,
            calendarCreatedByApp = existing?.calendarCreatedByApp ?: false
        )
    }

    private fun datedTimeFields(
        input: TodoInput,
        dateFields: TodoDateFields.Dated,
        prefsSnapshot: TodoPreferencesSnapshot
    ): TodoTimeFields {
        val remindStart = computeRemindAt(
            date = dateFields.date,
            hour = input.startHour,
            minute = input.startMinute,
            hoursBefore = 0,
            defaultHour = prefsSnapshot.defaultRemindHour,
            defaultMinute = prefsSnapshot.defaultRemindMinute,
            allDayFallback = false
        )
        val endHoursBefore = TodoReminderDefaults.effectiveHoursBefore(
            customHoursBefore = input.customHoursBefore,
            defaultHoursBefore = prefsSnapshot.defaultHoursBefore
        )
        val remindEnd = computeRemindAt(
            date = dateFields.date,
            hour = input.deadlineHour,
            minute = input.deadlineMinute,
            hoursBefore = endHoursBefore,
            defaultHour = prefsSnapshot.defaultRemindHour,
            defaultMinute = prefsSnapshot.defaultRemindMinute,
            allDayFallback = true
        )
        return TodoTimeFields(
            startHour = input.startHour,
            startMinute = input.startMinute,
            deadlineHour = input.deadlineHour,
            deadlineMinute = input.deadlineMinute,
            customHoursBefore = input.customHoursBefore,
            remindStartAtMillis = remindStart,
            remindAtMillis = remindEnd
        )
    }

    private suspend fun resolveTagId(name: String?): Long? {
        val n = name?.trim().orEmpty()
        if (n.isEmpty()) return null
        tagDao.findByName(n)?.let { return it.id }
        return tagDao.insert(TagEntity(name = n)).takeIf { it != -1L }
            ?: tagDao.findByName(n)?.id
    }

    private fun computeRemindAt(
        date: Int,
        hour: Int?,
        minute: Int?,
        hoursBefore: Int,
        defaultHour: Int,
        defaultMinute: Int,
        allDayFallback: Boolean
    ): Long? {
        if (hour != null && minute != null) {
            val target = DateUtils.timeOnDayMillis(date, hour, minute)
            return target - hoursBefore * 3_600_000L
        }
        if (!allDayFallback) return null
        return DateUtils.timeOnDayMillis(date, defaultHour, defaultMinute)
    }

    private data class TodoTimeFields(
        val startHour: Int? = null,
        val startMinute: Int? = null,
        val deadlineHour: Int? = null,
        val deadlineMinute: Int? = null,
        val customHoursBefore: Int? = null,
        val remindStartAtMillis: Long? = null,
        val remindAtMillis: Long? = null
    )
}

package com.zahri.lighttodo.data.todo

import com.zahri.lighttodo.data.local.TodoEntity
import com.zahri.lighttodo.usecase.todo.TodoRecord

internal fun TodoEntity.toTodoRecord(): TodoRecord =
    TodoRecord(
        id = id,
        title = title,
        note = note,
        date = date,
        dateMillis = dateMillis,
        startHour = startHour,
        startMinute = startMinute,
        deadlineHour = deadlineHour,
        deadlineMinute = deadlineMinute,
        remindStartAtMillis = remindStartAtMillis,
        remindAtMillis = remindAtMillis,
        customRemindHoursBefore = customRemindHoursBefore,
        tagId = tagId,
        done = done,
        doneAtMillis = doneAtMillis,
        createdAtMillis = createdAtMillis,
        calendarEventId = calendarEventId,
        calendarCreatedByApp = calendarCreatedByApp
    )

internal fun TodoRecord.toEntity(): TodoEntity =
    TodoEntity(
        id = id,
        title = title,
        note = note,
        date = date,
        dateMillis = dateMillis,
        startHour = startHour,
        startMinute = startMinute,
        deadlineHour = deadlineHour,
        deadlineMinute = deadlineMinute,
        remindStartAtMillis = remindStartAtMillis,
        remindAtMillis = remindAtMillis,
        customRemindHoursBefore = customRemindHoursBefore,
        tagId = tagId,
        done = done,
        doneAtMillis = doneAtMillis,
        createdAtMillis = createdAtMillis,
        calendarEventId = calendarEventId,
        calendarCreatedByApp = calendarCreatedByApp
    )

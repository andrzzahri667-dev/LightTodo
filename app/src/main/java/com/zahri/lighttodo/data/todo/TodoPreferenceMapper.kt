package com.zahri.lighttodo.data.todo

import com.zahri.lighttodo.data.prefs.UserPrefs
import com.zahri.lighttodo.usecase.todo.TodoPreferencesSnapshot

internal fun UserPrefs.Snapshot.toTodoPreferencesSnapshot(): TodoPreferencesSnapshot =
    TodoPreferencesSnapshot(
        defaultRemindHour = defaultRemindHour,
        defaultRemindMinute = defaultRemindMinute,
        defaultHoursBefore = defaultHoursBefore,
        calendarSyncEnabled = calendarSyncEnabled,
        calendarAccountName = calendarAccountName
    )

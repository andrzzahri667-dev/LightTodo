package com.zahri.lighttodo.usecase.boot

import com.zahri.lighttodo.usecase.todo.RescheduleRemindersUseCase

interface BootPreferencesRepository {
    suspend fun bootPreferences(): BootPreferences
}

data class BootPreferences(
    val quickAddNotifEnabled: Boolean = false,
    val calendarSyncEnabled: Boolean = false
)

class HandleBootCompletedUseCase(
    private val prefs: BootPreferencesRepository,
    private val rescheduleReminders: RescheduleRemindersUseCase
) {
    suspend operator fun invoke(): BootCompletedActions {
        rescheduleReminders()
        val snapshot = prefs.bootPreferences()
        return BootCompletedActions(
            startQuickAddService = snapshot.quickAddNotifEnabled,
            syncCalendar = snapshot.calendarSyncEnabled
        )
    }
}

data class BootCompletedActions(
    val startQuickAddService: Boolean,
    val syncCalendar: Boolean
)

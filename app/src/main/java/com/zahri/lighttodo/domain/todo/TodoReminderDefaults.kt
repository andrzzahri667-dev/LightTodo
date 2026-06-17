package com.zahri.lighttodo.domain.todo

object TodoReminderDefaults {
    fun effectiveHoursBefore(customHoursBefore: Int?, defaultHoursBefore: Int): Int =
        (customHoursBefore ?: defaultHoursBefore).coerceIn(0, 72)

    fun adjustHoursBefore(customHoursBefore: Int?, defaultHoursBefore: Int, delta: Int): Int =
        (effectiveHoursBefore(customHoursBefore, defaultHoursBefore) + delta).coerceIn(0, 72)
}

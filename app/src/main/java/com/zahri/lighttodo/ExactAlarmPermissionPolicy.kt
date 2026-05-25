package com.zahri.lighttodo

import android.os.Build

object ExactAlarmPermissionPolicy {
    fun shouldRequestSettings(
        sdkInt: Int = Build.VERSION.SDK_INT,
        canScheduleExactAlarms: Boolean,
        hasReminder: Boolean
    ): Boolean =
        hasReminder &&
            sdkInt >= Build.VERSION_CODES.S &&
            !canScheduleExactAlarms
}

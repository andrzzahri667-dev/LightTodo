package com.zahri.lighttodo

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExactAlarmPermissionPolicyTest {

    @Test
    fun shouldRequestOnlyWhenSavingReminderOnAndroid12PlusWithoutExactAccess() {
        assertTrue(
            ExactAlarmPermissionPolicy.shouldRequestSettings(
                sdkInt = Build.VERSION_CODES.S,
                canScheduleExactAlarms = false,
                hasReminder = true
            )
        )
    }

    @Test
    fun shouldNotRequestOnStartupOrForUndatedTodos() {
        assertFalse(
            ExactAlarmPermissionPolicy.shouldRequestSettings(
                sdkInt = Build.VERSION_CODES.S,
                canScheduleExactAlarms = false,
                hasReminder = false
            )
        )
    }

    @Test
    fun shouldNotRequestWhenExactAccessAlreadyGrantedOrBeforeAndroid12() {
        assertFalse(
            ExactAlarmPermissionPolicy.shouldRequestSettings(
                sdkInt = Build.VERSION_CODES.S,
                canScheduleExactAlarms = true,
                hasReminder = true
            )
        )
        assertFalse(
            ExactAlarmPermissionPolicy.shouldRequestSettings(
                sdkInt = Build.VERSION_CODES.R,
                canScheduleExactAlarms = false,
                hasReminder = true
            )
        )
    }
}

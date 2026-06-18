package com.zahri.lighttodo.domain.reminder

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderFullScreenPolicyTest {

    @Test
    fun attachesFullScreenIntentBeforeAndroid14() {
        assertTrue(
            ReminderFullScreenPolicy.shouldAttachFullScreenIntent(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                canUseFullScreenIntent = false
            )
        )
    }

    @Test
    fun attachesFullScreenIntentOnAndroid14OnlyWhenAllowed() {
        assertTrue(
            ReminderFullScreenPolicy.shouldAttachFullScreenIntent(
                sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                canUseFullScreenIntent = true
            )
        )
        assertFalse(
            ReminderFullScreenPolicy.shouldAttachFullScreenIntent(
                sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                canUseFullScreenIntent = false
            )
        )
    }
}

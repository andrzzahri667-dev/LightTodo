package com.zahri.lighttodo.integration.reminder

import android.os.Build

object ReminderFullScreenPolicy {
    fun shouldAttachFullScreenIntent(
        sdkInt: Int = Build.VERSION.SDK_INT,
        canUseFullScreenIntent: Boolean
    ): Boolean =
        sdkInt < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || canUseFullScreenIntent
}

package com.zahri.lighttodo.domain.reminder

object ReminderFullScreenPolicy {
    const val Android14Sdk = 34

    fun shouldAttachFullScreenIntent(
        sdkInt: Int,
        canUseFullScreenIntent: Boolean
    ): Boolean =
        sdkInt < Android14Sdk || canUseFullScreenIntent
}

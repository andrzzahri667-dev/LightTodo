package com.zahri.lighttodo.ui.note

enum class NoteLaunchSourceVisibilityAction {
    Keep,
    HideBeforeLaunch
}

object NoteLaunchSourceVisibilityPolicy {
    fun actionFor(
        miuiOptionsAvailable: Boolean,
        platformFallbackOptionsAvailable: Boolean
    ): NoteLaunchSourceVisibilityAction =
        if (!miuiOptionsAvailable && platformFallbackOptionsAvailable) {
            NoteLaunchSourceVisibilityAction.HideBeforeLaunch
        } else {
            NoteLaunchSourceVisibilityAction.Keep
        }
}

package com.zahri.lighttodo.ui.note

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteLaunchSourceVisibilityPolicyTest {
    @Test
    fun actionFor_miuiOptionsKeepVisibilityControlledByCallbacks() {
        assertEquals(
            NoteLaunchSourceVisibilityAction.Keep,
            NoteLaunchSourceVisibilityPolicy.actionFor(
                miuiOptionsAvailable = true,
                platformFallbackOptionsAvailable = true
            )
        )
    }

    @Test
    fun actionFor_platformFallbackHidesSourceBeforeLaunchToAvoidDoubleImage() {
        assertEquals(
            NoteLaunchSourceVisibilityAction.HideBeforeLaunch,
            NoteLaunchSourceVisibilityPolicy.actionFor(
                miuiOptionsAvailable = false,
                platformFallbackOptionsAvailable = true
            )
        )
    }

    @Test
    fun actionFor_noAnimatedOptionsKeepsSourceVisible() {
        assertEquals(
            NoteLaunchSourceVisibilityAction.Keep,
            NoteLaunchSourceVisibilityPolicy.actionFor(
                miuiOptionsAvailable = false,
                platformFallbackOptionsAvailable = false
            )
        )
    }
}

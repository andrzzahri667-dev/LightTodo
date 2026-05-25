package com.zahri.lighttodo.ui.note

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteEditorLaunchAnimationModePolicyTest {
    @Test
    fun modeFor_miuiOptionsUsesSystemScaleUpDown() {
        assertEquals(
            NoteEditorLaunchAnimationMode.MiuiSystemScaleUpDown,
            NoteEditorLaunchAnimationModePolicy.modeFor(
                miuiOptionsAvailable = true,
                sourceBoundsAvailable = true
            )
        )
    }

    @Test
    fun modeFor_noMiuiWithSourceUsesCustomContainerTransform() {
        assertEquals(
            NoteEditorLaunchAnimationMode.CustomContainerTransform,
            NoteEditorLaunchAnimationModePolicy.modeFor(
                miuiOptionsAvailable = false,
                sourceBoundsAvailable = true
            )
        )
    }

    @Test
    fun modeFor_noSourceStartsPlain() {
        assertEquals(
            NoteEditorLaunchAnimationMode.Plain,
            NoteEditorLaunchAnimationModePolicy.modeFor(
                miuiOptionsAvailable = false,
                sourceBoundsAvailable = false
            )
        )
    }
}

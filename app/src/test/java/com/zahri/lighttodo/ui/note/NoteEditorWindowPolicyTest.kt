package com.zahri.lighttodo.feature.noteeditor

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteEditorWindowPolicyTest {
    @Test
    fun shouldForceTransparentWindow_onlyForCustomContainerTransform() {
        assertEquals(
            false,
            NoteEditorWindowPolicy.shouldForceTransparentWindow(NoteEditorLaunchAnimationMode.MiuiSystemScaleUpDown)
        )
        assertEquals(
            true,
            NoteEditorWindowPolicy.shouldForceTransparentWindow(NoteEditorLaunchAnimationMode.CustomContainerTransform)
        )
        assertEquals(
            false,
            NoteEditorWindowPolicy.shouldForceTransparentWindow(NoteEditorLaunchAnimationMode.Plain)
        )
    }

    @Test
    fun shouldUseEdgeToEdgeWindow_forEveryEditorLaunchMode() {
        NoteEditorLaunchAnimationMode.entries.forEach { mode ->
            assertEquals(true, NoteEditorWindowPolicy.shouldUseEdgeToEdgeWindow(mode))
        }
    }
}

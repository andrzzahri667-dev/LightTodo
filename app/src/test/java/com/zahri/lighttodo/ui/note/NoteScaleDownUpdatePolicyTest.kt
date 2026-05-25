package com.zahri.lighttodo.ui.note

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteScaleDownUpdatePolicyTest {
    @Test
    fun actionFor_idleHostDoesNothing() {
        assertEquals(
            NoteScaleDownUpdateAction.None,
            NoteScaleDownUpdatePolicy.actionFor(editorWasLaunched = false, hasScaleDownData = true)
        )
    }

    @Test
    fun actionFor_launchedEditorWithScaleDataUpdatesSystemReturnTarget() {
        assertEquals(
            NoteScaleDownUpdateAction.UpdateData,
            NoteScaleDownUpdatePolicy.actionFor(editorWasLaunched = true, hasScaleDownData = true)
        )
    }

    @Test
    fun actionFor_launchedEditorWithoutScaleDataDisablesStaleBackAnimation() {
        assertEquals(
            NoteScaleDownUpdateAction.DisableAnimation,
            NoteScaleDownUpdatePolicy.actionFor(editorWasLaunched = true, hasScaleDownData = false)
        )
    }
}

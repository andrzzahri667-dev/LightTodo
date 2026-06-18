package com.zahri.lighttodo.feature.noteeditor

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteScaleDownUpdatePolicyTest {
    @Test
    fun actionFor_idleHostDoesNothing() {
        assertEquals(
            NoteScaleDownUpdateAction.None,
            NoteScaleDownUpdatePolicy.actionFor(editorWasLaunched = false, miuiReturnAnimationPrepared = true)
        )
    }

    @Test
    fun actionFor_launchedEditorWithMiuiReturnDataLetsSystemRunOriginalReturnAnimation() {
        assertEquals(
            NoteScaleDownUpdateAction.KeepSystemReturn,
            NoteScaleDownUpdatePolicy.actionFor(editorWasLaunched = true, miuiReturnAnimationPrepared = true)
        )
    }

    @Test
    fun actionFor_launchedEditorWithoutMiuiReturnDataDisablesStaleBackAnimation() {
        assertEquals(
            NoteScaleDownUpdateAction.DisableAnimation,
            NoteScaleDownUpdatePolicy.actionFor(editorWasLaunched = true, miuiReturnAnimationPrepared = false)
        )
    }
}

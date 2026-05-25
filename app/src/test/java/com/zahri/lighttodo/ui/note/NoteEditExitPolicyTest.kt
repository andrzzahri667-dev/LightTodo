package com.zahri.lighttodo.ui.note

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteEditExitPolicyTest {
    @Test
    fun actionFor_miuiReturnPreparedUsesSystemScaleDown() {
        assertEquals(
            NoteEditExitAction.SystemScaleDown,
            NoteEditExitPolicy.actionFor(miuiReturnAnimationPrepared = true)
        )
    }

    @Test
    fun actionFor_withoutMiuiReturnUsesExplicitFadeFallback() {
        assertEquals(
            NoteEditExitAction.FadeFallback,
            NoteEditExitPolicy.actionFor(miuiReturnAnimationPrepared = false)
        )
    }
}

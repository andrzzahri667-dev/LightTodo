package com.zahri.lighttodo.ui.note

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteSourceVisibilityCallbackPolicyTest {
    @Test
    fun actionFor_sourceActivityExitHidesThenRestoresSourceView() {
        assertEquals(
            NoteSourceVisibilityAction.Hide,
            NoteSourceVisibilityCallbackPolicy.actionFor(NoteSourceVisibilityCallbackPhase.SourceExitStarted)
        )
        assertEquals(
            NoteSourceVisibilityAction.Show,
            NoteSourceVisibilityCallbackPolicy.actionFor(NoteSourceVisibilityCallbackPhase.SourceExitFinished)
        )
    }

    @Test
    fun actionFor_reenterKeepsSourceViewVisibleForScaleDownLanding() {
        assertEquals(
            NoteSourceVisibilityAction.Keep,
            NoteSourceVisibilityCallbackPolicy.actionFor(NoteSourceVisibilityCallbackPhase.SourceReenterStarted)
        )
        assertEquals(
            NoteSourceVisibilityAction.Keep,
            NoteSourceVisibilityCallbackPolicy.actionFor(NoteSourceVisibilityCallbackPhase.SourceReenterFinished)
        )
    }
}

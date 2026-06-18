package com.zahri.lighttodo.feature.noteeditor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class NoteMediaSessionStateTest {

    @Test
    fun defaults_areIdle() {
        val state = NoteMediaSessionState()

        assertFalse(state.recording)
        assertNull(state.playingAudioRef)
    }
}

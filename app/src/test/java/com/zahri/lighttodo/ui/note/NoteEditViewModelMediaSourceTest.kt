package com.zahri.lighttodo.feature.noteeditor

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertTrue
import org.junit.Test

class NoteEditViewModelMediaSourceTest {
    @Test
    fun stopAudioPlaybackStopsPlayerBeforeRelease() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditViewModel.kt").readText()

        assertTrue(source.indexOf("activePlayer.runCatching { stop() }") < source.indexOf("activePlayer.release()"))
    }
}

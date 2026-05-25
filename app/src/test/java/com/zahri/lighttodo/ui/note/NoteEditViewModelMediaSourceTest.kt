package com.zahri.lighttodo.ui.note

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NoteEditViewModelMediaSourceTest {
    @Test
    fun stopAudioPlaybackStopsPlayerBeforeRelease() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/NoteEditViewModel.kt").readText()

        assertTrue(source.indexOf("activePlayer.runCatching { stop() }") < source.indexOf("activePlayer.release()"))
    }

    private fun sourceFile(relativePath: String): File {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        var dir = File(userDir).absoluteFile
        while (true) {
            val candidate = File(dir, relativePath)
            if (candidate.exists()) return candidate
            dir = dir.parentFile ?: break
        }
        error("Could not find $relativePath from $userDir")
    }
}

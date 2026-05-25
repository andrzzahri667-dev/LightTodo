package com.zahri.lighttodo.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AccessibilitySourceTest {
    @Test
    fun selectionCloseButtonsHaveContentDescriptions() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/HomeScreen.kt").readText()

        assertTrue(source.contains("contentDescription = stringResource(R.string.home_clear_selection)"))
    }

    @Test
    fun todoSelectionIndicatorHasStateDescription() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/HomeTodoPage.kt").readText()

        assertTrue(source.contains("R.string.home_selected_indicator"))
        assertTrue(source.contains("R.string.home_not_selected_indicator"))
    }

    @Test
    fun audioPlayPauseButtonHasContentDescription() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/NoteEditScreen.kt").readText()

        assertTrue(source.contains("if (playing) R.string.note_audio_pause else R.string.note_audio_play"))
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

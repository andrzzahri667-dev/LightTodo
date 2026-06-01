package com.zahri.lighttodo.ui

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertTrue
import org.junit.Test

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
}

package com.zahri.lighttodo.ui.note

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteEditorColorsTest {
    @Test
    fun background_usesWarmPaperInLightThemeAndBlackInDarkTheme() {
        assertEquals(Color(0xFFFFFCF6), NoteEditorColors.background(isDark = false))
        assertEquals(Color.Black, NoteEditorColors.background(isDark = true))
    }

    @Test
    fun editorAndTransformPreviewShareBackgroundColor() {
        listOf(false, true).forEach { isDark ->
            assertEquals(
                NoteEditorColors.editorBackground(isDark),
                NoteEditorColors.transformPreviewBackground(isDark)
            )
        }
    }
}

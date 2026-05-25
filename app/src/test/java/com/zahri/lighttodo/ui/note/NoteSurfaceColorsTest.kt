package com.zahri.lighttodo.ui.note

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteSurfaceColorsTest {
    @Test
    fun background_usesWarmPaperInLightThemeAndBlackInDarkTheme() {
        assertEquals(Color(0xFFFFFCF6), NoteSurfaceColors.background(isDark = false))
        assertEquals(Color.Black, NoteSurfaceColors.background(isDark = true))
    }

    @Test
    fun notesPageAndEditorShareBackgroundColor() {
        listOf(false, true).forEach { isDark ->
            assertEquals(
                NoteSurfaceColors.background(isDark),
                NoteSurfaceColors.notesPageBackground(isDark)
            )
            assertEquals(
                NoteSurfaceColors.background(isDark),
                NoteSurfaceColors.editorBackground(isDark)
            )
        }
    }
}

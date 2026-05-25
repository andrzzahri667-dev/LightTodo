package com.zahri.lighttodo.ui.note

import androidx.compose.ui.graphics.Color

object NoteEditorColors {
    private val LightPaper = Color(0xFFFFFCF6)
    private val DarkPaper = Color.Black

    fun background(isDark: Boolean): Color =
        if (isDark) DarkPaper else LightPaper

    fun editorBackground(isDark: Boolean): Color =
        background(isDark)

    fun transformPreviewBackground(isDark: Boolean): Color =
        background(isDark)
}

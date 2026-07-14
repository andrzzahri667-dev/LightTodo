package com.zahri.lighttodo.feature.noteeditor

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteMediaAccessibilitySourceTest {

    private val source by lazy {
        sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditScreen.kt"
        ).readText()
    }

    @Test
    fun actualAudioBlockUsesMarkdownThemePalette() {
        val audioBlock = source
            .substringAfter("private fun NoteAudioBlock(")
            .substringBefore("private fun mediaSelectionModifier(")

        assertTrue(audioBlock.contains("MarkdownRenderStyle.forDarkMode(isDark)"))
        assertTrue(audioBlock.contains("renderStyle.audioBackgroundColor"))
        assertTrue(audioBlock.contains("renderStyle.audioActiveBackgroundColor"))
        assertTrue(audioBlock.contains("renderStyle.audioAccentColor"))
        assertTrue(audioBlock.contains("renderStyle.audioTextColor"))
        assertTrue(audioBlock.contains("renderStyle.audioWaveColor"))
        assertFalse(audioBlock.contains("Color(0xFFFFF1DA)"))
    }

    @Test
    fun mediaSurfacesExposeClickAndLongClickSemantics() {
        assertTrue(source.contains("private fun Modifier.mediaInteractionModifier("))
        assertTrue(source.contains("this.contentDescription = description"))
        assertTrue(source.contains(".semantics(mergeDescendants = true)"))
        assertTrue(source.contains("role = Role.Button"))
        assertTrue(source.contains("semanticsOnClick(label = clickLabel)"))
        assertTrue(source.contains("semanticsOnLongClick(label = longClickLabel)"))
        assertTrue(source.contains("R.string.note_image_attachment"))
        assertTrue(source.contains("R.string.note_audio_attachment"))
    }

    @Test
    fun audioSurfaceMeetsMinimumTouchHeight() {
        val audioBlock = source
            .substringAfter("private fun NoteAudioBlock(")
            .substringBefore("private fun mediaSelectionModifier(")

        assertTrue(audioBlock.contains(".height(48.dp)"))
        assertFalse(audioBlock.contains(".height(44.dp)"))
    }
}

package com.zahri.lighttodo.ui.motion

import com.zahri.lighttodo.test.sourceFile
import com.zahri.lighttodo.test.sourcePath

import org.junit.Assert.assertFalse
import org.junit.Test

class MotionContentSizeSourceTest {
    @Test
    fun noteEditorTypingDoesNotAnimateContentSize() {
        val noteSource = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditScreen.kt").readText()
        val motionSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/AppMotion.kt").readText()
        val componentFile = sourcePath("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionContentSize.kt")

        assertFalse(noteSource.contains("motionNoteContentSize"))
        assertFalse(noteSource.contains("import androidx.compose.animation.animateContentSize"))
        assertFalse(noteSource.contains("animateContentSize("))
        assertFalse(motionSource.contains("NoteContentSize"))
        assertFalse(motionSource.contains("noteContentSizeSpring"))
        assertFalse(componentFile.exists())
    }
}

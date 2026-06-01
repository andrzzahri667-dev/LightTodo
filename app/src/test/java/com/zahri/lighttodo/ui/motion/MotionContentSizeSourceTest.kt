package com.zahri.lighttodo.ui.motion

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionContentSizeSourceTest {
    @Test
    fun noteContentSizeMotionIsCentralizedOutsideEditorScreen() {
        val noteSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/NoteEditScreen.kt").readText()
        val componentSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionContentSize.kt").readText()

        assertTrue(noteSource.contains("import com.zahri.lighttodo.ui.motion.components.motionNoteContentSize"))
        assertTrue(noteSource.contains(".motionNoteContentSize()"))
        assertFalse(noteSource.contains("import androidx.compose.animation.animateContentSize"))
        assertFalse(noteSource.contains("animateContentSize(animationSpec = AppMotion.noteContentSizeSpring())"))
        assertTrue(componentSource.contains("import androidx.compose.animation.animateContentSize"))
        assertTrue(componentSource.contains("fun Modifier.motionNoteContentSize(): Modifier"))
        assertTrue(componentSource.contains("AppMotion.noteContentSizeSpring()"))
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

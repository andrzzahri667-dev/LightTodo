package com.zahri.lighttodo.ui.note

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteEditorMotionLayoutSourceTest {
    @Test
    fun customContainerTransformUsesMotionLayoutFallbackEngine() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/NoteEditActivity.kt")
            .readText()

        assertTrue(source.contains("import androidx.constraintlayout.compose.MotionLayout"))
        assertTrue(source.contains("NoteEditorMotionLayoutTransformHost("))
        assertTrue(source.contains("MotionLayout("))
        assertTrue(source.contains("geometryProgress = NoteEditorContainerTransformPolicy.geometryProgressFor(progress.value)"))
        assertFalse(source.contains("private fun NoteEditorContainerTransformHost("))
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

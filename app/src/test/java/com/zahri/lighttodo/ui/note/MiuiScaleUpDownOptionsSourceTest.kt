package com.zahri.lighttodo.ui.note

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MiuiScaleUpDownOptionsSourceTest {
    @Test
    fun miuiReflectionAdapterLivesOutsideNoteEditorLauncher() {
        val launcher = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/NoteEditorLauncher.kt").readText()
        val adapter = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/MiuiScaleUpDownOptions.kt").readText()

        assertFalse(launcher.contains("private object MiuiScaleUpDownOptions"))
        assertTrue(adapter.contains("object MiuiScaleUpDownOptions"))
        assertTrue(adapter.contains("ActivityOptions::class.java.getMethod"))
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

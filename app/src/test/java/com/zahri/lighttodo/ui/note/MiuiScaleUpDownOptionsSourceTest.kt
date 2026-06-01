package com.zahri.lighttodo.ui.note

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiuiScaleUpDownOptionsSourceTest {
    @Test
    fun miuiReflectionAdapterLivesOutsideNoteEditorLauncher() {
        val launcher = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/NoteEditorLauncher.kt").readText()
        val adapter = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/MiuiScaleUpDownOptions.kt").readText()

        assertFalse(launcher.contains("private object MiuiScaleUpDownOptions"))
        assertTrue(adapter.contains("object MiuiScaleUpDownOptions"))
        assertTrue(adapter.contains("ActivityOptions::class.java.getMethod"))
    }
}

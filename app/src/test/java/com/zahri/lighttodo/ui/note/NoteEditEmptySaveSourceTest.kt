package com.zahri.lighttodo.feature.noteeditor

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteEditEmptySaveSourceTest {
    @Test
    fun onlyUnsavedNewNoteSkipsAnEmptySave() {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditViewModel.kt"
        ).readText()
        val saveBody = source
            .substringAfter("fun save()")
            .substringBefore("fun delete(onDone: () -> Unit)")

        assertTrue(
            saveBody.contains("if (noteId == null && t.isEmpty() && c.isBlank()) return@withContext")
        )
        assertFalse(
            saveBody.contains("if (t.isEmpty() && c.isBlank()) return@withContext")
        )
    }
}

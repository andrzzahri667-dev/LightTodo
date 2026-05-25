package com.zahri.lighttodo.ui.note

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NoteEditActivityStateRetentionTest {
    @Test
    fun customTransformEntryAnimationStateSurvivesActivityRecreation() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/NoteEditActivity.kt")
            .readText()

        assertTrue(
            "entryPlayed must use rememberSaveable so rotation does not replay the entry transform",
            source.contains("var entryPlayed by rememberSaveable { mutableStateOf(false) }")
        )
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

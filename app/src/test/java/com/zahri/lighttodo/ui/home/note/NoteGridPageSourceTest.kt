package com.zahri.lighttodo.ui.home.note

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NoteGridPageSourceTest {
    @Test
    fun noteCardsUseMinimumHeightInsteadOfFixedHeight() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/note/NoteGridPage.kt").readText()

        assertTrue(source.contains("private val CARD_MIN_HEIGHT = 160.dp"))
        assertTrue(source.contains(".defaultMinSize(minHeight = CARD_MIN_HEIGHT)"))
        assertFalse(source.contains(".height(CARD_HEIGHT)"))
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

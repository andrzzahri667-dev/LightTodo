package com.zahri.lighttodo.ui.motion

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionSelectionAndExpansionSourceTest {
    @Test
    fun selectionColorAndExpansionRotationAreCentralizedOutsideScreens() {
        val todoSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/HomeTodoPage.kt").readText()
        val noteGridSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/note/NoteGridPage.kt").readText()
        val selectionSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionSelectionColor.kt").readText()
        val expansionSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionExpansionRotation.kt").readText()

        assertTrue(todoSource.contains("import com.zahri.lighttodo.ui.motion.components.rememberMotionExpansionRotation"))
        assertTrue(todoSource.contains("import com.zahri.lighttodo.ui.motion.components.rememberMotionSelectionColor"))
        assertTrue(noteGridSource.contains("import com.zahri.lighttodo.ui.motion.components.rememberMotionNoteCardSelectionColor"))
        assertTrue(todoSource.contains("val arrowRotation by rememberMotionExpansionRotation("))
        assertTrue(todoSource.contains("val selectedBackground by rememberMotionSelectionColor("))
        assertTrue(noteGridSource.contains("val cardBackground by rememberMotionNoteCardSelectionColor("))
        assertFalse(todoSource.contains("import androidx.compose.animation.animateColorAsState"))
        assertFalse(noteGridSource.contains("import androidx.compose.animation.animateColorAsState"))
        assertFalse(todoSource.contains("label = \"section-arrow\""))
        assertTrue(selectionSource.contains("animateColorAsState"))
        assertTrue(selectionSource.contains("AppMotion.SelectionColorMillis"))
        assertTrue(selectionSource.contains("AppMotion.NoteCardSelectionColorMillis"))
        assertTrue(expansionSource.contains("animateFloatAsState"))
        assertTrue(expansionSource.contains("AppMotion.SectionExpandedRotationDegrees"))
        assertTrue(expansionSource.contains("AppMotion.sectionArrowSpring()"))
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

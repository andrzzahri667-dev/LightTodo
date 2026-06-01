package com.zahri.lighttodo.ui.home.note

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
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

    @Test
    fun emptyNotePlaceholderIsSharedBetweenGridAndEditorPreview() {
        val gridSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/note/NoteGridPage.kt").readText()
        val editorSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/NoteEditActivity.kt").readText()
        val placeholderSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/NoteEmptyPlaceholder.kt").readText()

        assertTrue(gridSource.contains("NoteEmptyPlaceholder()"))
        assertTrue(editorSource.contains("NoteEmptyPlaceholder()"))
        assertEquals(1, Regex("note_no_title").findAll(placeholderSource).count())
        assertFalse(gridSource.contains("stringResource(R.string.note_no_title)"))
        assertFalse(editorSource.contains("stringResource(R.string.note_no_title)"))
    }

    @Test
    fun noteGridPlacementUsesCurrentLazyItemMotionApi() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/note/NoteGridPage.kt").readText()
        val placementSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionNoteGridItemPlacement.kt").readText()
        val selectionSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionSelectionColor.kt").readText()

        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.components.motionNoteGridItem"))
        assertTrue(source.contains("modifier = motionNoteGridItem()"))
        assertTrue(source.contains("rememberMotionNoteCardSelectionColor("))
        assertFalse(source.contains("import com.zahri.lighttodo.ui.motion.AppMotion"))
        assertFalse(source.contains("Modifier.animateItem("))
        assertFalse(source.contains("placementSpec = AppMotion.noteGridPlacementSpring()"))
        assertFalse(source.contains("durationMillis = AppMotion.NoteCardSelectionColorMillis"))
        assertFalse(source.contains("animateItemPlacement("))
        assertTrue(placementSource.contains("fun LazyStaggeredGridItemScope.motionNoteGridItem("))
        assertTrue(placementSource.contains("animateItem("))
        assertTrue(placementSource.contains("placementSpec = AppMotion.noteGridPlacementSpring()"))
        assertTrue(selectionSource.contains("fun rememberMotionNoteCardSelectionColor("))
        assertTrue(selectionSource.contains("durationMillis = AppMotion.NoteCardSelectionColorMillis"))
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

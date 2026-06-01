package com.zahri.lighttodo.ui.motion

import com.zahri.lighttodo.test.sourceFile

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
        assertTrue(todoSource.contains("import com.zahri.lighttodo.ui.motion.components.motionExpansionRotationLayer"))
        assertTrue(noteGridSource.contains("import com.zahri.lighttodo.ui.motion.components.rememberMotionNoteCardSelectionColor"))
        assertTrue(todoSource.contains("val arrowRotation by rememberMotionExpansionRotation("))
        assertTrue(todoSource.contains(".motionExpansionRotationLayer(arrowRotation)"))
        assertFalse(todoSource.contains(".graphicsLayer { rotationZ = arrowRotation }"))
        assertTrue(todoSource.contains("val selectedBackground by rememberMotionSelectionColor("))
        assertTrue(noteGridSource.contains("val cardBackground by rememberMotionNoteCardSelectionColor("))
        assertFalse(todoSource.contains("import androidx.compose.animation.animateColorAsState"))
        assertFalse(noteGridSource.contains("import androidx.compose.animation.animateColorAsState"))
        assertFalse(todoSource.contains("label = \"section-arrow\""))
        assertTrue(selectionSource.contains("animateColorAsState"))
        assertTrue(selectionSource.contains("AppMotion.SelectionColorMillis"))
        assertTrue(selectionSource.contains("AppMotion.NoteCardSelectionColorMillis"))
        assertTrue(expansionSource.contains("animateFloatAsState"))
        assertTrue(expansionSource.contains("fun Modifier.motionExpansionRotationLayer("))
        assertTrue(expansionSource.contains("graphicsLayer { rotationZ = rotationDegrees }"))
        assertTrue(expansionSource.contains("AppMotion.SectionExpandedRotationDegrees"))
        assertTrue(expansionSource.contains("AppMotion.sectionArrowSpring()"))
    }
}

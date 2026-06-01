package com.zahri.lighttodo.ui.motion

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTransientVisibilitySourceTest {
    @Test
    fun transientVisibilityIsCentralizedOutsideScreens() {
        val homeSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/HomeScreen.kt").readText()
        val noteSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/NoteEditScreen.kt").readText()
        val componentSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionTransientVisibility.kt").readText()

        assertTrue(homeSource.contains("import com.zahri.lighttodo.ui.motion.components.MotionTransientVisibility"))
        assertTrue(noteSource.contains("import com.zahri.lighttodo.ui.motion.components.MotionTransientVisibility"))
        assertTrue(homeSource.contains("MotionTransientVisibility("))
        assertTrue(noteSource.contains("MotionTransientVisibility("))
        assertFalse(homeSource.contains("import androidx.compose.animation.AnimatedVisibility"))
        assertFalse(noteSource.contains("import androidx.compose.animation.AnimatedVisibility"))
        assertFalse(homeSource.contains("transientSurfaceEnter()"))
        assertFalse(noteSource.contains("transientSurfaceEnter()"))
        assertTrue(componentSource.contains("AnimatedVisibility("))
        assertTrue(componentSource.contains("enter = AppMotion.transientSurfaceEnter()"))
        assertTrue(componentSource.contains("exit = AppMotion.transientSurfaceExit()"))
    }
}

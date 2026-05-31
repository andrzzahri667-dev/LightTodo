package com.zahri.lighttodo.ui.motion

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPressScaleSourceTest {
    @Test
    fun pressScaleFeedbackIsCentralizedOutsideHomeScreen() {
        val homeSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/HomeScreen.kt").readText()
        val componentSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionPressScale.kt").readText()

        assertTrue(homeSource.contains("import com.zahri.lighttodo.ui.motion.components.rememberMotionPressScale"))
        assertTrue(homeSource.contains("val scale by rememberMotionPressScale("))
        assertFalse(homeSource.contains("import androidx.compose.animation.core.animateFloatAsState"))
        assertFalse(homeSource.contains("import androidx.compose.foundation.interaction.collectIsPressedAsState"))
        assertFalse(homeSource.contains("label = \"fab-scale\""))
        assertFalse(homeSource.contains("targetValue = if (pressed) 0.92f else 1f"))
        assertTrue(componentSource.contains("collectIsPressedAsState"))
        assertTrue(componentSource.contains("animateFloatAsState"))
        assertTrue(componentSource.contains("AppMotion.PressScale"))
        assertTrue(componentSource.contains("AppMotion.pressSpring()"))
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

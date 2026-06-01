package com.zahri.lighttodo.ui.motion

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPressScaleSourceTest {
    @Test
    fun pressScaleFeedbackIsCentralizedOutsideHomeScreen() {
        val homeSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/HomeScreen.kt").readText()
        val componentSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionPressScale.kt").readText()

        assertTrue(homeSource.contains("import com.zahri.lighttodo.ui.motion.components.rememberMotionPressScale"))
        assertTrue(homeSource.contains("import com.zahri.lighttodo.ui.motion.components.motionPressScaleLayer"))
        assertTrue(homeSource.contains("val scale by rememberMotionPressScale("))
        assertTrue(homeSource.contains(".motionPressScaleLayer(scale)"))
        assertFalse(homeSource.contains("scaleX = scale"))
        assertFalse(homeSource.contains("scaleY = scale"))
        assertFalse(homeSource.contains("import androidx.compose.animation.core.animateFloatAsState"))
        assertFalse(homeSource.contains("import androidx.compose.foundation.interaction.collectIsPressedAsState"))
        assertFalse(homeSource.contains("import androidx.compose.ui.graphics.graphicsLayer"))
        assertFalse(homeSource.contains("label = \"fab-scale\""))
        assertFalse(homeSource.contains("targetValue = if (pressed) 0.92f else 1f"))
        assertTrue(componentSource.contains("collectIsPressedAsState"))
        assertTrue(componentSource.contains("animateFloatAsState"))
        assertTrue(componentSource.contains("fun Modifier.motionPressScaleLayer("))
        assertTrue(componentSource.contains("graphicsLayer"))
        assertTrue(componentSource.contains("AppMotion.PressScale"))
        assertTrue(componentSource.contains("AppMotion.pressSpring()"))
    }
}

package com.zahri.lighttodo.ui.motion

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionCompletionSettleSourceTest {
    @Test
    fun todoCompletionContentSettleIsCentralizedOutsideTodoRow() {
        val todoSource = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeTodoPage.kt").readText()
        val componentSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionCompletionSettle.kt").readText()

        assertTrue(todoSource.contains("import com.zahri.lighttodo.ui.motion.components.rememberMotionCompletionSettle"))
        assertTrue(todoSource.contains("import com.zahri.lighttodo.ui.motion.components.motionCompletionSettleLayer"))
        assertTrue(todoSource.contains("val completionSettle = rememberMotionCompletionSettle(animating)"))
        assertTrue(todoSource.contains(".motionCompletionSettleLayer(completionSettle)"))
        assertFalse(todoSource.contains("alpha = completionSettle.alpha"))
        assertFalse(todoSource.contains("translationX = completionSettle.translationX * density"))
        assertFalse(todoSource.contains("import androidx.compose.animation.core.animateFloatAsState"))
        assertFalse(todoSource.contains("val contentAlpha by animateFloatAsState"))
        assertFalse(todoSource.contains("val contentTranslateX by animateFloatAsState"))
        assertFalse(todoSource.contains("TodoContentSettleMillis"))
        assertTrue(componentSource.contains("animateFloatAsState"))
        assertTrue(componentSource.contains("fun Modifier.motionCompletionSettleLayer("))
        assertTrue(componentSource.contains("graphicsLayer"))
        assertTrue(componentSource.contains("alpha = settle.alpha"))
        assertTrue(componentSource.contains("translationX = settle.translationX * density"))
        assertTrue(componentSource.contains("AppMotion.TodoCompletedContentAlpha"))
        assertTrue(componentSource.contains("AppMotion.TodoCompletedContentTranslationX"))
        assertTrue(componentSource.contains("AppMotion.TodoContentSettleMillis"))
    }
}

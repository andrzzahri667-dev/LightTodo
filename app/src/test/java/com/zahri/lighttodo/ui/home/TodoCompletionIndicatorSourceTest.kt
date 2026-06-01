package com.zahri.lighttodo.ui.home

import com.zahri.lighttodo.test.sourceFile
import com.zahri.lighttodo.test.sourcePath

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoCompletionIndicatorSourceTest {
    @Test
    fun todoRowsDelegateCompletionIconToLottieBackedIndicator() {
        val rowSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/HomeTodoPage.kt").readText()
        val indicatorSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/TodoCompletionIndicator.kt").readText()

        assertTrue(rowSource.contains("import com.zahri.lighttodo.ui.motion.components.TodoCompletionIndicator"))
        assertTrue(rowSource.contains("TodoCompletionIndicator("))
        assertFalse(rowSource.contains("val checkScale = remember { androidx.compose.animation.core.Animatable"))
        assertFalse(rowSource.contains("val checkmarkAlpha = remember { androidx.compose.animation.core.Animatable"))
        assertFalse(sourcePath("app/src/main/java/com/zahri/lighttodo/ui/home/TodoCompletionIndicator.kt").exists())
        assertTrue(indicatorSource.contains("import com.airbnb.lottie.compose.LottieAnimation"))
        assertTrue(indicatorSource.contains("R.raw.todo_check_success"))
        assertFalse(indicatorSource.contains("Text(\"✓\""))
        assertTrue(sourceFile("app/src/main/res/raw/todo_check_success.json").exists())
    }
}

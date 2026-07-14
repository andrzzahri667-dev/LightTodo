package com.zahri.lighttodo.feature.home

import com.zahri.lighttodo.test.sourceFile
import com.zahri.lighttodo.test.sourcePath

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoCompletionIndicatorSourceTest {
    @Test
    fun todoRowsDelegateCompletionIconToInterruptibleComposeIndicator() {
        val rowSource = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeTodoPage.kt").readText()
        val indicatorSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/TodoCompletionIndicator.kt").readText()
        val dependencies = sourceFile("app/build.gradle.kts").readText()

        assertTrue(rowSource.contains("import com.zahri.lighttodo.ui.motion.components.TodoCompletionIndicator"))
        assertTrue(rowSource.contains("TodoCompletionIndicator("))
        assertTrue(rowSource.contains("onDoneChange = { done -> vm.toggleDone(todo.id, done) }"))
        assertTrue(rowSource.contains("onToggle = { onDoneChange(!displayDone) }"))
        assertFalse(rowSource.contains("enabled = !animating"))
        assertFalse(sourcePath("app/src/main/java/com/zahri/lighttodo/feature/home/TodoCompletionIndicator.kt").exists())
        assertTrue(indicatorSource.contains("import androidx.compose.material.icons.filled.Check"))
        assertTrue(indicatorSource.contains("animateFloatAsState"))
        assertTrue(indicatorSource.contains("AppMotion.TodoCompletionCheckStartScale"))
        assertTrue(indicatorSource.contains("AppMotion.TodoCompletionCheckMillis"))
        assertTrue(indicatorSource.contains("Icons.Default.Check"))
        assertTrue(indicatorSource.contains(".size(48.dp)"))
        assertTrue(indicatorSource.contains(".size(24.dp)"))
        assertTrue(rowSource.contains(".padding(start = 4.dp, end = 16.dp)"))
        assertTrue(rowSource.contains(".padding(vertical = 12.dp)"))
        assertFalse(
            rowSource.contains(
                ".padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)"
            )
        )
        assertFalse(indicatorSource.contains("com.airbnb.lottie"))
        assertFalse(indicatorSource.contains("R.raw.todo_check_success"))
        assertFalse(dependencies.contains("lottie-compose"))
        assertFalse(sourcePath("app/src/main/res/raw/todo_check_success.json").exists())
    }
}

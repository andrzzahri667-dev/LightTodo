package com.zahri.lighttodo

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewModelInjectionSourceTest {
    @Test
    fun viewModelsDoNotUseAppInstanceAsDefaultDependencySource() {
        val viewModelFiles = listOf(
            "app/src/main/java/com/zahri/lighttodo/feature/home/HomeViewModel.kt",
            "app/src/main/java/com/zahri/lighttodo/feature/todoedit/EditViewModel.kt",
            "app/src/main/java/com/zahri/lighttodo/feature/settings/SettingsViewModel.kt",
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditViewModel.kt"
        )

        viewModelFiles.forEach { path ->
            val source = sourceFile(path).readText()
            assertFalse("$path must not read App.instance", source.contains("App.instance"))
        }
    }

    @Test
    fun composeScreensUseAppViewModelFactory() {
        val screenFiles = listOf(
            "app/src/main/java/com/zahri/lighttodo/feature/home/HomeScreen.kt",
            "app/src/main/java/com/zahri/lighttodo/feature/todoedit/EditScreen.kt",
            "app/src/main/java/com/zahri/lighttodo/feature/settings/SettingsScreen.kt",
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditScreen.kt"
        )

        screenFiles.forEach { path ->
            val source = sourceFile(path).readText()
            assertTrue("$path must use injected ViewModel factory", source.contains("lightTodoViewModelFactory()"))
            assertTrue("$path must pass factory to viewModel", source.contains("viewModel(factory = lightTodoViewModelFactory())"))
        }
    }

    @Test
    fun noteEditActivityUsesAppViewModelFactory() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditActivity.kt").readText()

        assertTrue(source.contains("by viewModels {"))
        assertTrue(source.contains("lightTodoViewModelFactory(this)"))
        assertFalse(source.contains("by viewModels()"))
    }

    @Test
    fun activityFeaturesUseInjectedViewModelFactory() {
        val activityFiles = listOf(
            "app/src/main/java/com/zahri/lighttodo/feature/quickadd/QuickAddActivity.kt",
            "app/src/main/java/com/zahri/lighttodo/feature/reminder/ReminderActivity.kt",
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditActivity.kt"
        )

        activityFiles.forEach { path ->
            val source = sourceFile(path).readText()
            assertTrue("$path must use Activity ViewModel injection", source.contains("by viewModels {"))
            assertTrue("$path must use app factory helper", source.contains("lightTodoViewModelFactory(this)"))
            assertFalse("$path must not use default ViewModel construction", source.contains("by viewModels()"))
        }
    }

    @Test
    fun appExposesContainerAndFactory() {
        val appSource = sourceFile("app/src/main/java/com/zahri/lighttodo/App.kt").readText()
        val containerSource = sourceFile("app/src/main/java/com/zahri/lighttodo/AppContainer.kt").readText()

        assertTrue(appSource.contains("val container by lazy"))
        assertTrue(appSource.contains("val viewModelFactory"))
        assertTrue(containerSource.contains("class AppContainer"))
        assertTrue(containerSource.contains("class LightTodoViewModelFactory"))
    }
}

package com.zahri.lighttodo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomePerformanceSourceTest {
    @Test
    fun homeScreenDerivesCurrentPageWithoutRecomposingWholeScaffoldEveryFrame() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/HomeScreen.kt").readText()

        assertTrue(source.contains("derivedStateOf { pagerState.currentPage }"))
    }

    @Test
    fun homeViewModelSuppressesDuplicateHomeDataEmissions() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/HomeViewModel.kt").readText()

        assertTrue(source.contains(".distinctUntilChanged()"))
    }

    @Test
    fun homeUiStateMapperPartitionsTodosInOnePass() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/HomeUiStateMapper.kt").readText()

        assertTrue(source.contains("val (done, undone) = data.todos.partition { it.done }"))
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

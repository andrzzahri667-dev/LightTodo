package com.zahri.lighttodo.ui.home

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertTrue
import org.junit.Test

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
}

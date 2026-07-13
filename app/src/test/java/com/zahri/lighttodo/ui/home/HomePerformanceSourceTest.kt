package com.zahri.lighttodo.feature.home

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePerformanceSourceTest {
    @Test
    fun homeScreenDerivesCurrentPageWithoutRecomposingWholeScaffoldEveryFrame() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeScreen.kt").readText()

        assertTrue(source.contains("derivedStateOf { pagerState.currentPage }"))
    }

    @Test
    fun homeViewModelSuppressesDuplicateHomeDataEmissions() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeViewModel.kt").readText()

        assertTrue(source.contains(".distinctUntilChanged()"))
    }

    @Test
    fun homeUiStateMapperPartitionsTodosInOnePass() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeUiStateMapper.kt").readText()

        assertTrue(source.contains("val (done, undone) = data.todos.partition { it.done }"))
    }

    @Test
    fun homeTodoListReusesOnlyCompatibleCompositions() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeTodoPage.kt").readText()

        assertTrue(source.contains("contentType = { it.contentType }"))
    }

    @Test
    fun notePreviewsAreRetainedByViewModelAndBuiltOffMainThread() {
        val viewModel = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/home/HomeViewModel.kt"
        ).readText()
        val grid = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/home/note/NoteGridPage.kt"
        ).readText()

        assertTrue(viewModel.contains("val noteUiState: StateFlow<HomeNotesUiState>"))
        assertTrue(viewModel.contains("withContext(Dispatchers.Default)"))
        assertTrue(viewModel.contains("SharingStarted.Eagerly"))
        assertFalse(grid.contains("NoteGridItemMemoizer()"))
        assertFalse(grid.contains("remember(notes)"))
    }

    @Test
    fun releaseBuildShipsAnAppBaselineProfile() {
        val profile = sourceFile("app/src/main/baseline-prof.txt").readText()
        val build = sourceFile("app/build.gradle.kts").readText()

        assertFalse(profile.contains("HSPLcom/zahri/lighttodo/**->**(**)**"))
        assertTrue(profile.contains("HSPLcom/zahri/lighttodo/feature/home/**->**(**)**"))
        assertTrue(profile.contains("HSPLcom/zahri/lighttodo/feature/settings/**->**(**)**"))
        assertTrue(profile.contains("HSPLcom/zahri/lighttodo/ui/motion/**->**(**)**"))
        assertTrue(build.contains(
            "implementation(\"androidx.profileinstaller:profileinstaller:1.3.1\")"
        ))
    }

    @Test
    fun homePagerStaysLazyWhenAccessibilityIsActive() {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/home/HomeScreen.kt"
        ).readText()

        assertFalse(source.contains("beyondViewportPageCount"))
        assertTrue(source.contains("pagerState.animateScrollToPage(index)"))
    }

    @Test
    fun settingsNavigationWaitsForThePagerAnimationToSettle() {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/home/HomeScreen.kt"
        ).readText()

        assertTrue(source.contains("settingsNavigationPending"))
        assertTrue(source.contains("snapshotFlow { pagerState.isScrollInProgress }"))
        assertTrue(source.contains("first { inProgress -> !inProgress }"))
        assertTrue(source.contains("enabled = !settingsNavigationPending"))
        assertTrue(source.contains("userScrollEnabled = !settingsNavigationPending"))
    }

    @Test
    fun addNoteButtonReadsGlobalBoundsOnlyWhenClicked() {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/home/HomeScreen.kt"
        ).readText()

        assertFalse(source.contains("onGloballyPositioned"))
        assertTrue(source.contains(".onPlaced(noteSourceCoordinates::update)"))
        assertTrue(source.contains("noteSourceCoordinates.boundsInRootOrNull()"))
    }
}

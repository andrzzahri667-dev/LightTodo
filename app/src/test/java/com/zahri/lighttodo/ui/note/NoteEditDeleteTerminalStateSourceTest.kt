package com.zahri.lighttodo.feature.noteeditor

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteEditDeleteTerminalStateSourceTest {
    @Test
    fun successfulDeleteMakesLaterSaveRequestsNoOpBeforeNavigation() {
        val viewModelSource = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditViewModel.kt"
        ).readText()
        val screenSource = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditScreen.kt"
        ).readText()

        val deletedFlag = viewModelSource.indexOf("private var deleteCompleted = false")
        val saveStart = viewModelSource.indexOf("fun save()")
        val saveGuard = viewModelSource.indexOf("if (deleteCompleted) return", saveStart)
        val activeSaveCheck = viewModelSource.indexOf("if (saveJob?.isActive == true)", saveStart)
        val deleteStart = viewModelSource.indexOf("fun delete(onDone: () -> Unit)")
        val repositoryDelete = viewModelSource.indexOf("noteUseCases.deleteNote.delete(", deleteStart)
        val terminalMark = viewModelSource.indexOf("deleteCompleted = true", repositoryDelete)
        val navigationCallback = viewModelSource.indexOf("onDone()", terminalMark)

        assertTrue("the editor needs a deleted terminal flag", deletedFlag >= 0)
        assertTrue(
            "save must stop before scheduling or repeating persistence after deletion",
            saveStart >= 0 && saveGuard > saveStart && saveGuard < activeSaveCheck
        )
        assertTrue(
            "deletion becomes terminal only after the repository delete succeeds",
            deleteStart >= 0 && repositoryDelete > deleteStart && terminalMark > repositoryDelete
        )
        assertTrue(
            "deletion must become terminal before navigation can dispose the screen",
            navigationCallback > terminalMark
        )
        assertTrue(
            "ordinary screen disposal must retain its save request",
            screenSource.contains("onDispose {\n            vm.save()\n        }")
        )
    }

    @Test
    fun backNavigationWaitsForTheCurrentNoteSave() {
        val viewModelSource = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditViewModel.kt"
        ).readText()
        val screenSource = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditScreen.kt"
        ).readText()

        assertTrue(viewModelSource.contains("suspend fun flushAndAwait()"))
        assertTrue(viewModelSource.contains("saveJob?.join()"))
        val leaveBody = screenSource
            .substringAfter("fun leaveNote()")
            .substringBefore("LaunchedEffect(Unit)")
        val flush = leaveBody.indexOf("vm.flushAndAwait()")
        val navigate = leaveBody.indexOf("onBack()")
        assertTrue(leaveBody.contains("scope.launch"))
        assertTrue(flush >= 0 && flush < navigate)
    }
}

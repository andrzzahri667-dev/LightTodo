package com.zahri.lighttodo.feature.home

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelCompletionSourceTest {
    @Test
    fun completionCommitsBeforeAnimationDelay() {
        val completionBranch = completionBranchSource()
        val commitIndex = completionBranch.indexOf("completionSequencer.submit(id, true)")
        val joinIndex = completionBranch.indexOf("mutation.join()")
        val delayIndex = completionBranch.indexOf("kotlinx.coroutines.delay(CompletionFeedbackMillis)")

        assertTrue("completion call is missing", commitIndex >= 0)
        assertTrue("completion join is missing", joinIndex > commitIndex)
        assertTrue("animation delay is missing", delayIndex >= 0)
        assertTrue(
            "completion must commit before the animation delay",
            joinIndex < delayIndex
        )
    }

    @Test
    fun completionPendingIdIsClearedInFinally() {
        val completionBranch = completionBranchSource()
        val tryIndex = completionBranch.indexOf("try {")
        val commitIndex = completionBranch.indexOf("completionSequencer.submit(id, true)")
        val joinIndex = completionBranch.indexOf("mutation.join()")
        val delayIndex = completionBranch.indexOf("kotlinx.coroutines.delay(CompletionFeedbackMillis)")
        val finallyIndex = completionBranch.indexOf("finally {")
        val finallyBody = completionBranch
            .substringAfter("finally {")
            .substringBefore("}")

        assertTrue("completion must be submitted before feedback", commitIndex in 0 until tryIndex)
        assertTrue("completion join must be inside try", joinIndex in tryIndex until delayIndex)
        assertTrue("animation delay must follow completion", commitIndex < delayIndex)
        assertTrue("completion pending cleanup must use finally", finallyIndex > delayIndex)
        assertTrue(
            "only current feedback may remove the pending id",
            finallyBody.contains("completionFeedbackTokens[id] === feedbackToken")
        )
    }

    @Test
    fun completionFeedbackIsShortAndUndoInterruptsIt() {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/home/HomeViewModel.kt"
        ).readText()
        val completionBranch = completionBranchSource()
        val undoBranch = completionBranch
            .substringAfter("if (!done) {")
            .substringBefore("return")
        val cancelIndex = undoBranch.indexOf("_pendingCompleteIds.value = _pendingCompleteIds.value - id")
        val undoIndex = undoBranch.indexOf("completionSequencer.submit(id, false)")

        assertTrue(source.contains("private const val CompletionFeedbackMillis = 240L"))
        assertTrue(undoBranch.contains("completionFeedbackTokens.remove(id)"))
        assertTrue("undo must cancel pending feedback", cancelIndex >= 0)
        assertTrue("undo commit is missing", undoIndex >= 0)
        assertTrue("pending feedback must be cancelled before undo", cancelIndex < undoIndex)
    }

    private fun completionBranchSource(): String {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/home/HomeViewModel.kt"
        ).readText()
        return source
            .substringAfter("fun toggleDone(id: Long, done: Boolean)")
            .substringBefore("fun setGroupExpanded")
    }
}

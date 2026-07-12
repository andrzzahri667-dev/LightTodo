package com.zahri.lighttodo.feature.home

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelCompletionSourceTest {
    @Test
    fun completionCommitsBeforeAnimationDelay() {
        val completionBranch = completionBranchSource()
        val commitIndex = completionBranch.indexOf("completeTodo(id, true)")
        val delayIndex = completionBranch.indexOf("kotlinx.coroutines.delay(320)")

        assertTrue("completion call is missing", commitIndex >= 0)
        assertTrue("animation delay is missing", delayIndex >= 0)
        assertTrue(
            "completion must commit before the animation delay",
            commitIndex < delayIndex
        )
    }

    @Test
    fun completionPendingIdIsClearedInFinally() {
        val completionBranch = completionBranchSource()
        val tryIndex = completionBranch.indexOf("try {")
        val commitIndex = completionBranch.indexOf("completeTodo(id, true)")
        val delayIndex = completionBranch.indexOf("kotlinx.coroutines.delay(320)")
        val finallyIndex = completionBranch.indexOf("finally {")
        val finallyBody = completionBranch
            .substringAfter("finally {")
            .substringBefore("}")

        assertTrue("completion must be inside try", tryIndex in 0 until commitIndex)
        assertTrue("animation delay must follow completion", commitIndex < delayIndex)
        assertTrue("completion pending cleanup must use finally", finallyIndex > delayIndex)
        assertTrue(
            "pending id must be removed inside finally",
            finallyBody.contains("_pendingCompleteIds.value = _pendingCompleteIds.value - id")
        )
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

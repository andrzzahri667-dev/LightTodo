package com.zahri.lighttodo.ui.reminder

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderCompletionLifecycleSourceTest {
    @Test
    fun reminderWaitsForCompletionBeforeFinishingAndRejectsDuplicateClicks() {
        val activitySource = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/reminder/ReminderActivity.kt"
        ).readText()
        val viewModelSource = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/reminder/ReminderViewModel.kt"
        ).readText()

        assertTrue(viewModelSource.contains("suspend fun complete(todoId: Long)"))
        assertTrue(viewModelSource.contains("withContext(NonCancellable + Dispatchers.IO)"))
        assertFalse(viewModelSource.contains("viewModelScope.launch"))
        assertTrue(activitySource.contains("var completing by remember { mutableStateOf(false) }"))
        assertTrue(activitySource.contains("enabled = !completing"))

        val completeCall = activitySource.indexOf("reminderViewModel.complete(todoId)")
        val finishCall = activitySource.indexOf("finish()", completeCall)
        val launchCall = activitySource.lastIndexOf("lifecycleScope.launch", completeCall)
        assertTrue(launchCall >= 0 && launchCall < completeCall)
        assertTrue(completeCall >= 0 && completeCall < finishCall)
    }
}

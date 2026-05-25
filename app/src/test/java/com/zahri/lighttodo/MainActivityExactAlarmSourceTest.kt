package com.zahri.lighttodo

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityExactAlarmSourceTest {

    @Test
    fun exactAlarmSettingsAreNotOpenedDuringStartup() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/MainActivity.kt")
            .readText()
        val onCreateStart = source.indexOf("override fun onCreate")
        val setContentStart = source.indexOf("setContent", onCreateStart)
        val startupBody = source.substring(onCreateStart, setContentStart)

        assertFalse(startupBody.contains("ACTION_REQUEST_SCHEDULE_EXACT_ALARM"))
    }

    @Test
    fun editSaveCanRequestExactAlarmSettingsWhenTodoHasReminder() {
        val activitySource = sourceFile("app/src/main/java/com/zahri/lighttodo/MainActivity.kt")
            .readText()
        val editSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/edit/EditScreen.kt")
            .readText()

        assertTrue(activitySource.contains("onRequestExactAlarmPermission"))
        assertTrue(activitySource.contains("ExactAlarmPermissionPolicy.shouldRequestSettings"))
        assertTrue(editSource.contains("onRequestExactAlarmPermission(state.hasReminder)"))
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

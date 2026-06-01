package com.zahri.lighttodo

import com.zahri.lighttodo.test.sourceFile

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
}

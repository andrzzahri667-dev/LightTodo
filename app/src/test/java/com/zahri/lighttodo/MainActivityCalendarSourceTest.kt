package com.zahri.lighttodo

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertFalse
import org.junit.Test

class MainActivityCalendarSourceTest {
    @Test
    fun mainActivityDoesNotTurnOnCalendarSyncFromPermissionState() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/MainActivity.kt").readText()

        assertFalse(source.contains("enableCalendarSync()"))
        assertFalse(source.contains("setCalendarSyncEnabled(true)"))
        assertFalse(source.contains("hasCalendarPermission()"))
    }
}

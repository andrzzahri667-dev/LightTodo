package com.zahri.lighttodo

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCalendarSyncSourceTest {
    @Test
    fun calendarObserverIsNotRegisteredWithoutCalendarPermission() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/App.kt").readText()
        val permissionGuard = source.indexOf("if (!hasCalendarPermissions()) {")
        val registerObserver = source.indexOf("CalendarObserver.register(this@App)")
        val guardedBlock = source.substring(permissionGuard, registerObserver)

        assertTrue(source.contains("private fun hasCalendarPermissions(): Boolean"))
        assertTrue(permissionGuard >= 0)
        assertTrue(registerObserver >= 0)
        assertTrue(permissionGuard < registerObserver)
        assertTrue(guardedBlock.contains("stopCalendarSync()"))
        assertTrue(guardedBlock.contains("return@collect"))
    }
}

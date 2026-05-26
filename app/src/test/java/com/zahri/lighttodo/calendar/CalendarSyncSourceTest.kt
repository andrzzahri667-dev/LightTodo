package com.zahri.lighttodo.calendar

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarSyncSourceTest {

    @Test
    fun orphanCleanupOnlyConsidersUndoneImportedEventsInsideSyncWindow() {
        val syncSource = sourceFile("app/src/main/java/com/zahri/lighttodo/calendar/CalendarSync.kt")
            .readText()
        val daoSource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/Daos.kt")
            .readText()

        assertTrue(syncSource.contains("listUndoneCalendarEventIdsInWindow(from, to)"))
        assertFalse(syncSource.contains("listCalendarEventIds()"))
        assertTrue(daoSource.contains("done = 0"))
        assertTrue(daoSource.contains("dateMillis >= :fromMillis"))
        assertTrue(daoSource.contains("dateMillis <= :toMillis"))
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

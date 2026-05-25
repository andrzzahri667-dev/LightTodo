package com.zahri.lighttodo.notify

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class QuickAddServiceSourceTest {
    @Test
    fun foregroundStartHandlesNotificationPermissionFailure() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/notify/QuickAddService.kt").readText()

        assertTrue(source.contains("catch (_: SecurityException)"))
        assertTrue(source.contains("stopSelf(startId)"))
        assertTrue(source.contains("START_NOT_STICKY"))
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

package com.zahri.lighttodo.notify

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAddServiceSourceTest {
    @Test
    fun foregroundStartHandlesNotificationPermissionFailure() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/notification/QuickAddService.kt").readText()

        assertTrue(source.contains("catch (_: SecurityException)"))
        assertTrue(source.contains("stopSelf(startId)"))
        assertTrue(source.contains("START_NOT_STICKY"))
    }
}

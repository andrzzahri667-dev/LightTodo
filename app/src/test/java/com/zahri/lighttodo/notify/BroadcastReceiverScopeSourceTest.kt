package com.zahri.lighttodo.notify

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BroadcastReceiverScopeSourceTest {
    @Test
    fun bootReceiverUsesApplicationScopeForAsyncWork() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/notify/BootReceiver.kt").readText()

        assertFalse(source.contains("CoroutineScope(Dispatchers.IO)"))
        assertTrue(source.contains("app.appScope.launch(Dispatchers.IO)"))
    }

    @Test
    fun reminderReceiverUsesApplicationScopeForAsyncWork() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/notify/ReminderReceiver.kt").readText()

        assertFalse(source.contains("CoroutineScope(Dispatchers.IO)"))
        assertTrue(source.contains("app.appScope.launch(Dispatchers.IO)"))
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

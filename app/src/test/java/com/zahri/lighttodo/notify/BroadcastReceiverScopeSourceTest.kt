package com.zahri.lighttodo.notify

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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

    @Test
    fun reminderReceiverFallsBackWhenFullScreenIntentIsUnavailable() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/notify/ReminderReceiver.kt").readText()

        assertTrue(source.contains("ReminderFullScreenPolicy.shouldAttachFullScreenIntent"))
        assertTrue(source.contains("if (attachFullScreenIntent)"))
        assertTrue(source.contains("setFullScreenIntent(fullScreenPi, true)"))
    }
}

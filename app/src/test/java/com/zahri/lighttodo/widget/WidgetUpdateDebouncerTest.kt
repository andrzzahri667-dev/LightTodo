package com.zahri.lighttodo.integration.widget

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetUpdateDebouncerTest {

    @Test
    fun submitCoalescesRapidRequestsIntoOneUpdate() = runBlocking {
        val updates = AtomicInteger(0)
        val debouncer = WidgetUpdateDebouncer(
            dispatcher = Dispatchers.Default,
            delayMillis = 60L
        )

        debouncer.submit(this) { updates.incrementAndGet() }
        delay(20L)
        debouncer.submit(this) { updates.incrementAndGet() }
        delay(20L)
        debouncer.submit(this) { updates.incrementAndGet() }

        delay(120L)

        assertEquals(1, updates.get())
    }
}

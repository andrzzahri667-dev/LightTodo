package com.zahri.lighttodo.widget

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class WidgetUpdateDebouncer(
    private val dispatcher: CoroutineDispatcher,
    private val delayMillis: Long
) {
    private val lock = Any()
    private var pendingJob: Job? = null

    fun submit(scope: CoroutineScope, block: suspend () -> Unit) {
        synchronized(lock) {
            pendingJob?.cancel()
            pendingJob = scope.launch(dispatcher) {
                delay(delayMillis)
                block()
            }
        }
    }
}

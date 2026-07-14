package com.zahri.lighttodo.feature.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class TodoCompletionSequencer(
    private val scope: CoroutineScope,
    private val complete: suspend (id: Long, done: Boolean) -> Unit
) {
    private val lock = Any()
    private val tails = mutableMapOf<Long, Job>()

    fun submit(id: Long, done: Boolean): Job {
        lateinit var current: Job
        synchronized(lock) {
            val previous = tails[id]
            current = scope.launch(start = CoroutineStart.LAZY) {
                previous?.join()
                complete(id, done)
            }
            tails[id] = current
        }
        current.invokeOnCompletion {
            synchronized(lock) {
                if (tails[id] === current) tails.remove(id)
            }
        }
        current.start()
        return current
    }
}

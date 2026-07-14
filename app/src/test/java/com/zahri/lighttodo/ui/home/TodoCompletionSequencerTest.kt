package com.zahri.lighttodo.feature.home

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoCompletionSequencerTest {

    @Test
    fun sameTodoMutationsRunInSubmissionOrder() = runBlocking {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        var invocation = 0
        val sequencer = TodoCompletionSequencer(this) { _, done ->
            invocation += 1
            events += "start:$done"
            if (invocation == 1) {
                firstStarted.complete(Unit)
                releaseFirst.await()
            }
            events += "finish:$done"
        }

        val first = sequencer.submit(id = 7L, done = true)
        firstStarted.await()
        val second = sequencer.submit(id = 7L, done = false)
        val third = sequencer.submit(id = 7L, done = true)

        assertEquals(listOf("start:true"), events)
        releaseFirst.complete(Unit)
        joinAll(first, second, third)

        assertEquals(
            listOf(
                "start:true",
                "finish:true",
                "start:false",
                "finish:false",
                "start:true",
                "finish:true"
            ),
            events
        )
    }
}

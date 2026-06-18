package com.zahri.lighttodo.integration.widget

import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks which items are in the "struck-through" visual state after the user
 * taps the checkbox. The state is a simple flag — no continuous animation.
 */
object WidgetAnimation {

    data class AnimState(
        val rowIndex: Int
    )

    private val states = ConcurrentHashMap<Long, AnimState>()

    fun start(id: Long, rowIndex: Int) {
        states[id] = AnimState(rowIndex)
    }

    fun stateOf(id: Long): AnimState? = states[id]

    fun isAnimating(id: Long): Boolean = states.containsKey(id)

    fun clear(id: Long) {
        states.remove(id)
    }
}

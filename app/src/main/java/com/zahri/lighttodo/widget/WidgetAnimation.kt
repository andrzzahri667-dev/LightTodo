package com.zahri.lighttodo.widget

import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks per-item animation state for the widget tap-to-complete sequence.
 *
 * Because [android.widget.RemoteViews] cannot run real animations, we fake
 * a 3-stage transition by keeping the row in the data set and re-rendering
 * it differently while the animation plays out.
 *
 * Lifecycle: state lives in process memory only. If the process is killed
 * mid-animation, the next refresh will simply mark the todo as done and
 * remove it (no orphaned visuals because the state map is empty on cold start).
 */
object WidgetAnimation {

    /** A single item can be in one of these visual states during the tap animation. */
    enum class Stage {
        /** Stage 1: yellow filled checkbox with a white tick. Title unchanged. */
        CHECK_ONLY,

        /** Stage 2: yellow checkbox + yellow strikethrough line over the title. */
        STRIKE
    }

    private val states = ConcurrentHashMap<Long, Stage>()

    fun setStage(id: Long, stage: Stage) {
        states[id] = stage
    }

    fun stageOf(id: Long): Stage? = states[id]

    fun isAnimating(id: Long): Boolean = id in states

    fun clear(id: Long) {
        states.remove(id)
    }
}

package com.zahri.lighttodo.ui.edit

object WheelPickerMotionPolicy {
    fun proximityForDistance(distancePx: Float, radiusPx: Float): Float {
        if (radiusPx <= 0f) return 0f
        return (1f - kotlin.math.abs(distancePx) / radiusPx).coerceIn(0f, 1f)
    }

    fun scaleForProximity(proximity: Float): Float =
        1f + proximity.coerceIn(0f, 1f) * 0.28f

    fun alphaForProximity(proximity: Float): Float =
        0.42f + proximity.coerceIn(0f, 1f) * 0.58f
}

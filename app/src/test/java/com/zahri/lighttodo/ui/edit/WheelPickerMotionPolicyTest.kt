package com.zahri.lighttodo.ui.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelPickerMotionPolicyTest {
    @Test
    fun proximityFallsOffContinuouslyWithDistanceFromCenter() {
        val radius = 120f

        assertEquals(1f, WheelPickerMotionPolicy.proximityForDistance(0f, radius), 0.0001f)
        assertEquals(0.5f, WheelPickerMotionPolicy.proximityForDistance(60f, radius), 0.0001f)
        assertEquals(0f, WheelPickerMotionPolicy.proximityForDistance(120f, radius), 0.0001f)
        assertEquals(0f, WheelPickerMotionPolicy.proximityForDistance(220f, radius), 0.0001f)
    }

    @Test
    fun scaleAndAlphaKeepNearbyItemsResponsiveButReadable() {
        val farScale = WheelPickerMotionPolicy.scaleForProximity(0f)
        val nearScale = WheelPickerMotionPolicy.scaleForProximity(0.5f)
        val centerScale = WheelPickerMotionPolicy.scaleForProximity(1f)
        val farAlpha = WheelPickerMotionPolicy.alphaForProximity(0f)
        val nearAlpha = WheelPickerMotionPolicy.alphaForProximity(0.5f)
        val centerAlpha = WheelPickerMotionPolicy.alphaForProximity(1f)

        assertTrue(farScale < nearScale)
        assertTrue(nearScale < centerScale)
        assertTrue(farAlpha < nearAlpha)
        assertTrue(nearAlpha < centerAlpha)
        assertEquals(1f, farScale, 0.0001f)
        assertTrue(centerScale in 1.22f..1.34f)
        assertTrue(nearScale >= 1.10f)
        assertTrue(farAlpha in 0.38f..0.48f)
        assertTrue(nearAlpha >= 0.65f)
        assertEquals(1f, centerAlpha, 0.0001f)
    }
}

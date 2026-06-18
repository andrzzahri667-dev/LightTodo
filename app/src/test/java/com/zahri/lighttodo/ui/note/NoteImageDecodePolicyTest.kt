package com.zahri.lighttodo.feature.noteeditor

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteImageDecodePolicyTest {
    @Test
    fun sampleSizeFor_keepsDecodedBitmapWithinDoubleTargetBounds() {
        assertEquals(
            4,
            NoteImageDecodePolicy.sampleSizeFor(
                sourceWidth = 4000,
                sourceHeight = 3000,
                targetWidth = 500,
                targetHeight = 375
            )
        )
    }

    @Test
    fun sampleSizeFor_keepsReasonableResolutionForNearTargetImages() {
        assertEquals(
            2,
            NoteImageDecodePolicy.sampleSizeFor(
                sourceWidth = 4000,
                sourceHeight = 3000,
                targetWidth = 1000,
                targetHeight = 750
            )
        )
    }

    @Test
    fun sampleSizeFor_doesNotUpsampleOrCrashOnInvalidBounds() {
        assertEquals(
            1,
            NoteImageDecodePolicy.sampleSizeFor(
                sourceWidth = 800,
                sourceHeight = 600,
                targetWidth = 1000,
                targetHeight = 750
            )
        )
        assertEquals(
            1,
            NoteImageDecodePolicy.sampleSizeFor(
                sourceWidth = 0,
                sourceHeight = 600,
                targetWidth = 500,
                targetHeight = 375
            )
        )
        assertEquals(
            1,
            NoteImageDecodePolicy.sampleSizeFor(
                sourceWidth = 4000,
                sourceHeight = 3000,
                targetWidth = 0,
                targetHeight = 0
            )
        )
    }
}

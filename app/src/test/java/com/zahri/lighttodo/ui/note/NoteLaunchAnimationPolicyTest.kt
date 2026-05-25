package com.zahri.lighttodo.ui.note

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteLaunchAnimationPolicyTest {
    @Test
    fun specFor_newNoteUsesRoundedViewAnimationWithoutCapturingFabIcon() {
        val spec = NoteLaunchAnimationPolicy.specFor(
            noteId = null,
            sourceWidthPx = 168f,
            density = 3f
        )

        assertEquals(NoteLaunchSnapshotMode.SolidColor, spec.snapshotMode)
        assertEquals(NoteLaunchAnimationPolicy.AnimLaunchActivityFromRoundedView, spec.miuiAnimationType)
        assertEquals(84, spec.sourceCornerRadiusPx)
    }

    @Test
    fun specFor_existingNoteUsesScaledThumbAnimationFromCardSnapshot() {
        val spec = NoteLaunchAnimationPolicy.specFor(
            noteId = 42L,
            sourceWidthPx = 480f,
            density = 3f
        )

        assertEquals(NoteLaunchSnapshotMode.CaptureSource, spec.snapshotMode)
        assertEquals(NoteLaunchAnimationPolicy.AnimLaunchActivityWithScaledThumb, spec.miuiAnimationType)
        assertEquals(36, spec.sourceCornerRadiusPx)
    }

    @Test
    fun specFor_invalidDensityFallsBackToSafeCardRadius() {
        val spec = NoteLaunchAnimationPolicy.specFor(
            noteId = 42L,
            sourceWidthPx = 480f,
            density = 0f
        )

        assertEquals(12, spec.sourceCornerRadiusPx)
    }

    @Test
    fun specFor_usesCallerScaleSoPressedSourceKeepsMiuiBreathingMotion() {
        val spec = NoteLaunchAnimationPolicy.specFor(
            noteId = null,
            sourceWidthPx = 168f,
            density = 3f,
            sourceScale = 0.92f
        )

        assertEquals(0.92f, spec.sourceScale, 0.0001f)
    }
}

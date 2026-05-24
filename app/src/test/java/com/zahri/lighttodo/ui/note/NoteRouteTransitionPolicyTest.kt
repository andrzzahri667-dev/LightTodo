package com.zahri.lighttodo.ui.note

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteRouteTransitionPolicyTest {
    @Test
    fun specFor_movesRouteCenterFromSourceCenter() {
        val spec = NoteRouteTransitionPolicy.specFor(
            sourceBounds = Rect(left = 20f, top = 100f, right = 180f, bottom = 260f),
            rootSize = IntSize(width = 400, height = 800)
        )

        assertEquals(TransformOrigin.Center.pivotFractionX, spec.transformOrigin.pivotFractionX, 0.0001f)
        assertEquals(TransformOrigin.Center.pivotFractionY, spec.transformOrigin.pivotFractionY, 0.0001f)
        assertEquals(IntOffset(x = -100, y = -220), spec.sourceCenterOffset)
        assertEquals(IntSize(width = 160, height = 160), spec.sourceSize)
        assertEquals(true, spec.hasSourceBounds)
        assertEquals(0.28f, spec.sourceScale, 0.01f)
    }

    @Test
    fun specFor_keepsFabLaunchVisibleWithMinimumScale() {
        val spec = NoteRouteTransitionPolicy.specFor(
            sourceBounds = Rect(left = 320f, top = 700f, right = 376f, bottom = 756f),
            rootSize = IntSize(width = 400, height = 800)
        )

        assertEquals(TransformOrigin.Center.pivotFractionX, spec.transformOrigin.pivotFractionX, 0.0001f)
        assertEquals(TransformOrigin.Center.pivotFractionY, spec.transformOrigin.pivotFractionY, 0.0001f)
        assertEquals(IntOffset(x = 148, y = 328), spec.sourceCenterOffset)
        assertEquals(IntSize(width = 56, height = 56), spec.sourceSize)
        assertEquals(true, spec.hasSourceBounds)
        assertEquals(0.16f, spec.sourceScale, 0.0001f)
    }

    @Test
    fun specFor_fallsBackToGentleCenterScaleWithoutUsableBounds() {
        val spec = NoteRouteTransitionPolicy.specFor(
            sourceBounds = null,
            rootSize = IntSize(width = 0, height = 800)
        )

        assertEquals(TransformOrigin.Center.pivotFractionX, spec.transformOrigin.pivotFractionX, 0.0001f)
        assertEquals(TransformOrigin.Center.pivotFractionY, spec.transformOrigin.pivotFractionY, 0.0001f)
        assertEquals(IntOffset.Zero, spec.sourceCenterOffset)
        assertEquals(IntSize.Zero, spec.sourceSize)
        assertEquals(false, spec.hasSourceBounds)
        assertEquals(0.985f, spec.sourceScale, 0.0001f)
    }

    @Test
    fun exitBehaviorForTarget_keepsBackgroundVisibleWhenOpeningNote() {
        assertEquals(
            NoteRouteBackgroundBehavior.KeepVisible,
            NoteRouteTransitionPolicy.exitBehaviorForTarget(
                targetRoute = "note_edit?id={id}",
                noteRoute = "note_edit?id={id}"
            )
        )
    }

    @Test
    fun popEnterBehaviorForInitial_keepsBackgroundVisibleWhenClosingNote() {
        assertEquals(
            NoteRouteBackgroundBehavior.KeepVisible,
            NoteRouteTransitionPolicy.popEnterBehaviorForInitial(
                initialRoute = "note_edit?id={id}",
                noteRoute = "note_edit?id={id}"
            )
        )
    }

    @Test
    fun backgroundBehavior_usesStandardSlideForNonNoteRoutes() {
        assertEquals(
            NoteRouteBackgroundBehavior.StandardSlide,
            NoteRouteTransitionPolicy.exitBehaviorForTarget(
                targetRoute = "settings",
                noteRoute = "note_edit?id={id}"
            )
        )
        assertEquals(
            NoteRouteBackgroundBehavior.StandardSlide,
            NoteRouteTransitionPolicy.popEnterBehaviorForInitial(
                initialRoute = "settings",
                noteRoute = "note_edit?id={id}"
            )
        )
    }
}

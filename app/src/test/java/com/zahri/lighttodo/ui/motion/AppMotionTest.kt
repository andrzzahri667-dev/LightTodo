package com.zahri.lighttodo.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMotionTest {
    @Test
    fun routeDurations_keepNoteMotionComfortableAndBounded() {
        assertEquals(400, AppMotion.RouteDurationMillis)
        assertEquals(360, AppMotion.NoteEnterTransformMillis)
        assertEquals(300, AppMotion.NoteExitTransformMillis)
        assertTrue(AppMotion.NoteExitTransformMillis < AppMotion.NoteEnterTransformMillis)
        assertTrue(AppMotion.NoteEnterTransformMillis <= AppMotion.RouteDurationMillis)
    }

    @Test
    fun transientSurfaceDurations_stayShorterThanRouteTransitions() {
        assertTrue(AppMotion.SurfaceVisibilityFadeMillis in 90..180)
        assertTrue(AppMotion.SurfaceVisibilitySlideMillis in 140..220)
        assertTrue(AppMotion.SurfaceVisibilitySlideMillis < AppMotion.RouteDurationMillis)
    }

    @Test
    fun pressSpring_isSnappyButNotOverlyBouncy() {
        assertTrue(AppMotion.PressSpringDampingRatio in 0.5f..0.75f)
        assertTrue(AppMotion.PressSpringStiffness in 650f..900f)
    }

    @Test
    fun listAndCompletionMotion_keepsRepeatedInteractionsTight() {
        assertTrue(AppMotion.ListPlacementStiffness in 420f..560f)
        assertTrue(AppMotion.SectionArrowStiffness in 420f..560f)
        assertTrue(AppMotion.CheckmarkFadeMillis <= 190)
        assertTrue(AppMotion.TodoContentSettleMillis <= 240)
        assertTrue(AppMotion.SelectionColorMillis <= 180)
    }

    @Test
    fun noteGridMotion_handlesCardReorderWithoutFeelingSlow() {
        assertTrue(AppMotion.NoteGridPlacementStiffness in 420f..560f)
        assertTrue(AppMotion.NoteGridPlacementDampingRatio in 0.78f..0.92f)
        assertTrue(AppMotion.NoteCardSelectionColorMillis <= 180)
    }

    @Test
    fun noteEditorContentMotion_isFastEnoughForTypingContext() {
        assertTrue(AppMotion.NoteContentSizeDampingRatio in 0.78f..0.92f)
        assertTrue(AppMotion.NoteContentSizeStiffness in 420f..560f)
    }
}

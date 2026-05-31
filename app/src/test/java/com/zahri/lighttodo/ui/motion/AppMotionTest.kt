package com.zahri.lighttodo.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMotionTest {
    @Test
    fun routeDurations_keepAppNavigationComfortableAndBounded() {
        assertEquals(400, AppMotion.RouteDurationMillis)
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
        assertTrue(AppMotion.SectionItemFadeInMillis in 120..220)
        assertTrue(AppMotion.SectionItemFadeOutMillis in 90..180)
        assertTrue(AppMotion.SectionItemFadeOutMillis < AppMotion.SectionItemFadeInMillis)
        assertTrue(AppMotion.TodoContentSettleMillis <= 240)
        assertTrue(AppMotion.SelectionColorMillis <= 180)
    }

    @Test
    fun pickerItemMotion_isCentralizedAndQuick() {
        assertTrue(AppMotion.PickerItemAlphaMillis in 120..180)
        assertTrue(AppMotion.PickerItemScaleMillis in 120..180)
        assertTrue(AppMotion.PickerItemProximityRadiusItems in 2.0f..2.8f)
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

    @Test
    fun noteSourceResetDelay_isCentralizedWithMotionTiming() {
        assertEquals(1_200L, AppMotion.NoteSourceAnimationResetDelayMillis)
    }
}

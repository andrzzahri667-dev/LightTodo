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
    fun pressSpring_isSubtleAndCriticallyDamped() {
        assertEquals(1f, AppMotion.PressSpringDampingRatio, 0.0001f)
        assertTrue(AppMotion.PressSpringStiffness in 650f..900f)
        assertEquals(0.97f, AppMotion.PressScale, 0.0001f)
    }

    @Test
    fun listAndCompletionMotion_keepsRepeatedInteractionsTight() {
        assertTrue(AppMotion.ListPlacementStiffness in 420f..560f)
        assertTrue(AppMotion.SectionArrowStiffness in 420f..560f)
        assertEquals(180f, AppMotion.SectionExpandedRotationDegrees, 0.0001f)
        assertTrue(AppMotion.SectionItemFadeInMillis in 120..220)
        assertTrue(AppMotion.SectionItemFadeOutMillis in 90..180)
        assertTrue(AppMotion.SectionItemFadeOutMillis < AppMotion.SectionItemFadeInMillis)
        assertTrue(AppMotion.TodoContentSettleMillis <= 240)
        assertTrue(AppMotion.TodoCompletedContentAlpha in 0.4f..0.55f)
        assertTrue(AppMotion.TodoCompletedContentTranslationX in 4f..8f)
        assertTrue(AppMotion.SelectionColorMillis <= 180)
    }

    @Test
    fun pickerItemMotion_usesACompactDirectManipulationRadius() {
        assertTrue(AppMotion.PickerItemProximityRadiusItems in 2.0f..2.8f)
    }

    @Test
    fun noteGridMotion_handlesCardReorderWithoutFeelingSlow() {
        assertTrue(AppMotion.NoteGridPlacementStiffness in 420f..560f)
        assertTrue(AppMotion.NoteGridPlacementDampingRatio in 0.78f..0.92f)
        assertTrue(AppMotion.NoteCardSelectionColorMillis <= 180)
    }

    @Test
    fun noteSourceResetDelay_isCentralizedWithMotionTiming() {
        assertEquals(1_200L, AppMotion.NoteSourceAnimationResetDelayMillis)
    }
}

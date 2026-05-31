package com.zahri.lighttodo.ui.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

object AppMotion {
    val StandardEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    const val RouteDurationMillis = 400

    const val SurfaceVisibilityFadeMillis = 140
    const val SurfaceVisibilitySlideMillis = 180

    const val PressSpringDampingRatio = 0.62f
    const val PressSpringStiffness = 760f
    const val PressScale = 0.92f

    const val ListPlacementDampingRatio = 0.86f
    const val ListPlacementStiffness = 500f
    const val NoteGridPlacementDampingRatio = 0.84f
    const val NoteGridPlacementStiffness = 500f
    const val NoteContentSizeDampingRatio = 0.86f
    const val NoteContentSizeStiffness = 500f
    const val SectionArrowDampingRatio = 0.78f
    const val SectionArrowStiffness = 520f
    const val SectionExpandedRotationDegrees = 180f

    const val SelectionColorMillis = 160
    const val NoteCardSelectionColorMillis = 160
    const val SectionItemFadeInMillis = 170
    const val SectionItemFadeOutMillis = 120
    const val PickerItemAlphaMillis = 150
    const val PickerItemScaleMillis = 150
    const val PickerItemProximityRadiusItems = 2.35f
    const val TodoContentSettleMillis = 240
    const val NoteSourceAnimationResetDelayMillis = 1_200L

    fun <T> routeTween(): TweenSpec<T> =
        tween(RouteDurationMillis, easing = StandardEasing)

    fun <T> pressSpring(): SpringSpec<T> =
        spring(dampingRatio = PressSpringDampingRatio, stiffness = PressSpringStiffness)

    fun <T> listPlacementSpring(): SpringSpec<T> =
        spring(dampingRatio = ListPlacementDampingRatio, stiffness = ListPlacementStiffness)

    fun <T> noteGridPlacementSpring(): SpringSpec<T> =
        spring(dampingRatio = NoteGridPlacementDampingRatio, stiffness = NoteGridPlacementStiffness)

    fun <T> noteContentSizeSpring(): SpringSpec<T> =
        spring(dampingRatio = NoteContentSizeDampingRatio, stiffness = NoteContentSizeStiffness)

    fun <T> sectionArrowSpring(): SpringSpec<T> =
        spring(dampingRatio = SectionArrowDampingRatio, stiffness = SectionArrowStiffness)

    fun sectionItemEnter(): EnterTransition =
        fadeIn(tween(SectionItemFadeInMillis, easing = EmphasizedEasing)) +
            expandVertically(tween(SectionItemFadeInMillis, easing = EmphasizedEasing))

    fun sectionItemExit(): ExitTransition =
        fadeOut(tween(SectionItemFadeOutMillis, easing = StandardEasing)) +
            shrinkVertically(tween(SectionItemFadeOutMillis, easing = StandardEasing))

    fun transientSurfaceEnter(): EnterTransition =
        fadeIn(tween(SurfaceVisibilityFadeMillis, easing = EmphasizedEasing)) +
            slideInVertically(tween(SurfaceVisibilitySlideMillis, easing = EmphasizedEasing)) { it / 3 }

    fun transientSurfaceExit(): ExitTransition =
        fadeOut(tween(SurfaceVisibilityFadeMillis, easing = EmphasizedEasing)) +
            slideOutVertically(tween(SurfaceVisibilitySlideMillis, easing = EmphasizedEasing)) { it / 3 }
}

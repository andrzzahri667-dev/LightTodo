package com.zahri.lighttodo.ui.motion.components

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.zahri.lighttodo.ui.motion.AppMotion

fun <S> AnimatedContentTransitionScope<S>.motionRouteEnterTransition(): EnterTransition =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        AppMotion.routeTween()
    )

fun <S> AnimatedContentTransitionScope<S>.motionRouteExitTransition(): ExitTransition =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        AppMotion.routeTween()
    )

fun <S> AnimatedContentTransitionScope<S>.motionRoutePopEnterTransition(): EnterTransition =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        AppMotion.routeTween()
    )

fun <S> AnimatedContentTransitionScope<S>.motionRoutePopExitTransition(): ExitTransition =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        AppMotion.routeTween()
    )

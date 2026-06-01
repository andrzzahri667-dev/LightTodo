package com.zahri.lighttodo.ui.motion.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier
import com.zahri.lighttodo.ui.motion.AppMotion

@OptIn(ExperimentalFoundationApi::class)
fun LazyItemScope.motionSectionListItem(
    modifier: Modifier = Modifier
): Modifier =
    modifier.animateItem(
        fadeInSpec = tween(AppMotion.SectionItemFadeInMillis, easing = AppMotion.EmphasizedEasing),
        placementSpec = AppMotion.listPlacementSpring(),
        fadeOutSpec = tween(AppMotion.SectionItemFadeOutMillis, easing = AppMotion.StandardEasing)
    )

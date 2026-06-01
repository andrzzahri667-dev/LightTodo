package com.zahri.lighttodo.ui.motion.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier
import com.zahri.lighttodo.ui.motion.AppMotion

@OptIn(ExperimentalFoundationApi::class)
fun LazyItemScope.motionSectionItemPlacement(
    modifier: Modifier = Modifier
): Modifier =
    modifier.animateItem(
        fadeInSpec = null,
        placementSpec = AppMotion.listPlacementSpring(),
        fadeOutSpec = null
    )

package com.zahri.lighttodo.ui.motion.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.ui.Modifier
import com.zahri.lighttodo.ui.motion.AppMotion

@OptIn(ExperimentalFoundationApi::class)
fun LazyStaggeredGridItemScope.motionNoteGridItem(
    modifier: Modifier = Modifier
): Modifier =
    modifier.animateItem(
        placementSpec = AppMotion.noteGridPlacementSpring()
    )

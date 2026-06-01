package com.zahri.lighttodo.ui.motion.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState

suspend fun LazyListState.motionWheelPickerScrollToItem(index: Int) {
    animateScrollToItem(index)
}

@OptIn(ExperimentalFoundationApi::class)
suspend fun PagerState.motionHomePagerScrollToPage(page: Int) {
    animateScrollToPage(page)
}

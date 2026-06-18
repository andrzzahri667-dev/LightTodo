package com.zahri.lighttodo.ui.motion

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionScrollSourceTest {
    @Test
    fun scrollCallsUseComposeApisDirectlyWithoutSingleLineMotionWrappers() {
        val wheelSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/edit/WheelPicker.kt").readText()
        val homeSource = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeScreen.kt").readText()

        assertFalse(wheelSource.contains("import com.zahri.lighttodo.ui.motion.components.motionWheelPickerScrollToItem"))
        assertFalse(wheelSource.contains("listState.motionWheelPickerScrollToItem(target)"))
        assertTrue(wheelSource.contains("listState.animateScrollToItem(target)"))
        assertFalse(homeSource.contains("import com.zahri.lighttodo.ui.motion.components.motionHomePagerScrollToPage"))
        assertFalse(homeSource.contains("pagerState.motionHomePagerScrollToPage(index)"))
        assertTrue(homeSource.contains("pagerState.animateScrollToPage(index)"))
        assertFalse(sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components").walk().any {
            it.name == "MotionScroll.kt"
        })
    }
}

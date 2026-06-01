package com.zahri.lighttodo.ui.motion

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionScrollSourceTest {
    @Test
    fun scrollAnimationsAreCentralizedOutsideScreens() {
        val wheelSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/edit/WheelPicker.kt").readText()
        val homeSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/HomeScreen.kt").readText()
        val scrollSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionScroll.kt").readText()

        assertTrue(wheelSource.contains("import com.zahri.lighttodo.ui.motion.components.motionWheelPickerScrollToItem"))
        assertTrue(wheelSource.contains("listState.motionWheelPickerScrollToItem(target)"))
        assertFalse(wheelSource.contains("listState.animateScrollToItem(target)"))
        assertTrue(homeSource.contains("import com.zahri.lighttodo.ui.motion.components.motionHomePagerScrollToPage"))
        assertTrue(homeSource.contains("pagerState.motionHomePagerScrollToPage(index)"))
        assertFalse(homeSource.contains("pagerState.animateScrollToPage(index)"))
        assertTrue(scrollSource.contains("suspend fun LazyListState.motionWheelPickerScrollToItem("))
        assertTrue(scrollSource.contains("animateScrollToItem(index)"))
        assertTrue(scrollSource.contains("suspend fun PagerState.motionHomePagerScrollToPage("))
        assertTrue(scrollSource.contains("animateScrollToPage(page)"))
    }

    private fun sourceFile(relativePath: String): File {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        var dir = File(userDir).absoluteFile
        while (true) {
            val candidate = File(dir, relativePath)
            if (candidate.exists()) return candidate
            dir = dir.parentFile ?: break
        }
        error("Could not find $relativePath from $userDir")
    }
}

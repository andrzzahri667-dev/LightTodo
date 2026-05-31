package com.zahri.lighttodo.ui.edit

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelPickerMotionSourceTest {
    @Test
    fun wheelPickerUsesCentralAppMotionForItemAlpha() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/edit/WheelPicker.kt").readText()

        assertTrue(source.contains("import androidx.compose.animation.core.animateFloatAsState"))
        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.AppMotion"))
        assertTrue(source.contains("AppMotion.PickerItemAlphaMillis"))
        assertFalse(source.contains("import androidx.compose.animation.core.Animatable"))
        assertFalse(source.contains("tween(durationMillis = 150)"))
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

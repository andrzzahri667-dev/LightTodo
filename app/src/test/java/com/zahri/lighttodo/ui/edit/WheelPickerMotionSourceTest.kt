package com.zahri.lighttodo.ui.edit

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelPickerMotionSourceTest {
    @Test
    fun wheelPickerUsesCentralAppMotionForItemAlpha() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/edit/WheelPicker.kt").readText()
        val componentSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/WheelPickerItemMotion.kt").readText()

        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.AppMotion"))
        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.WheelPickerMotionPolicy"))
        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.components.rememberWheelPickerItemMotion"))
        assertTrue(source.contains("WheelPickerMotionPolicy.proximityForDistance"))
        assertTrue(source.contains("val itemMotion = rememberWheelPickerItemMotion(proximity)"))
        assertTrue(source.contains("this.alpha = itemMotion.alpha"))
        assertTrue(source.contains("scaleX = itemMotion.scale"))
        assertTrue(source.contains("scaleY = itemMotion.scale"))
        assertFalse(source.contains("import androidx.compose.animation.core.animateFloatAsState"))
        assertFalse(source.contains("WheelPickerMotionPolicy.scaleForProximity"))
        assertFalse(source.contains("WheelPickerMotionPolicy.alphaForProximity"))
        assertFalse(source.contains("AppMotion.PickerItemAlphaMillis"))
        assertFalse(source.contains("import androidx.compose.animation.core.Animatable"))
        assertFalse(source.contains("package com.zahri.lighttodo.ui.edit\n\nobject WheelPickerMotionPolicy"))
        assertFalse(source.contains("tween(durationMillis = 150)"))
        assertFalse(source.contains("val targetAlpha = if (isSelected) 1f else 0.4f"))
        assertTrue(componentSource.contains("animateFloatAsState"))
        assertTrue(componentSource.contains("WheelPickerMotionPolicy.alphaForProximity"))
        assertTrue(componentSource.contains("WheelPickerMotionPolicy.scaleForProximity"))
        assertTrue(componentSource.contains("AppMotion.PickerItemAlphaMillis"))
        assertTrue(componentSource.contains("AppMotion.PickerItemScaleMillis"))
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

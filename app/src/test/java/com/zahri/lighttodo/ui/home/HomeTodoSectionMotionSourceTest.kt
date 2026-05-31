package com.zahri.lighttodo.ui.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTodoSectionMotionSourceTest {
    @Test
    fun todoSectionRowsAnimateEnterExitAndPlacement() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/HomeTodoPage.kt").readText()
        val componentSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionSectionVisibility.kt").readText()

        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.components.MotionSectionVisibility"))
        assertTrue(source.contains("MotionSectionVisibility("))
        assertTrue(source.contains("visible = item.visible"))
        assertFalse(source.contains("import androidx.compose.animation.AnimatedVisibility"))
        assertFalse(source.contains("AnimatedVisibility("))
        assertTrue(source.contains("Modifier.animateItem("))
        assertTrue(source.contains("fadeInSpec = tween(AppMotion.SectionItemFadeInMillis"))
        assertTrue(source.contains("fadeOutSpec = tween(AppMotion.SectionItemFadeOutMillis"))
        assertTrue(source.contains("placementSpec = AppMotion.listPlacementSpring()"))
        assertFalse(source.contains("Modifier.animateItemPlacement("))
        assertTrue(componentSource.contains("AnimatedVisibility("))
        assertTrue(componentSource.contains("enter = AppMotion.sectionItemEnter()"))
        assertTrue(componentSource.contains("exit = AppMotion.sectionItemExit()"))
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

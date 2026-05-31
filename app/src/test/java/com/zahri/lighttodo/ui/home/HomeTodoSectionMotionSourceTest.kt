package com.zahri.lighttodo.ui.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTodoSectionMotionSourceTest {
    @Test
    fun todoSectionRowsAnimateEnterExitAndPlacement() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/HomeTodoPage.kt").readText()

        assertTrue(source.contains("Modifier.animateItem("))
        assertTrue(source.contains("fadeInSpec = tween(AppMotion.SectionItemFadeInMillis"))
        assertTrue(source.contains("fadeOutSpec = tween(AppMotion.SectionItemFadeOutMillis"))
        assertTrue(source.contains("placementSpec = AppMotion.listPlacementSpring()"))
        assertFalse(source.contains("Modifier.animateItemPlacement("))
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

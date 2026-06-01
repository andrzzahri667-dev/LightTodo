package com.zahri.lighttodo.ui.home

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTodoSectionMotionSourceTest {
    @Test
    fun todoSectionRowsAnimateEnterExitAndPlacement() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/home/HomeTodoPage.kt").readText()
        val visibilitySource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionSectionVisibility.kt").readText()
        val listItemSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionListItemPlacement.kt").readText()

        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.components.MotionSectionVisibility"))
        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.components.motionSectionListItem"))
        assertTrue(source.contains("MotionSectionVisibility("))
        assertTrue(source.contains("visible = item.visible"))
        assertFalse(source.contains("import androidx.compose.animation.AnimatedVisibility"))
        assertFalse(source.contains("AnimatedVisibility("))
        assertTrue(source.contains("modifier = motionSectionListItem().padding(bottom = 8.dp)"))
        assertTrue(source.contains("modifier = motionSectionListItem()"))
        assertFalse(source.contains("Modifier.animateItem("))
        assertFalse(source.contains("fadeInSpec = tween(AppMotion.SectionItemFadeInMillis"))
        assertFalse(source.contains("fadeOutSpec = tween(AppMotion.SectionItemFadeOutMillis"))
        assertFalse(source.contains("placementSpec = AppMotion.listPlacementSpring()"))
        assertFalse(source.contains("Modifier.animateItemPlacement("))
        assertTrue(visibilitySource.contains("AnimatedVisibility("))
        assertTrue(visibilitySource.contains("enter = AppMotion.sectionItemEnter()"))
        assertTrue(visibilitySource.contains("exit = AppMotion.sectionItemExit()"))
        assertTrue(listItemSource.contains("fun LazyItemScope.motionSectionListItem("))
        assertTrue(listItemSource.contains("animateItem("))
        assertTrue(listItemSource.contains("fadeInSpec = tween(AppMotion.SectionItemFadeInMillis"))
        assertTrue(listItemSource.contains("fadeOutSpec = tween(AppMotion.SectionItemFadeOutMillis"))
        assertTrue(listItemSource.contains("placementSpec = AppMotion.listPlacementSpring()"))
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

package com.zahri.lighttodo.feature.home

import com.zahri.lighttodo.test.sourceFile
import com.zahri.lighttodo.test.sourcePath

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTodoSectionMotionSourceTest {
    @Test
    fun todoSectionRowsUseLazyItemEnterExitAndPlacement() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/home/HomeTodoPage.kt").readText()
        val visibilityFile = sourcePath("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionSectionVisibility.kt")
        val listItemSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionListItemPlacement.kt").readText()

        assertFalse(visibilityFile.exists())
        assertFalse(source.contains("import com.zahri.lighttodo.ui.motion.components.MotionSectionVisibility"))
        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.components.motionSectionItemPlacement"))
        assertFalse(source.contains("MotionSectionVisibility("))
        assertFalse(source.contains("visible = item.visible"))
        assertFalse(source.contains("import androidx.compose.animation.AnimatedVisibility"))
        assertFalse(source.contains("AnimatedVisibility("))
        assertTrue(source.contains("modifier = motionSectionItemPlacement().padding(bottom = 8.dp)"))
        assertFalse(source.contains("motionSectionListItem("))
        assertFalse(source.contains("Modifier.animateItem("))
        assertFalse(source.contains("Modifier.animateItemPlacement("))
        assertTrue(listItemSource.contains("fun LazyItemScope.motionSectionItemPlacement("))
        assertFalse(listItemSource.contains("motionSectionListItem("))
        assertTrue(listItemSource.contains("animateItem("))
        assertTrue(listItemSource.contains("fadeInSpec = AppMotion.sectionItemFadeIn()"))
        assertTrue(listItemSource.contains("fadeOutSpec = AppMotion.sectionItemFadeOut()"))
        assertTrue(listItemSource.contains("placementSpec = AppMotion.listPlacementSpring()"))
    }
}

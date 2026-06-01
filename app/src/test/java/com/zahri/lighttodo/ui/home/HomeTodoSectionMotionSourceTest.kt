package com.zahri.lighttodo.ui.home

import com.zahri.lighttodo.test.sourceFile

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
        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.components.motionSectionItemPlacement"))
        assertTrue(source.contains("MotionSectionVisibility("))
        assertTrue(source.contains("visible = item.visible"))
        assertFalse(source.contains("import androidx.compose.animation.AnimatedVisibility"))
        assertFalse(source.contains("AnimatedVisibility("))
        assertTrue(source.contains("modifier = motionSectionItemPlacement().padding(bottom = 8.dp)"))
        assertTrue(source.contains("modifier = motionSectionItemPlacement()"))
        assertFalse(source.contains("motionSectionListItem("))
        assertFalse(source.contains("Modifier.animateItem("))
        assertFalse(source.contains("fadeInSpec = tween(AppMotion.SectionItemFadeInMillis"))
        assertFalse(source.contains("fadeOutSpec = tween(AppMotion.SectionItemFadeOutMillis"))
        assertFalse(source.contains("placementSpec = AppMotion.listPlacementSpring()"))
        assertFalse(source.contains("Modifier.animateItemPlacement("))
        assertTrue(visibilitySource.contains("AnimatedVisibility("))
        assertTrue(visibilitySource.contains("enter = AppMotion.sectionItemEnter()"))
        assertTrue(visibilitySource.contains("exit = AppMotion.sectionItemExit()"))
        assertTrue(listItemSource.contains("fun LazyItemScope.motionSectionItemPlacement("))
        assertFalse(listItemSource.contains("motionSectionListItem("))
        assertTrue(listItemSource.contains("animateItem("))
        assertTrue(listItemSource.contains("fadeInSpec = null"))
        assertTrue(listItemSource.contains("fadeOutSpec = null"))
        assertTrue(listItemSource.contains("placementSpec = AppMotion.listPlacementSpring()"))
        assertFalse(listItemSource.contains("tween(AppMotion.SectionItemFadeInMillis"))
        assertFalse(listItemSource.contains("tween(AppMotion.SectionItemFadeOutMillis"))
    }
}

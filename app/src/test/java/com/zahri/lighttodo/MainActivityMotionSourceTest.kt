package com.zahri.lighttodo

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityMotionSourceTest {

    @Test
    fun noteSourceResetDelayComesFromAppMotion() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/MainActivity.kt").readText()
        val visibilitySource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/NoteSourceVisibilityMotion.kt").readText()

        assertTrue(source.contains("NoteSourceVisibilityMotion.ResetDelayMillis"))
        assertFalse(source.contains("import com.zahri.lighttodo.ui.motion.AppMotion"))
        assertFalse(source.contains("AppMotion.NoteSourceAnimationResetDelayMillis"))
        assertFalse(source.contains("const val SourceAnimationResetDelayMillis"))
        assertFalse(source.contains("1_200L"))
        assertTrue(visibilitySource.contains("AppMotion.NoteSourceAnimationResetDelayMillis"))
        assertTrue(visibilitySource.contains("fun Modifier.motionNoteSourceVisibilityLayer("))
        assertTrue(visibilitySource.contains("graphicsLayer { alpha = if (hidden) 0f else 1f }"))
    }

    @Test
    fun routeTransitionsAreCentralizedOutsideMainActivity() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/MainActivity.kt").readText()
        val routeSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionRouteTransitions.kt").readText()

        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.components.motionRouteEnterTransition"))
        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.components.motionRouteExitTransition"))
        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.components.motionRoutePopEnterTransition"))
        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.components.motionRoutePopExitTransition"))
        assertTrue(source.contains("enterTransition = { motionRouteEnterTransition() }"))
        assertTrue(source.contains("exitTransition = { motionRouteExitTransition() }"))
        assertTrue(source.contains("popEnterTransition = { motionRoutePopEnterTransition() }"))
        assertTrue(source.contains("popExitTransition = { motionRoutePopExitTransition() }"))
        assertFalse(source.contains("slideIntoContainer("))
        assertFalse(source.contains("slideOutOfContainer("))
        assertFalse(source.contains("AppMotion.routeTween()"))
        assertTrue(routeSource.contains("slideIntoContainer("))
        assertTrue(routeSource.contains("slideOutOfContainer("))
        assertTrue(routeSource.contains("AppMotion.routeTween()"))
    }
}

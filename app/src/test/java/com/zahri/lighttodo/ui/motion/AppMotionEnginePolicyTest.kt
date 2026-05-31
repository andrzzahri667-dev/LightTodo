package com.zahri.lighttodo.ui.motion

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMotionEnginePolicyTest {
    @Test
    fun noteEditorContainerTransformPrefersPlatformThenMotionLayoutBeforePrimitiveCompose() {
        assertEquals(
            listOf(
                AppMotionEngine.PlatformActivityOptions,
                AppMotionEngine.MotionLayout,
                AppMotionEngine.ComposePrimitive
            ),
            AppMotionEnginePolicy.preferredEngines(AppMotionUseCase.NoteEditorContainerTransform)
        )
    }

    @Test
    fun assetTimelineAnimationPrefersLottieBeforePrimitiveCompose() {
        assertEquals(
            listOf(
                AppMotionEngine.Lottie,
                AppMotionEngine.ComposePrimitive
            ),
            AppMotionEnginePolicy.preferredEngines(AppMotionUseCase.AssetTimelineAnimation)
        )
    }

    @Test
    fun projectIncludesComposeMotionLayoutDependency() {
        val buildFile = sourceFile("app/build.gradle.kts").readText()

        assertTrue(buildFile.contains("androidx.constraintlayout:constraintlayout-compose"))
    }

    @Test
    fun projectIncludesLottieComposeDependency() {
        val buildFile = sourceFile("app/build.gradle.kts").readText()

        assertTrue(buildFile.contains("com.airbnb.android:lottie-compose"))
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

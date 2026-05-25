package com.zahri.lighttodo

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityMotionSourceTest {

    @Test
    fun noteSourceResetDelayComesFromAppMotion() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/MainActivity.kt").readText()

        assertTrue(source.contains("AppMotion.NoteSourceAnimationResetDelayMillis"))
        assertFalse(source.contains("const val SourceAnimationResetDelayMillis"))
        assertFalse(source.contains("1_200L"))
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

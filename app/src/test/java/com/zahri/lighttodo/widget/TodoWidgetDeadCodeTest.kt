package com.zahri.lighttodo.widget

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class TodoWidgetDeadCodeTest {
    @Test
    fun obsoleteWidgetServicePlaceholderIsRemoved() {
        val repo = repoRoot()

        assertFalse(File(repo, "app/src/main/java/com/zahri/lighttodo/widget/TodoWidgetService.kt").exists())
    }

    private fun repoRoot(): File {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        var dir = File(userDir).absoluteFile
        while (true) {
            if (File(dir, "settings.gradle.kts").exists()) return dir
            dir = dir.parentFile ?: break
        }
        error("Could not find repo root from $userDir")
    }
}

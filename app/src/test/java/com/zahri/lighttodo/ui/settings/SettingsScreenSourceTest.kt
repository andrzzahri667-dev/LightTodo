package com.zahri.lighttodo.ui.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsScreenSourceTest {

    @Test
    fun feedbackUsesSnackbarOverlayInsteadOfInlineLayoutText() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/settings/SettingsScreen.kt")
            .readText()

        assertTrue(source.contains("SnackbarHostState"))
        assertTrue(source.contains("SnackbarHost("))
        assertTrue(source.contains("LaunchedEffect(feedbackMessage)"))
        assertFalse(source.contains("toast.value?.let"))
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

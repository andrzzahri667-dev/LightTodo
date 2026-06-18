package com.zahri.lighttodo.feature.settings

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsScreenSourceTest {

    @Test
    fun feedbackUsesSnackbarOverlayInsteadOfInlineLayoutText() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/settings/SettingsScreen.kt")
            .readText()

        assertTrue(source.contains("SnackbarHostState"))
        assertTrue(source.contains("SnackbarHost("))
        assertTrue(source.contains("LaunchedEffect(feedbackMessage)"))
        assertFalse(source.contains("toast.value?.let"))
    }
}

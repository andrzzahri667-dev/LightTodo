package com.zahri.lighttodo.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TodoWidgetProviderSourceTest {
    @Test
    fun providerDoesNotBlockBroadcastThreadForDatabaseWork() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/widget/TodoWidgetProvider.kt")
            .readText()

        assertFalse(source.contains("runBlocking"))
        assertTrue(source.contains("goAsync()"))
        assertTrue(source.contains("appScope.launch(Dispatchers.IO)"))
    }

    @Test
    fun partialRowAnimationTargetsOnlyClickedWidgetInstance() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/widget/TodoWidgetProvider.kt")
            .readText()

        assertTrue(source.contains("EXTRA_WIDGET_ID"))
        assertTrue(source.contains("partiallyUpdateAppWidget(widgetId, rv)"))
        assertFalse(source.contains("for (id in ids) {\n                mgr.partiallyUpdateAppWidget(id, rv)"))
    }

    @Test
    fun providerDoesNotKeepDebugLogsInReleasePath() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/widget/TodoWidgetProvider.kt")
            .readText()

        assertFalse(source.contains("Log.d("))
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

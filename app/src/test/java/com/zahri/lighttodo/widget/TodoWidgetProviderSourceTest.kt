package com.zahri.lighttodo.integration.widget

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoWidgetProviderSourceTest {
    @Test
    fun providerDoesNotBlockBroadcastThreadForDatabaseWork() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/TodoWidgetProvider.kt")
            .readText()

        assertFalse(source.contains("runBlocking"))
        assertTrue(source.contains("goAsync()"))
        assertTrue(source.contains("appScope.launch(Dispatchers.IO)"))
    }

    @Test
    fun partialRowAnimationTargetsOnlyClickedWidgetInstance() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/TodoWidgetProvider.kt")
            .readText()

        assertTrue(source.contains("EXTRA_WIDGET_ID"))
        assertTrue(source.contains("partiallyUpdateAppWidget(widgetId, rv)"))
        assertFalse(source.contains("for (id in ids) {\n                mgr.partiallyUpdateAppWidget(id, rv)"))
    }

    @Test
    fun providerDoesNotKeepDebugLogsInReleasePath() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/TodoWidgetProvider.kt")
            .readText()

        assertFalse(source.contains("Log.d("))
    }

    @Test
    fun dataChangeWidgetUpdatesAreDebounced() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/TodoWidgetProvider.kt")
            .readText()

        assertTrue(source.contains("WidgetUpdateDebouncer"))
        assertTrue(source.contains("WidgetUpdateDebouncer("))
        assertFalse(source.contains("fun notifyAllWidgetsDataChanged(context: Context) {\n            val app = context.applicationContext as App\n            app.appScope.launch(Dispatchers.IO)"))
    }

    @Test
    fun dataChangeDebouncerDoesNotRetainOldAppScopeAcrossProcessState() {
        val providerSource = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/TodoWidgetProvider.kt")
            .readText()
        val debouncerSource = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/WidgetUpdateDebouncer.kt")
            .readText()

        assertTrue(providerSource.contains("widgetUpdateDebouncer.submit(app.appScope)"))
        assertFalse(debouncerSource.contains("private val scope: CoroutineScope"))
    }
}

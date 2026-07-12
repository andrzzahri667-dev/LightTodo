package com.zahri.lighttodo.widget

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
        val actionSource = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/WidgetActionReceiver.kt")
            .readText()

        assertTrue(actionSource.contains("EXTRA_WIDGET_ID"))
        assertTrue(actionSource.contains("partiallyUpdateAppWidget(widgetId, rv)"))
        assertFalse(actionSource.contains("for (id in ids) {\n                mgr.partiallyUpdateAppWidget(id, rv)"))
    }

    @Test
    fun widgetCompletionCommitsBeforeAnimationDelay() {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/integration/widget/WidgetActionReceiver.kt"
        ).readText()
        val completion = source
            .substringAfter("private fun startCompleteAnimation(")
            .substringBefore("private fun launchAsync(")
        val commit = completion.indexOf("completeTodo(todoId, true)")
        val delay = completion.indexOf("delay(STRIKE_DISPLAY_MS)")

        assertTrue(commit >= 0 && commit < delay)
    }

    @Test
    fun providerDoesNotKeepDebugLogsInReleasePath() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/TodoWidgetProvider.kt")
            .readText()

        assertFalse(source.contains("Log.d("))
    }

    @Test
    fun dayModeWidgetTextUsesDarkColorAndAllStaticLabelsAreReset() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/TodoWidgetProvider.kt")
            .readText()

        assertTrue(source.contains("private val LIGHT_TEXT_PRIMARY = 0xFF000000.toInt()"))
        assertTrue(source.contains("private val DARK_TEXT_PRIMARY = 0xFFFFFFFF.toInt()"))
        assertTrue(source.contains("R.id.widget_title,\n                lightColor = LIGHT_TEXT_PRIMARY"))
        assertTrue(source.contains("R.id.widget_empty,\n                lightColor = TEXT_SECONDARY"))
        assertTrue(source.contains("TITLE_IDS[i],\n                        lightColor = LIGHT_TEXT_PRIMARY"))
        assertTrue(source.contains("val subtitleColor = if (TodoWidgetDisplayPolicy.isSubtitleOverdue(item, clock))"))
        assertTrue(source.contains("SUBTITLE_IDS[i],\n                        lightColor = subtitleColor"))
        assertTrue(source.contains("setColorInt(viewId, \"setTextColor\", lightColor, darkColor)"))
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

    @Test
    fun widgetInternalActionsUseNonExportedReceiver() {
        val providerSource = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/widget/TodoWidgetProvider.kt")
            .readText()
        val manifestSource = sourceFile("app/src/main/AndroidManifest.xml").readText()

        assertTrue(providerSource.contains("Intent(context, WidgetActionReceiver::class.java)"))
        assertFalse(providerSource.contains("ACTION_ITEM_CLICK ->"))
        assertFalse(providerSource.contains("ACTION_REFRESH ->"))
        assertTrue(manifestSource.contains("android:name=\".integration.widget.WidgetActionReceiver\""))
        assertTrue(manifestSource.contains("android:exported=\"false\""))
        assertFalse(
            manifestSource
                .substringAfter("android:name=\".integration.widget.TodoWidgetProvider\"")
                .substringBefore("</receiver>")
                .contains("com.zahri.lighttodo.WIDGET_ITEM_CLICK")
        )
        assertFalse(
            manifestSource
                .substringAfter("android:name=\".integration.widget.TodoWidgetProvider\"")
                .substringBefore("</receiver>")
                .contains("com.zahri.lighttodo.WIDGET_REFRESH")
        )
    }
}

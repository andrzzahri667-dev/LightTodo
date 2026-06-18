package com.zahri.lighttodo.integration.widget

import android.content.Context
import com.zahri.lighttodo.usecase.todo.WidgetUpdater

class AndroidWidgetUpdater(
    private val context: Context
) : WidgetUpdater {
    override fun notifyTodosChanged() {
        TodoWidgetProvider.notifyAllWidgetsDataChanged(context)
    }
}

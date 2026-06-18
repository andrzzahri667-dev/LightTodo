package com.zahri.lighttodo.integration.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.zahri.lighttodo.App
import com.zahri.lighttodo.AppLaunchIntents
import com.zahri.lighttodo.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TodoWidgetProvider.ACTION_ITEM_CLICK -> {
                val todoId = intent.getLongExtra(TodoWidgetProvider.EXTRA_TODO_ID, -1L)
                val isCheck = intent.getBooleanExtra(TodoWidgetProvider.EXTRA_IS_CHECK, false)
                val rowIndex = intent.getIntExtra(TodoWidgetProvider.EXTRA_ROW_INDEX, -1)
                val widgetId = intent.getIntExtra(
                    TodoWidgetProvider.EXTRA_WIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (
                    todoId > 0 &&
                    isCheck &&
                    rowIndex >= 0 &&
                    widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                ) {
                    val pending = goAsync()
                    startCompleteAnimation(context, todoId, rowIndex, widgetId, pending)
                } else {
                    openApp(context)
                }
            }
            TodoWidgetProvider.ACTION_REFRESH -> {
                val pending = goAsync()
                launchAsync(context, pending) { appContext ->
                    TodoWidgetProvider.updateAllWidgetsNow(appContext)
                }
            }
        }
    }

    private fun openApp(context: Context) {
        context.startActivity(AppLaunchIntents.main(context))
    }

    private fun startCompleteAnimation(
        context: Context,
        todoId: Long,
        rowIndex: Int,
        widgetId: Int,
        pendingResult: PendingResult
    ) {
        if (WidgetAnimation.isAnimating(todoId)) {
            pendingResult.finish()
            return
        }

        val appCtx = context.applicationContext as App
        WidgetAnimation.start(todoId, rowIndex)
        appCtx.appScope.launch(Dispatchers.IO) {
            var savedDone = false
            try {
                val rv = RemoteViews(appCtx.packageName, R.layout.widget_2x2)
                val item = appCtx.container.todoUseCases.loadWidgetCompletionAnimation(
                    todoId = todoId,
                    fallbackTitle = appCtx.getString(R.string.home_no_title)
                )
                if (item != null) {
                    val titleText = item.title
                    val titleSpanned = SpannableString(titleText)
                    titleSpanned.setSpan(
                        StrikethroughSpan(), 0, titleSpanned.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    rv.setTextViewText(TITLE_IDS[rowIndex], titleSpanned)

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_SP,
                            14f,
                            appCtx.resources.displayMetrics
                        )
                    }
                    val textWidthDp = paint.measureText(titleText) / appCtx.resources.displayMetrics.density
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        rv.setViewLayoutWidth(STRIKE_IDS[rowIndex], textWidthDp, TypedValue.COMPLEX_UNIT_DIP)
                    }
                    rv.setViewVisibility(STRIKE_IDS[rowIndex], View.VISIBLE)
                }
                rv.setTextColor(TITLE_IDS[rowIndex], 0xFFE8C96A.toInt())
                rv.setImageViewResource(CHECK_IDS[rowIndex], R.drawable.widget_checkbox_checked)
                applyPartialUpdate(appCtx, widgetId, rv)

                delay(STRIKE_DISPLAY_MS)
                appCtx.container.todoUseCases.completeTodo(todoId, true)
                savedDone = true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                WidgetAnimation.clear(todoId)
                if (!savedDone) {
                    TodoWidgetProvider.notifyAllWidgetsDataChanged(appCtx)
                }
                pendingResult.finish()
            }
        }
    }

    private fun launchAsync(
        context: Context,
        pendingResult: PendingResult,
        block: suspend (Context) -> Unit
    ) {
        val app = context.applicationContext as App
        app.appScope.launch(Dispatchers.IO) {
            try {
                block(app)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun applyPartialUpdate(context: Context, widgetId: Int, rv: RemoteViews) {
        val mgr = AppWidgetManager.getInstance(context)
        mgr.partiallyUpdateAppWidget(widgetId, rv)
    }

    private companion object {
        private const val STRIKE_DISPLAY_MS = 350L
        private val TITLE_IDS = intArrayOf(R.id.title_0, R.id.title_1, R.id.title_2)
        private val CHECK_IDS = intArrayOf(R.id.check_0, R.id.check_1, R.id.check_2)
        private val STRIKE_IDS = intArrayOf(R.id.strike_0, R.id.strike_1, R.id.strike_2)
    }
}

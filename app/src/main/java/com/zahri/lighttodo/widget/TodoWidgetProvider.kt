package com.zahri.lighttodo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver.PendingResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.zahri.lighttodo.App
import com.zahri.lighttodo.MainActivity
import com.zahri.lighttodo.R
import com.zahri.lighttodo.ui.home.dateLabel
import com.zahri.lighttodo.ui.home.displayTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Objects

/**
 * 2x2 暗色圆角小组件。
 *  - 顶部："今日安排" + 右上 > 圆形按钮（点击进 App）
 *  - 中间：未完成、且当天或已逾期的任务列表（按时间升序）。最多显示 3 条，固定行布局。
 *  - 每行：橙色圆点 + 标题（一行省略） + 日期（红色=逾期）+ 右侧 [ ] 勾选框
 *  - 点击 [ ] -> 帧动画：勾选 → 文字渐隐 → 整行淡出 → 持久化 done 并移除
 *  - 点击文字行 -> 打开主页
 *
 * 使用固定行布局（3 个 row）替代 ListView，动画帧通过
 * [AppWidgetManager.partiallyUpdateAppWidget] 只更新被点击的那一行，
 * 不影响其他行，消除闪烁。
 */
class TodoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        launchAsync(context, pending) { appContext ->
            for (id in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(id)
                updateWidget(appContext, appWidgetManager, id, options)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val pending = goAsync()
        launchAsync(context, pending) { appContext ->
            updateWidget(appContext, appWidgetManager, appWidgetId, newOptions)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "miui.appwidget.action.APPWIDGET_UPDATE" -> {
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (ids != null) {
                    onUpdate(context, AppWidgetManager.getInstance(context), ids)
                }
            }
            ACTION_ITEM_CLICK -> {
                val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
                val isCheck = intent.getBooleanExtra(EXTRA_IS_CHECK, false)
                val rowIndex = intent.getIntExtra(EXTRA_ROW_INDEX, -1)
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (
                    todoId > 0 &&
                    isCheck &&
                    rowIndex >= 0 &&
                    widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                ) {
                    val pending = goAsync()
                    startCompleteAnimation(context, todoId, rowIndex, widgetId, pending)
                } else {
                    val open = Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(open)
                }
            }
            ACTION_REFRESH -> {
                val pending = goAsync()
                launchAsync(context, pending) { appContext ->
                    updateAllWidgets(appContext)
                }
            }
            else -> super.onReceive(context, intent)
        }
    }

    // ---- Animation ----

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
                // Step 1: checkbox yellow + gray strikethrough + gray text.
                val rv = RemoteViews(appCtx.packageName, R.layout.widget_2x2)
                val item = appCtx.db.todoDao().findByIdSync(todoId)
                if (item != null) {
                    val titleText = item.displayTitle(appCtx)
                    val titleSpanned = SpannableString(titleText)
                    titleSpanned.setSpan(
                        StrikethroughSpan(), 0, titleSpanned.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    rv.setTextViewText(TITLE_IDS[rowIndex], titleSpanned)

                    // Size strike line to match measured text width.
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
                rv.setTextColor(TITLE_IDS[rowIndex], 0xFFE8C96A.toInt()) // pale yellow
                rv.setImageViewResource(CHECK_IDS[rowIndex], R.drawable.widget_checkbox_checked)
                applyPartialUpdate(appCtx, widgetId, rv)

                // Step 2: persist done and remove the row.
                delay(STRIKE_DISPLAY_MS)
                appCtx.repository.setDone(todoId, true)
                savedDone = true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                WidgetAnimation.clear(todoId)
                if (!savedDone) {
                    notifyAllWidgetsDataChanged(appCtx)
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

    companion object {
        const val ACTION_ITEM_CLICK = "com.zahri.lighttodo.WIDGET_ITEM_CLICK"
        const val ACTION_TOGGLE_DONE = "com.zahri.lighttodo.WIDGET_TOGGLE_DONE"
        const val ACTION_REFRESH = "com.zahri.lighttodo.WIDGET_REFRESH"
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_IS_CHECK = "is_check"
        const val EXTRA_ROW_INDEX = "row_index"
        const val EXTRA_WIDGET_ID = "widget_id"

        private const val STRIKE_DISPLAY_MS = 350L // how long the strikethrough is visible before removal

        private val ROW_IDS = intArrayOf(R.id.row_0, R.id.row_1, R.id.row_2)
        private val DOT_IDS = intArrayOf(R.id.dot_0, R.id.dot_1, R.id.dot_2)
        private val TITLE_IDS = intArrayOf(R.id.title_0, R.id.title_1, R.id.title_2)
        private val SUBTITLE_IDS = intArrayOf(R.id.subtitle_0, R.id.subtitle_1, R.id.subtitle_2)
        private val CHECK_IDS = intArrayOf(R.id.check_0, R.id.check_1, R.id.check_2)
        private val STRIKE_IDS = intArrayOf(R.id.strike_0, R.id.strike_1, R.id.strike_2)

        private fun updateWidget(context: Context, mgr: AppWidgetManager, widgetId: Int, options: Bundle? = null) {
            val views = RemoteViews(context.packageName, R.layout.widget_2x2)

            // Force square: use the shorter dimension
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && options != null) {
                val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
                val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                val w = if (maxW > 0) maxW else minW
                val h = if (maxH > 0) maxH else minH
                val side = minOf(w, h).takeIf { it > 0 }
                if (side != null) {
                    views.setViewLayoutWidth(
                        R.id.square_container,
                        side.toFloat(),
                        TypedValue.COMPLEX_UNIT_DIP
                    )
                    views.setViewLayoutHeight(
                        R.id.square_container,
                        side.toFloat(),
                        TypedValue.COMPLEX_UNIT_DIP
                    )
                }
            }

            // Open app on background tap
            val openAppPi = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(android.R.id.background, openAppPi)

            // Query data
            val app = context.applicationContext as App
            val items = app.db.todoDao().listAllUndoneSync(nowMillis = System.currentTimeMillis(), limit = 3)
            val clock = TodoWidgetDisplayPolicy.clockAt()

            // Populate rows
            for (i in 0..2) {
                val item = items.getOrNull(i)
                if (item != null) {
                    views.setViewVisibility(ROW_IDS[i], View.VISIBLE)
                    views.setTextViewText(TITLE_IDS[i], item.displayTitle(context))
                    // Explicitly reset all properties that animation may have changed,
                    // because partiallyUpdateAppWidget diffs can survive updateAppWidget.
                    views.setTextColor(TITLE_IDS[i], context.getColor(R.color.widget_text_primary))

                    val datePart = if (com.zahri.lighttodo.util.DateUtils.isTodayOrFalse(item.date)) "" else item.dateLabel()
                    val subText = datePart + TodoWidgetDisplayPolicy.deadlineSuffix(item)
                    views.setTextViewText(SUBTITLE_IDS[i], subText)
                    views.setViewVisibility(
                        SUBTITLE_IDS[i],
                        if (subText.isEmpty()) View.GONE else View.VISIBLE
                    )
                    views.setTextColor(
                        SUBTITLE_IDS[i],
                        if (TodoWidgetDisplayPolicy.isSubtitleOverdue(item, clock)) {
                            context.getColor(R.color.widget_overdue)
                        } else {
                            context.getColor(R.color.widget_text_secondary)
                        }
                    )

                    views.setImageViewResource(CHECK_IDS[i], R.drawable.widget_checkbox)
                    views.setInt(CHECK_IDS[i], "setImageAlpha", 255)
                    views.setInt(DOT_IDS[i], "setImageAlpha", 255)
                    views.setViewVisibility(STRIKE_IDS[i], View.GONE)

                    // Click: checkbox -> complete animation
                    val checkIntent = Intent(context, TodoWidgetProvider::class.java).apply {
                        action = ACTION_ITEM_CLICK
                        putExtra(EXTRA_TODO_ID, item.id)
                        putExtra(EXTRA_IS_CHECK, true)
                        putExtra(EXTRA_ROW_INDEX, i)
                        putExtra(EXTRA_WIDGET_ID, widgetId)
                    }
                    val checkPi = PendingIntent.getBroadcast(
                        context, widgetRequestCode(widgetId, item.id, i, 0),
                        checkIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(CHECK_IDS[i], checkPi)

                    // Click: title/subtitle -> open app
                    val openIntent = Intent(context, TodoWidgetProvider::class.java).apply {
                        action = ACTION_ITEM_CLICK
                        putExtra(EXTRA_TODO_ID, item.id)
                        putExtra(EXTRA_IS_CHECK, false)
                        putExtra(EXTRA_WIDGET_ID, widgetId)
                    }
                    val openPi = PendingIntent.getBroadcast(
                        context, widgetRequestCode(widgetId, item.id, i, 100),
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(TITLE_IDS[i], openPi)
                    views.setOnClickPendingIntent(SUBTITLE_IDS[i], openPi)
                } else {
                    views.setViewVisibility(ROW_IDS[i], View.GONE)
                }
            }

            views.setViewVisibility(R.id.widget_empty, if (items.isEmpty()) View.VISIBLE else View.GONE)

            mgr.updateAppWidget(widgetId, views)
        }

        fun notifyAllWidgetsDataChanged(context: Context) {
            val app = context.applicationContext as App
            app.appScope.launch(Dispatchers.IO) {
                updateAllWidgets(app)
            }
        }

        private fun updateAllWidgets(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, TodoWidgetProvider::class.java))
            for (id in ids) updateWidget(context, mgr, id)
        }

        private fun applyPartialUpdate(context: Context, widgetId: Int, rv: RemoteViews) {
            val mgr = AppWidgetManager.getInstance(context)
            mgr.partiallyUpdateAppWidget(widgetId, rv)
        }

        private fun widgetRequestCode(widgetId: Int, itemId: Long, rowIndex: Int, offset: Int): Int =
            Objects.hash(widgetId, itemId, rowIndex, offset)
    }
}

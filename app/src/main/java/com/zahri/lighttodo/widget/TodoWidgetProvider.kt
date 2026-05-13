package com.zahri.lighttodo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.RemoteViews
import com.zahri.lighttodo.App
import com.zahri.lighttodo.MainActivity
import com.zahri.lighttodo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 2x2 暗色圆角小组件。
 *  - 顶部："今日安排" + 右上 > 圆形按钮（点击进 App）
 *  - 中间：未完成、且当天或已逾期的任务列表（按时间升序）。超出不滚（按用户要求）
 *  - 每行：橙色圆点 + 标题（一行省略） + 日期（红色=逾期）+ 右侧 [ ] 勾选框
 *  - 点击 [ ] 触发 ACTION_TOGGLE_DONE，标记完成 -> 删除线 -> 从列表移除
 *  - 点击文字行 -> 打开编辑页
 */
class TodoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(id)
            updateWidget(context, appWidgetManager, id, options)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val minW = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxW = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val minH = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val maxH = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        Log.d("Widget", "W: $minW~${maxW}dp  H: $minH~${maxH}dp")
        updateWidget(context, appWidgetManager, appWidgetId, newOptions)
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
                if (todoId > 0 && isCheck) {
                    val app = context.applicationContext as App
                    runBlocking(Dispatchers.IO) {
                        app.repository.setDone(todoId, true)
                    }
                    notifyAllWidgetsDataChanged(context)
                } else {
                    // row tap -> open app (todoId optional — could route to edit if present)
                    val open = Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(open)
                }
            }
            ACTION_REFRESH -> notifyAllWidgetsDataChanged(context)
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_ITEM_CLICK = "com.zahri.lighttodo.WIDGET_ITEM_CLICK"
        const val ACTION_TOGGLE_DONE = "com.zahri.lighttodo.WIDGET_TOGGLE_DONE"
        const val ACTION_REFRESH = "com.zahri.lighttodo.WIDGET_REFRESH"
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_IS_CHECK = "is_check"

        fun updateWidget(context: Context, mgr: AppWidgetManager, widgetId: Int, options: Bundle? = null) {
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
                    Log.d("Widget", "forcing square: ${side}dp (original W=$w H=$h)")
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

            // open app on background tap (any area not covered by list items)
            val openAppPi = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(android.R.id.background, openAppPi)

            // RemoteViewsService for the list
            val serviceIntent = Intent(context, TodoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // template for click on items
            val templateIntent = Intent(context, TodoWidgetProvider::class.java).apply {
                action = ACTION_ITEM_CLICK
            }
            val templatePi = PendingIntent.getBroadcast(
                context, 0, templateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, templatePi)

            mgr.updateAppWidget(widgetId, views)
            mgr.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list)
        }

        fun notifyAllWidgetsDataChanged(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, TodoWidgetProvider::class.java))
            for (id in ids) updateWidget(context, mgr, id)
        }
    }
}

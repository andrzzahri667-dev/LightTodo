package com.zahri.lighttodo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.zahri.lighttodo.MainActivity
import com.zahri.lighttodo.R

/**
 * 2x2 暗色圆角小部件。
 *
 * MIUI 适配要点：
 *  - 运行在 :widgetProvider 独立进程
 *  - 根布局 id = @android:id/background，背景非透明
 *  - meta-data: miuiWidget=true, miuiWidgetRefresh=exposure
 *  - 处理 miui.appwidget.action.APPWIDGET_UPDATE（曝光刷新）
 *  - 不在 widget 进程里写数据库（避免多进程并发），勾选完成的动作交给主进程的 receiver
 *
 * 行为：
 *  - 整块（除 [ ] 勾选框外）点击 → 打开 App
 *  - 每行：橙色圆点 + 标题 + 日期/时间 + [ ]
 *  - [ ] 点击 → 触发 com.zahri.lighttodo.WIDGET_TOGGLE_DONE 广播（由主进程接收）
 */
class TodoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        notifyAllWidgetsDataChanged(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        // MIUI 曝光刷新：用户滑到 widget 时系统会发一个 miui.appwidget.action.APPWIDGET_UPDATE
        if (intent.action == ACTION_MIUI_APPWIDGET_UPDATE) {
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, TodoWidgetProvider::class.java))
            onUpdate(context, AppWidgetManager.getInstance(context), ids)
            return
        }
        if (intent.action == ACTION_REFRESH) {
            notifyAllWidgetsDataChanged(context)
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_TOGGLE_DONE = "com.zahri.lighttodo.WIDGET_TOGGLE_DONE"
        const val ACTION_REFRESH = "com.zahri.lighttodo.WIDGET_REFRESH"
        const val ACTION_MIUI_APPWIDGET_UPDATE = "miui.appwidget.action.APPWIDGET_UPDATE"
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_OPEN_APP = "open_app"

        fun updateWidget(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_2x2)

            // 整块点击 → 进 App（除了 [ ]，因为 fillInIntent 优先级高于 background click）
            val openAppPi = PendingIntent.getActivity(
                context, widgetId,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(android.R.id.background, openAppPi)
            views.setOnClickPendingIntent(R.id.widget_clickable, openAppPi)
            views.setOnClickPendingIntent(R.id.widget_title, openAppPi)
            views.setOnClickPendingIntent(R.id.widget_empty, openAppPi)

            // RemoteViewsService 提供 list items
            val serviceIntent = Intent(context, TodoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // 行内点击模板：[ ] 触发主进程的 ToggleDoneReceiver；行其他位置打开 App
            val templateIntent = Intent(ACTION_TOGGLE_DONE).apply {
                // 显式发到主进程的 receiver（避免命中本 provider 在 :widgetProvider 进程）
                setClassName(context, "com.zahri.lighttodo.widget.WidgetActionReceiver")
                data = Uri.parse("widget://action/$widgetId")
            }
            val templatePi = PendingIntent.getBroadcast(
                context, widgetId, templateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, templatePi)

            mgr.updateAppWidget(widgetId, views)
            mgr.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list)
        }

        fun notifyAllWidgetsDataChanged(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, TodoWidgetProvider::class.java))
            if (ids.isEmpty()) return
            for (id in ids) updateWidget(context, mgr, id)
        }
    }
}

package com.zahri.lighttodo.integration.calendar

import android.content.Context
import com.zahri.lighttodo.App
import com.zahri.lighttodo.R
import com.zahri.lighttodo.domain.calendar.CalendarSyncPolicy
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 系统日历双向同步。
 *
 * 设计：
 *  - 默认扫描所有非节假日日历；设置里可手动输入账户名缩小范围
 *  - 排除节日和假期：用 displayName 黑名单过滤常见节假日日历
 *  - 时间窗口：今天起的近 60 天（避免历史事件刷屏，逾期靠 App 内任务而不是日历）
 *  - 外部导入的任务带 calendarEventId 且 calendarCreatedByApp=false，编辑页禁用字段
 *  - 本 App 创建的任务可镜像到系统日历，calendarCreatedByApp=true，仍允许编辑
 *  - 去重：calendarEventId UNIQUE
 *  - 移除已不存在的未完成事件（避免日历删了，待办还留着；完成历史保留）
 */
object CalendarSync {

    /**
     * @return 同步导入的任务条数；-1 表示失败/没权限
     */
    suspend fun runOnce(context: Context, force: Boolean = false): Int =
        withTimeoutOrNull(CalendarSyncPolicy.SyncTimeoutMillis) {
            runOnceLocked(context, force)
        } ?: -1

    private suspend fun runOnceLocked(context: Context, force: Boolean): Int = CalendarSyncCoordinator.withLock {
        val app = context.applicationContext as App
        app.container.syncCalendar(
            force = force,
            noTitleFallback = context.getString(R.string.calendar_no_title)
        )
    }
}

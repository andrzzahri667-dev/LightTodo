package com.zahri.lighttodo.integration.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.zahri.lighttodo.App
import com.zahri.lighttodo.R
import com.zahri.lighttodo.data.TodoDateFields
import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.util.DateUtils
import com.zahri.lighttodo.integration.widget.TodoWidgetProvider
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.util.Calendar

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
        val prefs = app.prefs.snapshot()
        if (!force && !prefs.calendarSyncEnabled) return@withLock -1
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return@withLock -1
        }

        val calendarIds = pickCalendarIds(context, prefs.calendarAccountName)
        if (calendarIds.isEmpty()) return@withLock 0

        val now = LocalDate.now()
        val from = DateUtils.startOfDayMillis(now)
        val to = DateUtils.endOfDayMillis(now.plusDays(60))

        val events = queryEvents(context, calendarIds, from, to)
        val eventIds = events.map { it.id }

        val orphanEventIds = CalendarSyncPolicy.orphanEventIds(
            importedEventIds = app.db.todoDao().listUndoneCalendarEventIdsInWindow(from, to),
            providerEventIds = eventIds
        )
        if (orphanEventIds.isNotEmpty()) {
            val orphanTodos = app.db.todoDao().findByCalendarEventIds(orphanEventIds)
            orphanTodos
                .filter { it.calendarCreatedByApp }
                .forEach { app.db.todoDao().setCalendarLink(it.id, eventId = null, createdByApp = false) }
            orphanTodos
                .filterNot { it.calendarCreatedByApp }
                .map { it.id }
                .takeIf { it.isNotEmpty() }
                ?.let { app.db.todoDao().deleteByIds(it) }
        }

        // Insert new + update existing entities by calendarEventId
        val existingEntities = if (eventIds.isEmpty()) {
            emptyList()
        } else {
            app.db.todoDao().findByCalendarEventIds(eventIds)
        }
        val existingMap = existingEntities.associateBy { it.calendarEventId }
        val now2 = System.currentTimeMillis()

        val toUpsert = mutableListOf<TodoEntity>()
        for (ev in events) {
            val dateFields = TodoDateFields.fromEpochMillis(ev.startMillis)
            val (startH, startM) = if (ev.allDay) null to null else hourMinuteOf(ev.startMillis)
            val (endH, endM) = if (ev.allDay || ev.endMillis == null) null to null else hourMinuteOf(ev.endMillis)
            val existing = existingMap[ev.id]
            val doneMerge = CalendarSyncPolicy.mergeDoneState(
                providerCanceled = ev.status == CalendarContract.Events.STATUS_CANCELED,
                existingDone = existing?.done,
                existingDoneAtMillis = existing?.doneAtMillis,
                nowMillis = now2
            )
            toUpsert += TodoEntity(
                id = existing?.id ?: 0L,
                title = ev.title.ifBlank { context.getString(R.string.calendar_no_title) },
                note = ev.description?.takeIf { it.isNotBlank() },
                date = dateFields.date,
                dateMillis = dateFields.dateMillis,
                startHour = startH,
                startMinute = startM,
                deadlineHour = endH ?: startH,
                deadlineMinute = endM ?: startM,
                remindStartAtMillis = null,
                remindAtMillis = null,
                customRemindHoursBefore = null,
                tagId = existing?.tagId,
                done = doneMerge.done,
                doneAtMillis = doneMerge.doneAtMillis,
                createdAtMillis = existing?.createdAtMillis ?: now2,
                calendarEventId = ev.id,
                calendarCreatedByApp = existing?.calendarCreatedByApp ?: false
            )
        }
        if (toUpsert.isNotEmpty()) {
            app.db.todoDao().upsertAll(toUpsert)
        }

        // Refresh widget(s) when data changed
        if (toUpsert.isNotEmpty() || orphanEventIds.isNotEmpty()) {
            TodoWidgetProvider.notifyAllWidgetsDataChanged(context)
        }
        toUpsert.size
    }

    private fun pickCalendarIds(context: Context, userFilter: String): List<Long> {
        val cr = context.contentResolver
        val cursor = cr.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
            ), null, null, null
        ) ?: return emptyList()

        val ids = mutableListOf<Long>()
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val acctName = it.getString(1).orEmpty()
                val acctType = it.getString(2).orEmpty()
                val displayName = it.getString(3).orEmpty()

                // Skip Holidays / festivals
                if (CalendarSyncPolicy.shouldExcludeCalendarName(displayName)) continue

                if (CalendarSyncPolicy.matchesAccount(acctName, acctType, userFilter)) {
                    ids += id
                }
            }
        }
        return ids
    }

    private fun queryEvents(
        context: Context,
        calendarIds: List<Long>,
        from: Long,
        to: Long
    ): List<RawEvent> {
        val cr = context.contentResolver
        val placeholders = calendarIds.joinToString(",") { "?" }
        val sel = "${CalendarContract.Events.CALENDAR_ID} IN ($placeholders) AND " +
                "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val args = (calendarIds.map { it.toString() } + listOf(from.toString(), to.toString())).toTypedArray()

        val cursor = cr.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.STATUS
            ),
            sel, args, "${CalendarContract.Events.DTSTART} ASC"
        ) ?: return emptyList()

        val out = mutableListOf<RawEvent>()
        cursor.use {
            while (it.moveToNext()) {
                out += RawEvent(
                    id = it.getLong(0),
                    title = it.getString(1).orEmpty(),
                    description = it.getString(2),
                    startMillis = it.getLong(3),
                    allDay = it.getInt(4) == 1,
                    endMillis = if (it.isNull(5)) null else it.getLong(5),
                    status = if (it.isNull(6)) null else it.getInt(6)
                )
            }
        }
        return out
    }

    private fun hourMinuteOf(millis: Long): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
    }

    private data class RawEvent(
        val id: Long,
        val title: String,
        val description: String?,
        val startMillis: Long,
        val endMillis: Long?,
        val allDay: Boolean,
        val status: Int?
    )
}

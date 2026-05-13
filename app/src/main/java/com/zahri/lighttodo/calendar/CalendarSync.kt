package com.zahri.lighttodo.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.zahri.lighttodo.App
import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.util.DateUtils
import java.time.LocalDate
import java.util.Calendar

/**
 * 单向只读拉取系统日历（含小米日历）。
 *
 * 改进版：
 *  - 用 Instances.CONTENT_URI 而不是 Events，自动展开重复事件
 *  - 默认拉取所有非"节日"日历，让用户在设置里勾选要排除哪些
 *  - 仍然过滤掉 displayName 包含"节日 / 假期"等关键字的日历
 */
object CalendarSync {

    private val EXCLUDED_NAME_KEYWORDS = listOf("节日", "假期", "假日", "Holidays", "节假日", "Festival")

    data class CalendarInfo(
        val id: Long,
        val displayName: String,
        val accountName: String,
        val accountType: String
    )

    /**
     * 列出系统中所有可用的（非节日）日历，供 UI 让用户勾选。
     */
    fun listCalendars(context: Context): List<CalendarInfo> {
        if (!hasPermission(context)) return emptyList()
        val cr = context.contentResolver
        val cursor = cr.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE
            ),
            null, null, null
        ) ?: return emptyList()
        val out = mutableListOf<CalendarInfo>()
        cursor.use {
            while (it.moveToNext()) {
                val name = it.getString(1).orEmpty()
                if (EXCLUDED_NAME_KEYWORDS.any { kw -> name.contains(kw, ignoreCase = true) }) continue
                out += CalendarInfo(
                    id = it.getLong(0),
                    displayName = name,
                    accountName = it.getString(2).orEmpty(),
                    accountType = it.getString(3).orEmpty()
                )
            }
        }
        return out
    }

    /**
     * @return 同步导入的任务条数；-1 表示失败/没权限/未启用
     */
    suspend fun runOnce(context: Context): Int {
        val app = context.applicationContext as App
        val prefs = app.prefs.snapshot()
        if (!prefs.calendarSyncEnabled) return -1
        if (!hasPermission(context)) return -1

        val calendarIds = pickCalendarIds(context, prefs.calendarAccountName, prefs.excludedCalendarIds)
        if (calendarIds.isEmpty()) return 0

        val now = LocalDate.now()
        val from = DateUtils.startOfDayMillis(now)
        val to = DateUtils.endOfDayMillis(now.plusDays(60))

        val events = queryInstances(context, calendarIds, from, to)

        val existing = app.db.todoDao().listCalendarEventIds().toSet()
        val nowMs = System.currentTimeMillis()

        val toInsert = mutableListOf<TodoEntity>()
        val seenIds = mutableListOf<Long>()
        for (ev in events) {
            seenIds += ev.id
            if (ev.id in existing) continue
            val (date, dateMillis) = DateUtils.dayKeyAndStartFromMillis(ev.startMillis)
            val (h, m) = if (ev.allDay) null to null else hourMinuteOf(ev.startMillis)
            toInsert += TodoEntity(
                title = ev.title.ifBlank { "（无标题事件）" },
                note = ev.description?.takeIf { it.isNotBlank() },
                date = date,
                dateMillis = dateMillis,
                deadlineHour = h,
                deadlineMinute = m,
                remindAtMillis = null, // 不和系统日历自身的提醒抢
                customRemindHoursBefore = null,
                tagId = null,
                done = false,
                createdAtMillis = nowMs,
                calendarEventId = ev.id
            )
        }
        if (toInsert.isNotEmpty()) {
            app.db.todoDao().upsertAll(toInsert)
        }
        app.db.todoDao().deleteCalendarOrphans(seenIds)
        return toInsert.size
    }

    private fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    private fun pickCalendarIds(
        context: Context,
        userFilter: String,
        excludedIds: Set<Long>
    ): List<Long> {
        val all = listCalendars(context)
        val filtered = if (userFilter.isBlank()) {
            // 默认：所有非节日日历都拉
            all
        } else {
            all.filter { it.accountName.contains(userFilter, ignoreCase = true) }
        }
        return filtered.map { it.id }.filter { it !in excludedIds }
    }

    private fun queryInstances(
        context: Context,
        calendarIds: List<Long>,
        from: Long,
        to: Long
    ): List<RawEvent> {
        // Instances API：把范围作为 URI path 传入，会自动展开重复事件
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, from)
        ContentUris.appendId(builder, to)
        val uri = builder.build()

        val placeholders = calendarIds.joinToString(",") { "?" }
        val sel = "${CalendarContract.Instances.CALENDAR_ID} IN ($placeholders)"
        val args = calendarIds.map { it.toString() }.toTypedArray()

        val cursor = context.contentResolver.query(
            uri,
            arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY
            ),
            sel, args, "${CalendarContract.Instances.BEGIN} ASC"
        ) ?: return emptyList()

        val out = mutableListOf<RawEvent>()
        cursor.use {
            while (it.moveToNext()) {
                out += RawEvent(
                    id = it.getLong(0),
                    title = it.getString(1).orEmpty(),
                    description = it.getString(2),
                    startMillis = it.getLong(3),
                    allDay = it.getInt(4) == 1
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
        val allDay: Boolean
    )
}

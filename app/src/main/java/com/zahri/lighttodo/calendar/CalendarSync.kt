package com.zahri.lighttodo.calendar

import android.Manifest
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
 * 单向只读拉取小米日历。
 *
 * 设计：
 *  - 仅扫描"小米账户"相关日历（默认匹配 account_name 包含 "xiaomi" / "MI" / "小米"，
 *    设置里也可手动输入指定账户名精确匹配）
 *  - 排除节日和假期：小米的内置节日日历通常 account_type = "LOCAL"
 *    或日历 displayName 包含"节日/假期"。我们用一组 displayName 黑名单 + 仅取
 *    ACCOUNT_TYPE != "LOCAL" 来过滤
 *  - 时间窗口：今天起的近 60 天（避免历史事件刷屏，逾期靠 App 内任务而不是日历）
 *  - 只读：导入的任务带 calendarEventId，编辑页禁用所有字段，只能勾选完成
 *  - 去重：calendarEventId UNIQUE
 *  - 移除已不存在的事件（避免日历删了，待办还留着）
 */
object CalendarSync {

    private val EXCLUDED_NAME_KEYWORDS = listOf("节日", "假期", "假日", "Holidays", "节假日")

    /**
     * @return 同步导入的任务条数；-1 表示失败/没权限/未启用
     */
    suspend fun runOnce(context: Context): Int {
        val app = context.applicationContext as App
        val prefs = app.prefs.snapshot()
        if (!prefs.calendarSyncEnabled) return -1
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return -1
        }

        val calendarIds = pickCalendarIds(context, prefs.calendarAccountName)
        if (calendarIds.isEmpty()) return 0

        val now = LocalDate.now()
        val from = DateUtils.startOfDayMillis(now)
        val to = DateUtils.endOfDayMillis(now.plusDays(60))

        val events = queryEvents(context, calendarIds, from, to)

        // Insert as new entities, dedup by calendarEventId via REPLACE on UNIQUE index
        val existing = app.db.todoDao().listCalendarEventIds().toSet()
        val now2 = System.currentTimeMillis()

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
                remindAtMillis = null, // do not interfere with system calendar reminders
                customRemindHoursBefore = null,
                tagId = null,
                done = false,
                createdAtMillis = now2,
                calendarEventId = ev.id
            )
        }
        if (toInsert.isNotEmpty()) {
            app.db.todoDao().upsertAll(toInsert)
        }
        // Remove events that disappeared
        app.db.todoDao().deleteCalendarOrphans(seenIds)
        return toInsert.size
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
                if (EXCLUDED_NAME_KEYWORDS.any { kw -> displayName.contains(kw, ignoreCase = true) }) continue
                // Skip local-only "Holidays"-ish calendars
                if (acctType.equals("LOCAL", ignoreCase = true)) continue

                if (matchesAccount(acctName, userFilter)) {
                    ids += id
                }
            }
        }
        return ids
    }

    private fun matchesAccount(acctName: String, userFilter: String): Boolean {
        val f = userFilter.trim()
        return if (f.isNotEmpty()) {
            acctName.equals(f, ignoreCase = true) || acctName.contains(f, ignoreCase = true)
        } else {
            // Heuristic: any name containing xiaomi / mi / 小米
            val lower = acctName.lowercase()
            lower.contains("xiaomi") || lower.contains("小米") || lower.contains("mi.")
                || lower == "mi" || lower.contains("@mi.com")
        }
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
                CalendarContract.Events.ALL_DAY
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

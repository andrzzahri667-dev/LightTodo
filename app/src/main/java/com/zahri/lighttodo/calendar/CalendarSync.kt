package com.zahri.lighttodo.calendar

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
import com.zahri.lighttodo.widget.TodoWidgetProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    /** Serializes concurrent runOnce calls (e.g. App.onCreate + BootReceiver at boot). */
    private val syncMutex = Mutex()

    /**
     * @return 同步导入的任务条数；-1 表示失败/没权限
     */
    suspend fun runOnce(context: Context, force: Boolean = false): Int = syncMutex.withLock {
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

        // Insert new + update existing entities by calendarEventId
        val existingEntities = app.db.todoDao().findByCalendarEventIds(events.map { it.id })
        val existingMap = existingEntities.associateBy { it.calendarEventId }
        val now2 = System.currentTimeMillis()

        val toUpsert = mutableListOf<TodoEntity>()
        val seenIds = mutableListOf<Long>()
        for (ev in events) {
            seenIds += ev.id
            val dateFields = TodoDateFields.fromEpochMillis(ev.startMillis)
            val (startH, startM) = if (ev.allDay) null to null else hourMinuteOf(ev.startMillis)
            val (endH, endM) = if (ev.allDay || ev.endMillis == null) null to null else hourMinuteOf(ev.endMillis)
            val existing = existingMap[ev.id]
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
                done = existing?.done ?: false,
                doneAtMillis = existing?.doneAtMillis,
                createdAtMillis = existing?.createdAtMillis ?: now2,
                calendarEventId = ev.id
            )
        }
        if (toUpsert.isNotEmpty()) {
            app.db.todoDao().upsertAll(toUpsert)
        }
        // Remove events that disappeared from the system calendar
        app.db.todoDao().deleteCalendarOrphans(seenIds)

        // Refresh widget(s) when data changed
        if (toUpsert.isNotEmpty()) {
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
                val displayName = it.getString(3).orEmpty()

                // Skip Holidays / festivals
                if (EXCLUDED_NAME_KEYWORDS.any { kw -> displayName.contains(kw, ignoreCase = true) }) continue

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
            // 无过滤条件时匹配所有非节日日历
            true
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
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.DTEND
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
                    endMillis = if (it.isNull(5)) null else it.getLong(5)
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
        val allDay: Boolean
    )
}

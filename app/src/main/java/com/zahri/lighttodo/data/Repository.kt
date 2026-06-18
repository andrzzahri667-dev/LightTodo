package com.zahri.lighttodo.data

import android.content.Context
import com.zahri.lighttodo.integration.calendar.CalendarEventWriter
import com.zahri.lighttodo.integration.calendar.CalendarSyncCoordinator
import com.zahri.lighttodo.integration.reminder.ReminderScheduler
import com.zahri.lighttodo.integration.widget.TodoWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 单一数据源。
 *
 * 关键决策：保存任务时，根据 prefs 默认值 + 任务的截止时间，自动算出 remindAtMillis，
 * 让 UI 不必关心提醒计算细节。然后顺手挂上/取消 AlarmManager。
 */
class Repository(
    private val context: Context,
    private val todoDao: TodoDao,
    private val tagDao: TagDao,
    private val prefs: UserPrefs
) {

    val todosFlow: Flow<List<TodoEntity>> = todoDao.observeAll()
    val tagsFlow: Flow<List<TagEntity>> = tagDao.observeAll()

    fun homeFlow(): Flow<HomeData> =
        combine(todosFlow, tagsFlow, prefs.flow) { todos, tags, p -> HomeData(todos, tags, p) }

    suspend fun saveTodo(input: TodoInput): Long {
        val p = prefs.flow.first()
        val tagId = resolveTagId(input.tagName)

        val dateFields = TodoDateFields.fromParts(input.year, input.month, input.day)

        // 无日期任务：清空所有时间相关字段与提醒；
        // 有日期任务：保留时间，并按规则计算提醒时间戳。
        val startHour: Int?
        val startMinute: Int?
        val deadlineHour: Int?
        val deadlineMinute: Int?
        val customHoursBefore: Int?
        val remindStart: Long?
        val remindEnd: Long?
        if (dateFields is TodoDateFields.Dated) {
            startHour = input.startHour
            startMinute = input.startMinute
            deadlineHour = input.deadlineHour
            deadlineMinute = input.deadlineMinute
            customHoursBefore = input.customHoursBefore
            // 开始时间提醒：恰在开始时刻触发（不应用"提前 N 小时"）
            remindStart = computeRemindAt(
                dateMillis = dateFields.dateMillis,
                hour = input.startHour,
                minute = input.startMinute,
                hoursBefore = 0,
                defaultHour = p.defaultRemindHour,
                defaultMinute = p.defaultRemindMinute,
                allDayFallback = false
            )
            // 截止时间提醒：未覆盖时使用全局默认提前小时数；显式 0 表示到点提醒。
            val endHoursBefore = TodoReminderDefaults.effectiveHoursBefore(
                customHoursBefore = input.customHoursBefore,
                defaultHoursBefore = p.defaultHoursBefore
            )
            remindEnd = computeRemindAt(
                dateMillis = dateFields.dateMillis,
                hour = input.deadlineHour,
                minute = input.deadlineMinute,
                hoursBefore = endHoursBefore,
                defaultHour = p.defaultRemindHour,
                defaultMinute = p.defaultRemindMinute,
                allDayFallback = true
            )
        } else {
            startHour = null
            startMinute = null
            deadlineHour = null
            deadlineMinute = null
            customHoursBefore = null
            remindStart = null
            remindEnd = null
        }

        val existing = input.id?.let { todoDao.findById(it) }
        val now = System.currentTimeMillis()
        val entity = TodoEntity(
            id = input.id ?: 0L,
            title = input.title?.takeIf { it.isNotBlank() },
            note = input.note?.takeIf { it.isNotBlank() },
            date = dateFields.date,
            dateMillis = dateFields.dateMillis,
            startHour = startHour,
            startMinute = startMinute,
            deadlineHour = deadlineHour,
            deadlineMinute = deadlineMinute,
            remindStartAtMillis = remindStart,
            remindAtMillis = remindEnd,
            customRemindHoursBefore = customHoursBefore,
            tagId = tagId,
            done = existing?.done ?: false,
            doneAtMillis = existing?.doneAtMillis,
            createdAtMillis = existing?.createdAtMillis ?: now,
            calendarEventId = existing?.calendarEventId,
            calendarCreatedByApp = existing?.calendarCreatedByApp ?: false
        )
        return if (p.calendarSyncEnabled) {
            CalendarSyncCoordinator.withLock { persistTodo(entity, p, now, input.id) }
        } else {
            persistTodo(entity, p, now, input.id)
        }
    }

    suspend fun setDone(id: Long, done: Boolean) {
        val p = prefs.snapshot()
        if (p.calendarSyncEnabled) {
            CalendarSyncCoordinator.withLock { setDoneLocked(id, done, p) }
        } else {
            setDoneLocked(id, done, p)
        }
    }

    private suspend fun setDoneLocked(id: Long, done: Boolean, prefsSnapshot: UserPrefs.Snapshot) {
        val before = todoDao.findById(id) ?: return
        val doneAtMillis = if (done) System.currentTimeMillis() else null
        todoDao.setDone(id, done, doneAtMillis)
        val updated = before.copy(done = done, doneAtMillis = doneAtMillis)
        if (prefsSnapshot.calendarSyncEnabled) {
            withContext<Unit>(Dispatchers.IO) {
                if (done) {
                    updated.calendarEventId?.let { CalendarEventWriter.setCompleted(context, it, true) }
                } else if (updated.dateMillis != null) {
                    val eventId = CalendarEventWriter.upsertFromTodo(context, updated, prefsSnapshot.calendarAccountName)
                    if (eventId != null && eventId != updated.calendarEventId) {
                        todoDao.setCalendarLink(
                            id = id,
                            eventId = eventId,
                            createdByApp = updated.calendarCreatedByApp || updated.calendarEventId == null
                        )
                    }
                }
            }
        }
        if (done) ReminderScheduler.cancel(context, id)
        else {
            val t = updated
            val now = System.currentTimeMillis()
            if (t.remindStartAtMillis != null && t.remindStartAtMillis > now)
                ReminderScheduler.schedule(context, t, isStart = true)
            if (t.remindAtMillis != null && t.remindAtMillis > now)
                ReminderScheduler.schedule(context, t, isStart = false)
        }
        TodoWidgetProvider.notifyAllWidgetsDataChanged(context)
    }

    suspend fun delete(id: Long) {
        val p = prefs.snapshot()
        if (p.calendarSyncEnabled) {
            CalendarSyncCoordinator.withLock {
                todoDao.findById(id)?.calendarEventId?.let { eventId ->
                    withContext(Dispatchers.IO) { CalendarEventWriter.deleteEvent(context, eventId) }
                }
                deleteLocalTodo(id)
            }
        } else {
            deleteLocalTodo(id)
        }
    }

    suspend fun deleteMany(ids: List<Long>) {
        if (ids.isEmpty()) return
        val p = prefs.snapshot()
        if (p.calendarSyncEnabled) {
            CalendarSyncCoordinator.withLock {
                val calendarEventIds = todoDao.findByIds(ids)
                    .mapNotNull { it.calendarEventId }
                withContext(Dispatchers.IO) {
                    calendarEventIds.forEach { CalendarEventWriter.deleteEvent(context, it) }
                }
                deleteLocalTodos(ids)
            }
        } else {
            deleteLocalTodos(ids)
        }
    }

    suspend fun clearDone() {
        val p = prefs.snapshot()
        if (p.calendarSyncEnabled) {
            CalendarSyncCoordinator.withLock {
                val doneTodos = todoDao.listDone()
                val doneReminderIds = todoDao.listDoneWithReminders().map { it.id }
                withContext(Dispatchers.IO) {
                    doneTodos.mapNotNull { it.calendarEventId }
                        .forEach { CalendarEventWriter.deleteEvent(context, it) }
                }
                todoDao.deleteAllDone()
                doneReminderIds.forEach { id -> ReminderScheduler.cancel(context, id) }
                TodoWidgetProvider.notifyAllWidgetsDataChanged(context)
            }
        } else {
            val doneReminderIds = todoDao.listDoneWithReminders().map { it.id }
            todoDao.deleteAllDone()
            doneReminderIds.forEach { id -> ReminderScheduler.cancel(context, id) }
            TodoWidgetProvider.notifyAllWidgetsDataChanged(context)
        }
    }

    suspend fun rescheduleAllAlarms() {
        val allTodos = todoDao.listAll()
        allTodos.forEach { t -> ReminderScheduler.cancel(context, t.id) }
        val list = allTodos.filter { !it.done && it.hasAnyReminder() }
        val now = System.currentTimeMillis()
        list.forEach { t ->
            if (t.remindStartAtMillis != null && t.remindStartAtMillis > now)
                ReminderScheduler.schedule(context, t, isStart = true)
            if (t.remindAtMillis != null && t.remindAtMillis > now)
                ReminderScheduler.schedule(context, t, isStart = false)
        }
    }

    private suspend fun resolveTagId(name: String?): Long? {
        val n = name?.trim().orEmpty()
        if (n.isEmpty()) return null
        tagDao.findByName(n)?.let { return it.id }
        return tagDao.insert(TagEntity(name = n)).takeIf { it != -1L }
            ?: tagDao.findByName(n)?.id
    }

    private suspend fun persistTodo(
        entity: TodoEntity,
        prefsSnapshot: UserPrefs.Snapshot,
        now: Long,
        inputId: Long?
    ): Long {
        val mirrored = mirrorTodoToCalendar(entity, prefsSnapshot)
        val id = todoDao.upsert(mirrored)
        val saved = mirrored.copy(id = if (inputId != null) inputId else id)
        ReminderScheduler.cancel(context, saved.id)
        if (!saved.done) {
            val t = saved
            if (t.remindStartAtMillis != null && t.remindStartAtMillis > now)
                ReminderScheduler.schedule(context, t, isStart = true)
            if (t.remindAtMillis != null && t.remindAtMillis > now)
                ReminderScheduler.schedule(context, t, isStart = false)
        }
        TodoWidgetProvider.notifyAllWidgetsDataChanged(context)
        return saved.id
    }

    private suspend fun deleteLocalTodo(id: Long) {
        todoDao.delete(id)
        ReminderScheduler.cancel(context, id)
        TodoWidgetProvider.notifyAllWidgetsDataChanged(context)
    }

    private suspend fun deleteLocalTodos(ids: List<Long>) {
        todoDao.deleteByIds(ids)
        ids.forEach { id -> ReminderScheduler.cancel(context, id) }
        TodoWidgetProvider.notifyAllWidgetsDataChanged(context)
    }

    private suspend fun mirrorTodoToCalendar(todo: TodoEntity, prefs: UserPrefs.Snapshot): TodoEntity = withContext(Dispatchers.IO) {
        if (!prefs.calendarSyncEnabled) return@withContext todo
        if (todo.dateMillis == null) {
            todo.calendarEventId?.let { CalendarEventWriter.deleteEvent(context, it) }
            return@withContext todo.copy(calendarEventId = null, calendarCreatedByApp = false)
        }
        val eventId = CalendarEventWriter.upsertFromTodo(context, todo, prefs.calendarAccountName)
        if (eventId != null) {
            todo.copy(
                calendarEventId = eventId,
                calendarCreatedByApp = todo.calendarCreatedByApp || todo.calendarEventId == null
            )
        } else {
            todo
        }
    }

    /**
     * 计算提醒时间戳：
     *  - 有具体时刻：trigger = (date + hour:minute) - hoursBefore
     *  - 全天：仅当 allDayFallback=true 时使用当天默认时刻（如 09:00）；否则返回 null
     */
    private fun computeRemindAt(
        dateMillis: Long,
        hour: Int?,
        minute: Int?,
        hoursBefore: Int,
        defaultHour: Int,
        defaultMinute: Int,
        allDayFallback: Boolean
    ): Long? {
        if (hour != null && minute != null) {
            val target = dateMillis + hour * 3_600_000L + minute * 60_000L
            return target - hoursBefore * 3_600_000L
        }
        if (!allDayFallback) return null
        return dateMillis + defaultHour * 3_600_000L + defaultMinute * 60_000L
    }
}

data class HomeData(
    val todos: List<TodoEntity>,
    val tags: List<TagEntity>,
    val prefs: UserPrefs.Snapshot
)

data class TodoInput(
    val id: Long? = null,
    val title: String?,
    val note: String?,
    /** year/month/day 三者要么同时非空（有日期任务），要么同时为 null（无日期任务） */
    val year: Int? = null,
    val month: Int? = null, // 1-12
    val day: Int? = null,   // 1-31
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val deadlineHour: Int? = null,
    val deadlineMinute: Int? = null,
    val customHoursBefore: Int? = null,
    val tagName: String? = null
)

package com.zahri.lighttodo.data

import android.content.Context
import com.zahri.lighttodo.notify.ReminderScheduler
import com.zahri.lighttodo.util.DateUtils
import com.zahri.lighttodo.widget.TodoWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

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
        val (date, dateMillis) = DateUtils.dayKeyAndStart(input.year, input.month, input.day)
        val remindAt = computeRemindAt(
            dateMillis = dateMillis,
            deadlineHour = input.deadlineHour,
            deadlineMinute = input.deadlineMinute,
            customHoursBefore = input.customHoursBefore,
            defaultHour = p.defaultRemindHour,
            defaultMinute = p.defaultRemindMinute,
            defaultHoursBefore = p.defaultHoursBefore
        )
        val existing = input.id?.let { todoDao.findById(it) }
        val now = System.currentTimeMillis()
        val entity = TodoEntity(
            id = input.id ?: 0L,
            title = input.title?.takeIf { it.isNotBlank() },
            note = input.note?.takeIf { it.isNotBlank() },
            date = date,
            dateMillis = dateMillis,
            deadlineHour = input.deadlineHour,
            deadlineMinute = input.deadlineMinute,
            remindAtMillis = remindAt,
            customRemindHoursBefore = input.customHoursBefore,
            tagId = tagId,
            done = existing?.done ?: false,
            doneAtMillis = existing?.doneAtMillis,
            createdAtMillis = existing?.createdAtMillis ?: now,
            calendarEventId = existing?.calendarEventId
        )
        val id = todoDao.upsert(entity)
        // After insert, we need the actual id to schedule alarm
        val saved = entity.copy(id = if (input.id != null) input.id else id)
        ReminderScheduler.cancel(context, saved.id)
        if (!saved.done && saved.remindAtMillis != null && saved.remindAtMillis > now) {
            ReminderScheduler.schedule(context, saved)
        }
        TodoWidgetProvider.notifyAllWidgetsDataChanged(context)
        return saved.id
    }

    suspend fun setDone(id: Long, done: Boolean) {
        todoDao.setDone(id, done, if (done) System.currentTimeMillis() else null)
        if (done) ReminderScheduler.cancel(context, id)
        else {
            val t = todoDao.findById(id)
            if (t?.remindAtMillis != null && t.remindAtMillis > System.currentTimeMillis()) {
                ReminderScheduler.schedule(context, t)
            }
        }
        TodoWidgetProvider.notifyAllWidgetsDataChanged(context)
    }

    suspend fun delete(id: Long) {
        ReminderScheduler.cancel(context, id)
        todoDao.delete(id)
        TodoWidgetProvider.notifyAllWidgetsDataChanged(context)
    }

    suspend fun clearDone() {
        todoDao.deleteAllDone()
        TodoWidgetProvider.notifyAllWidgetsDataChanged(context)
    }

    suspend fun rescheduleAllAlarms() {
        val list = todoDao.listWithReminders()
        val now = System.currentTimeMillis()
        list.forEach { t ->
            if (t.remindAtMillis != null && t.remindAtMillis > now) {
                ReminderScheduler.schedule(context, t)
            }
        }
    }

    private suspend fun resolveTagId(name: String?): Long? {
        val n = name?.trim().orEmpty()
        if (n.isEmpty()) return null
        tagDao.findByName(n)?.let { return it.id }
        return tagDao.insert(TagEntity(name = n)).takeIf { it != -1L }
            ?: tagDao.findByName(n)?.id
    }

    /**
     * 计算提醒时间戳：
     *  - 有截止时刻：deadline - hoursBefore
     *  - 全天：当天默认时刻（如 09:00）
     *  - 用户没设自定义 hoursBefore，则使用全局默认
     */
    private fun computeRemindAt(
        dateMillis: Long,
        deadlineHour: Int?,
        deadlineMinute: Int?,
        customHoursBefore: Int?,
        defaultHour: Int,
        defaultMinute: Int,
        defaultHoursBefore: Int
    ): Long? {
        if (deadlineHour != null && deadlineMinute != null) {
            val deadline = dateMillis + deadlineHour * 3_600_000L + deadlineMinute * 60_000L
            val hoursBefore = customHoursBefore ?: defaultHoursBefore
            return deadline - hoursBefore * 3_600_000L
        }
        // all-day
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
    val year: Int,
    val month: Int, // 1-12
    val day: Int,   // 1-31
    val deadlineHour: Int? = null,
    val deadlineMinute: Int? = null,
    val customHoursBefore: Int? = null,
    val tagName: String? = null
)

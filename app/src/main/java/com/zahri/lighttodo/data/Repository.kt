package com.zahri.lighttodo.data

import com.zahri.lighttodo.data.local.TagDao
import com.zahri.lighttodo.data.local.TagEntity
import com.zahri.lighttodo.data.local.TodoDao
import com.zahri.lighttodo.data.local.TodoEntity
import com.zahri.lighttodo.data.prefs.UserPrefs
import com.zahri.lighttodo.domain.todo.TodoDateFields
import com.zahri.lighttodo.domain.todo.TodoInput
import com.zahri.lighttodo.domain.todo.TodoReminderDefaults
import com.zahri.lighttodo.usecase.calendar.CalendarSyncRepository
import com.zahri.lighttodo.usecase.todo.HomeTag
import com.zahri.lighttodo.usecase.todo.HomeTodoSnapshot
import com.zahri.lighttodo.usecase.todo.TodoPreferencesSnapshot
import com.zahri.lighttodo.usecase.todo.TodoRecord
import com.zahri.lighttodo.usecase.todo.TodoRepository
import com.zahri.lighttodo.usecase.todo.TodoTag
import com.zahri.lighttodo.usecase.todo.toHomeTodo
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
    private val todoDao: TodoDao,
    private val tagDao: TagDao,
    private val prefs: UserPrefs
) : TodoRepository, CalendarSyncRepository {

    val todosFlow: Flow<List<TodoEntity>> = todoDao.observeAll()
    val tagsFlow: Flow<List<TagEntity>> = tagDao.observeAll()

    override fun observeHome(): Flow<HomeTodoSnapshot> =
        combine(todosFlow, tagsFlow, prefs.flow) { todos, tags, p ->
            HomeTodoSnapshot(
                todos = todos.map { it.toTodoRecord().toHomeTodo() },
                tags = tags.map { HomeTag(id = it.id, name = it.name) },
                collapsedTagIds = p.collapsedTagIds,
                doneSectionExpanded = p.doneSectionExpanded
            )
        }

    fun homeFlow(): Flow<HomeData> =
        combine(todosFlow, tagsFlow, prefs.flow) { todos, tags, p -> HomeData(todos, tags, p) }

    override suspend fun prefsSnapshot(): TodoPreferencesSnapshot =
        prefs.flow.first().toTodoPreferencesSnapshot()

    override suspend fun listTags(): List<TodoTag> =
        tagDao.listAll().map { TodoTag(id = it.id, name = it.name) }

    override suspend fun buildTodo(
        input: TodoInput,
        prefsSnapshot: TodoPreferencesSnapshot,
        now: Long
    ): TodoRecord {
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
                defaultHour = prefsSnapshot.defaultRemindHour,
                defaultMinute = prefsSnapshot.defaultRemindMinute,
                allDayFallback = false
            )
            // 截止时间提醒：未覆盖时使用全局默认提前小时数；显式 0 表示到点提醒。
            val endHoursBefore = TodoReminderDefaults.effectiveHoursBefore(
                customHoursBefore = input.customHoursBefore,
                defaultHoursBefore = prefsSnapshot.defaultHoursBefore
            )
            remindEnd = computeRemindAt(
                dateMillis = dateFields.dateMillis,
                hour = input.deadlineHour,
                minute = input.deadlineMinute,
                hoursBefore = endHoursBefore,
                defaultHour = prefsSnapshot.defaultRemindHour,
                defaultMinute = prefsSnapshot.defaultRemindMinute,
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
        return TodoRecord(
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
    }

    override suspend fun upsertTodo(record: TodoRecord, inputId: Long?): TodoRecord {
        val entity = record.toEntity()
        val generatedId = todoDao.upsert(entity)
        return entity.copy(id = inputId ?: generatedId).toTodoRecord()
    }

    override suspend fun setDoneLocal(id: Long, done: Boolean, now: Long): TodoRecord? {
        val before = todoDao.findById(id) ?: return null
        val doneAtMillis = if (done) now else null
        todoDao.setDone(id, done, doneAtMillis)
        return before.copy(done = done, doneAtMillis = doneAtMillis).toTodoRecord()
    }

    override suspend fun findTodoById(id: Long): TodoRecord? =
        todoDao.findById(id)?.toTodoRecord()

    override suspend fun findTodosByIds(ids: List<Long>): List<TodoRecord> =
        todoDao.findByIds(ids).map { it.toTodoRecord() }

    override suspend fun findTodosByCalendarEventIds(eventIds: List<Long>): List<TodoRecord> =
        todoDao.findByCalendarEventIds(eventIds).map { it.toTodoRecord() }

    override suspend fun listAllTodos(): List<TodoRecord> =
        todoDao.listAll().map { it.toTodoRecord() }

    override fun listWidgetTodos(nowMillis: Long, limit: Int): List<TodoRecord> =
        todoDao.listAllUndoneSync(nowMillis, limit).map { it.toTodoRecord() }

    override suspend fun listUndoneCalendarEventIdsInWindow(fromMillis: Long, toMillis: Long): List<Long> =
        todoDao.listUndoneCalendarEventIdsInWindow(fromMillis, toMillis)

    override suspend fun listDoneTodos(): List<TodoRecord> =
        todoDao.listDone().map { it.toTodoRecord() }

    override suspend fun listDoneTodoIdsWithReminders(): List<Long> =
        todoDao.listDoneWithReminders().map { it.id }

    override suspend fun setTodoCalendarLink(id: Long, eventId: Long?, createdByApp: Boolean) {
        todoDao.setCalendarLink(id, eventId, createdByApp)
    }

    override suspend fun deleteLocalTodo(id: Long) {
        todoDao.delete(id)
    }

    override suspend fun deleteLocalTodos(ids: List<Long>) {
        todoDao.deleteByIds(ids)
    }

    override suspend fun upsertTodos(todos: List<TodoRecord>) {
        todoDao.upsertAll(todos.map { it.toEntity() })
    }

    override suspend fun deleteAllDoneTodos() {
        todoDao.deleteAllDone()
    }

    private fun UserPrefs.Snapshot.toTodoPreferencesSnapshot(): TodoPreferencesSnapshot =
        TodoPreferencesSnapshot(
            defaultRemindHour = defaultRemindHour,
            defaultRemindMinute = defaultRemindMinute,
            defaultHoursBefore = defaultHoursBefore,
            calendarSyncEnabled = calendarSyncEnabled,
            calendarAccountName = calendarAccountName
        )

    private suspend fun resolveTagId(name: String?): Long? {
        val n = name?.trim().orEmpty()
        if (n.isEmpty()) return null
        tagDao.findByName(n)?.let { return it.id }
        return tagDao.insert(TagEntity(name = n)).takeIf { it != -1L }
            ?: tagDao.findByName(n)?.id
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

private fun TodoEntity.toTodoRecord(): TodoRecord =
    TodoRecord(
        id = id,
        title = title,
        note = note,
        date = date,
        dateMillis = dateMillis,
        startHour = startHour,
        startMinute = startMinute,
        deadlineHour = deadlineHour,
        deadlineMinute = deadlineMinute,
        remindStartAtMillis = remindStartAtMillis,
        remindAtMillis = remindAtMillis,
        customRemindHoursBefore = customRemindHoursBefore,
        tagId = tagId,
        done = done,
        doneAtMillis = doneAtMillis,
        createdAtMillis = createdAtMillis,
        calendarEventId = calendarEventId,
        calendarCreatedByApp = calendarCreatedByApp
    )

private fun TodoRecord.toEntity(): TodoEntity =
    TodoEntity(
        id = id,
        title = title,
        note = note,
        date = date,
        dateMillis = dateMillis,
        startHour = startHour,
        startMinute = startMinute,
        deadlineHour = deadlineHour,
        deadlineMinute = deadlineMinute,
        remindStartAtMillis = remindStartAtMillis,
        remindAtMillis = remindAtMillis,
        customRemindHoursBefore = customRemindHoursBefore,
        tagId = tagId,
        done = done,
        doneAtMillis = doneAtMillis,
        createdAtMillis = createdAtMillis,
        calendarEventId = calendarEventId,
        calendarCreatedByApp = calendarCreatedByApp
    )

data class HomeData(
    val todos: List<TodoEntity>,
    val tags: List<TagEntity>,
    val prefs: UserPrefs.Snapshot
)

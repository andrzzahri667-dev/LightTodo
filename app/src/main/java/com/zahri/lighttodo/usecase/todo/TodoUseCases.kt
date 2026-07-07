package com.zahri.lighttodo.usecase.todo

import com.zahri.lighttodo.domain.todo.TodoDisplayText
import com.zahri.lighttodo.domain.todo.TodoInput
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TodoRepository {
    fun observeHome(): Flow<HomeTodoSnapshot>
    suspend fun prefsSnapshot(): TodoPreferencesSnapshot
    suspend fun listTags(): List<TodoTag>
    suspend fun buildTodo(input: TodoInput, prefsSnapshot: TodoPreferencesSnapshot, now: Long): TodoRecord
    suspend fun upsertTodo(record: TodoRecord, inputId: Long?): TodoRecord
    suspend fun setDoneLocal(id: Long, done: Boolean, now: Long): TodoRecord?
    suspend fun findTodoById(id: Long): TodoRecord?
    suspend fun findTodosByIds(ids: List<Long>): List<TodoRecord>
    suspend fun listAllTodos(): List<TodoRecord>
    fun listWidgetTodos(nowMillis: Long, limit: Int): List<TodoRecord>
    suspend fun listDoneTodos(): List<TodoRecord>
    suspend fun listDoneTodoIdsWithReminders(): List<Long>
    suspend fun setTodoCalendarLink(id: Long, eventId: Long?, createdByApp: Boolean)
    suspend fun deleteLocalTodo(id: Long)
    suspend fun deleteLocalTodos(ids: List<Long>)
    suspend fun deleteAllDoneTodos()
}

interface HomePreferencesRepository {
    suspend fun setCollapsedTagIds(ids: Set<String>)
    suspend fun setDoneSectionExpanded(expanded: Boolean)
}

interface ReminderGateway {
    fun schedule(todo: TodoRecord, isStart: Boolean)
    fun cancel(todoId: Long)
}

interface CalendarGateway {
    suspend fun <T> withSyncLock(block: suspend () -> T): T
    suspend fun setCompleted(eventId: Long, done: Boolean)
    suspend fun upsertFromTodo(todo: TodoRecord, accountName: String): Long?
    suspend fun deleteEvent(eventId: Long)
}

interface WidgetUpdater {
    fun notifyTodosChanged()
}

class ObserveHomeUseCase(
    private val repository: TodoRepository
) {
    operator fun invoke(): Flow<HomeTodoSnapshot> =
        repository.observeHome()
}

data class HomeTodoSnapshot(
    val todos: List<HomeTodo>,
    val tags: List<HomeTag>,
    val collapsedTagIds: Set<String>,
    val doneSectionExpanded: Boolean
)

data class TodoPreferencesSnapshot(
    val defaultRemindHour: Int = 9,
    val defaultRemindMinute: Int = 0,
    val defaultHoursBefore: Int = 2,
    val calendarSyncEnabled: Boolean = false,
    val calendarAccountName: String = ""
)

data class TodoTag(
    val id: Long,
    val name: String
)

data class TodoRecord(
    val id: Long = 0,
    val title: String? = null,
    val note: String? = null,
    val date: Int? = null,
    val dateMillis: Long? = null,
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val deadlineHour: Int? = null,
    val deadlineMinute: Int? = null,
    val remindStartAtMillis: Long? = null,
    val remindAtMillis: Long? = null,
    val customRemindHoursBefore: Int? = null,
    val tagId: Long? = null,
    val done: Boolean = false,
    val doneAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val calendarEventId: Long? = null,
    val calendarCreatedByApp: Boolean = false
)

data class HomeTag(
    val id: Long,
    val name: String
)

data class HomeTodo(
    val id: Long,
    val title: String?,
    val note: String?,
    val date: Int?,
    val dateMillis: Long?,
    val startHour: Int?,
    val startMinute: Int?,
    val deadlineHour: Int?,
    val deadlineMinute: Int?,
    val tagId: Long?,
    val done: Boolean,
    val doneAtMillis: Long?,
    val createdAtMillis: Long,
    val calendarEventId: Long?
)

fun TodoRecord.toHomeTodo(): HomeTodo =
    HomeTodo(
        id = id,
        title = title,
        note = note,
        date = date,
        dateMillis = dateMillis,
        startHour = startHour,
        startMinute = startMinute,
        deadlineHour = deadlineHour,
        deadlineMinute = deadlineMinute,
        tagId = tagId,
        done = done,
        doneAtMillis = doneAtMillis,
        createdAtMillis = createdAtMillis,
        calendarEventId = calendarEventId
    )

class LoadTodoEditUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(id: Long?, initialTitle: String?): TodoEditSnapshot? {
        val prefsSnapshot = repository.prefsSnapshot()
        val tags = repository.listTags()
        val defaultRemindLabel = "%02d:%02d".format(
            prefsSnapshot.defaultRemindHour,
            prefsSnapshot.defaultRemindMinute
        )
        val tagOptions = tags.map { TodoEditTagOption(it.name) }
        if (id == null) {
            return TodoEditSnapshot(
                id = null,
                title = initialTitle.orEmpty(),
                note = "",
                date = LocalDate.now(),
                startTime = null,
                endTime = null,
                customHoursBefore = null,
                defaultHoursBefore = prefsSnapshot.defaultHoursBefore,
                defaultRemindLabel = defaultRemindLabel,
                tagName = "",
                allTags = tagOptions,
                readOnly = false
            )
        }

        val todo = repository.findTodoById(id) ?: return null
        val tag = todo.tagId?.let { tagId -> tags.firstOrNull { it.id == tagId } }
        return TodoEditSnapshot(
            id = todo.id,
            title = todo.title.orEmpty(),
            note = todo.note.orEmpty(),
            date = todo.date?.let { LocalDate.of(it / 10000, (it / 100) % 100, it % 100) },
            startTime = if (todo.startHour != null && todo.startMinute != null) {
                todo.startHour to todo.startMinute
            } else {
                null
            },
            endTime = if (todo.deadlineHour != null && todo.deadlineMinute != null) {
                todo.deadlineHour to todo.deadlineMinute
            } else {
                null
            },
            customHoursBefore = todo.customRemindHoursBefore,
            defaultHoursBefore = prefsSnapshot.defaultHoursBefore,
            defaultRemindLabel = defaultRemindLabel,
            tagName = tag?.name.orEmpty(),
            allTags = tagOptions,
            readOnly = todo.calendarEventId != null && !todo.calendarCreatedByApp
        )
    }
}

data class TodoEditSnapshot(
    val id: Long?,
    val title: String,
    val note: String,
    val date: LocalDate?,
    val startTime: Pair<Int, Int>?,
    val endTime: Pair<Int, Int>?,
    val customHoursBefore: Int?,
    val defaultHoursBefore: Int,
    val defaultRemindLabel: String,
    val tagName: String,
    val allTags: List<TodoEditTagOption>,
    val readOnly: Boolean
)

data class TodoEditTagOption(
    val name: String
)

class UpdateHomePreferencesUseCase(
    private val prefs: HomePreferencesRepository
) {
    suspend fun setGroupExpanded(
        currentCollapsedTagIds: Set<String>,
        key: String,
        expanded: Boolean
    ) {
        val next = currentCollapsedTagIds.toMutableSet()
        if (expanded) next -= key else next += key
        prefs.setCollapsedTagIds(next)
    }

    suspend fun setDoneExpanded(expanded: Boolean) {
        prefs.setDoneSectionExpanded(expanded)
    }
}

class SaveTodoUseCase(
    private val repository: TodoRepository,
    private val reminderGateway: ReminderGateway,
    private val calendarGateway: CalendarGateway,
    private val widgetUpdater: WidgetUpdater
) {
    suspend operator fun invoke(input: TodoInput): Long {
        val prefsSnapshot = repository.prefsSnapshot()
        val now = System.currentTimeMillis()
        val entity = repository.buildTodo(input, prefsSnapshot, now)
        return if (prefsSnapshot.calendarSyncEnabled) {
            calendarGateway.withSyncLock { persistTodo(entity, prefsSnapshot, now, input.id) }
        } else {
            persistTodo(entity, prefsSnapshot, now, input.id)
        }
    }

    private suspend fun persistTodo(
        record: TodoRecord,
        prefsSnapshot: TodoPreferencesSnapshot,
        now: Long,
        inputId: Long?
    ): Long {
        var saved = repository.upsertTodo(record, inputId)
        scheduleReminders(saved, now)
        val mirrored = mirrorTodoToCalendar(saved, prefsSnapshot)
        if (
            mirrored.calendarEventId != saved.calendarEventId ||
            mirrored.calendarCreatedByApp != saved.calendarCreatedByApp
        ) {
            repository.setTodoCalendarLink(
                id = saved.id,
                eventId = mirrored.calendarEventId,
                createdByApp = mirrored.calendarCreatedByApp
            )
            saved = saved.copy(
                calendarEventId = mirrored.calendarEventId,
                calendarCreatedByApp = mirrored.calendarCreatedByApp
            )
        }
        widgetUpdater.notifyTodosChanged()
        return saved.id
    }

    private fun scheduleReminders(todo: TodoRecord, now: Long) {
        reminderGateway.cancel(todo.id)
        if (!todo.done) {
            if (todo.remindStartAtMillis != null && todo.remindStartAtMillis > now)
                reminderGateway.schedule(todo, isStart = true)
            if (todo.remindAtMillis != null && todo.remindAtMillis > now)
                reminderGateway.schedule(todo, isStart = false)
        }
    }

    private suspend fun mirrorTodoToCalendar(todo: TodoRecord, prefs: TodoPreferencesSnapshot): TodoRecord {
        if (!prefs.calendarSyncEnabled) return todo
        if (todo.dateMillis == null) {
            todo.calendarEventId?.let { calendarGateway.deleteEvent(it) }
            return todo.copy(calendarEventId = null, calendarCreatedByApp = false)
        }
        val eventId = calendarGateway.upsertFromTodo(todo, prefs.calendarAccountName)
        return if (eventId != null) {
            todo.copy(
                calendarEventId = eventId,
                calendarCreatedByApp = todo.calendarCreatedByApp || todo.calendarEventId == null
            )
        } else {
            todo
        }
    }
}

class CompleteTodoUseCase(
    private val repository: TodoRepository,
    private val reminderGateway: ReminderGateway,
    private val calendarGateway: CalendarGateway,
    private val widgetUpdater: WidgetUpdater
) {
    suspend operator fun invoke(id: Long, done: Boolean) {
        val prefsSnapshot = repository.prefsSnapshot()
        if (prefsSnapshot.calendarSyncEnabled) {
            calendarGateway.withSyncLock { setDoneLocked(id, done, prefsSnapshot) }
        } else {
            setDoneLocked(id, done, prefsSnapshot)
        }
    }

    private suspend fun setDoneLocked(id: Long, done: Boolean, prefsSnapshot: TodoPreferencesSnapshot) {
        val updated = repository.setDoneLocal(id, done, System.currentTimeMillis()) ?: return
        if (prefsSnapshot.calendarSyncEnabled) {
            if (done) {
                updated.calendarEventId?.let { calendarGateway.setCompleted(it, true) }
            } else if (updated.dateMillis != null) {
                val eventId = calendarGateway.upsertFromTodo(updated, prefsSnapshot.calendarAccountName)
                if (eventId != null && eventId != updated.calendarEventId) {
                    repository.setTodoCalendarLink(
                        id = id,
                        eventId = eventId,
                        createdByApp = updated.calendarCreatedByApp || updated.calendarEventId == null
                    )
                }
            }
        }
        if (done) reminderGateway.cancel(id)
        else {
            val t = updated
            val now = System.currentTimeMillis()
            if (t.remindStartAtMillis != null && t.remindStartAtMillis > now)
                reminderGateway.schedule(t, isStart = true)
            if (t.remindAtMillis != null && t.remindAtMillis > now)
                reminderGateway.schedule(t, isStart = false)
        }
        widgetUpdater.notifyTodosChanged()
    }
}

class DeleteTodoUseCase(
    private val repository: TodoRepository,
    private val reminderGateway: ReminderGateway,
    private val calendarGateway: CalendarGateway,
    private val widgetUpdater: WidgetUpdater
) {
    suspend fun delete(id: Long) {
        val prefsSnapshot = repository.prefsSnapshot()
        if (prefsSnapshot.calendarSyncEnabled) {
            calendarGateway.withSyncLock {
                repository.findTodoById(id)?.calendarEventId?.let { eventId ->
                    calendarGateway.deleteEvent(eventId)
                }
                deleteLocalTodo(id)
            }
        } else {
            deleteLocalTodo(id)
        }
    }

    suspend fun deleteMany(ids: List<Long>) {
        if (ids.isEmpty()) return
        val prefsSnapshot = repository.prefsSnapshot()
        if (prefsSnapshot.calendarSyncEnabled) {
            calendarGateway.withSyncLock {
                val calendarEventIds = repository.findTodosByIds(ids)
                    .mapNotNull { it.calendarEventId }
                calendarEventIds.forEach { calendarGateway.deleteEvent(it) }
                deleteLocalTodos(ids)
            }
        } else {
            deleteLocalTodos(ids)
        }
    }

    suspend fun clearDone() {
        val prefsSnapshot = repository.prefsSnapshot()
        if (prefsSnapshot.calendarSyncEnabled) {
            calendarGateway.withSyncLock {
                val doneTodos = repository.listDoneTodos()
                val doneReminderIds = repository.listDoneTodoIdsWithReminders()
                doneTodos.mapNotNull { it.calendarEventId }
                    .forEach { calendarGateway.deleteEvent(it) }
                repository.deleteAllDoneTodos()
                doneReminderIds.forEach(reminderGateway::cancel)
                widgetUpdater.notifyTodosChanged()
            }
        } else {
            val doneReminderIds = repository.listDoneTodoIdsWithReminders()
            repository.deleteAllDoneTodos()
            doneReminderIds.forEach(reminderGateway::cancel)
            widgetUpdater.notifyTodosChanged()
        }
    }

    private suspend fun deleteLocalTodo(id: Long) {
        repository.deleteLocalTodo(id)
        reminderGateway.cancel(id)
        widgetUpdater.notifyTodosChanged()
    }

    private suspend fun deleteLocalTodos(ids: List<Long>) {
        repository.deleteLocalTodos(ids)
        ids.forEach(reminderGateway::cancel)
        widgetUpdater.notifyTodosChanged()
    }
}

class RescheduleRemindersUseCase(
    private val repository: TodoRepository,
    private val reminderGateway: ReminderGateway
) {
    suspend operator fun invoke() {
        val allTodos = repository.listAllTodos()
        allTodos.forEach { t -> reminderGateway.cancel(t.id) }
        val list = allTodos.filter { !it.done && it.hasAnyReminder() }
        val now = System.currentTimeMillis()
        list.forEach { t ->
            if (t.remindStartAtMillis != null && t.remindStartAtMillis > now)
                reminderGateway.schedule(t, isStart = true)
            if (t.remindAtMillis != null && t.remindAtMillis > now)
                reminderGateway.schedule(t, isStart = false)
        }
    }
}

class LoadReminderDialogUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(todoId: Long, fallbackTitle: String): TodoReminderDialog? {
        val todo = repository.findTodoById(todoId) ?: return null
        return TodoReminderDialog(
            title = todoReminderTitle(todo, fallbackTitle),
            note = todo.notePreview()
        )
    }
}

data class TodoReminderDialog(
    val title: String,
    val note: String
)

class LoadReminderNotificationUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(todoId: Long, fallbackTitle: String): TodoReminderNotification? {
        val todo = repository.findTodoById(todoId) ?: return null
        if (todo.done) return null
        return TodoReminderNotification(
            title = todoReminderTitle(todo, fallbackTitle),
            notePreview = todo.notePreview(),
            startHour = todo.startHour,
            startMinute = todo.startMinute,
            deadlineHour = todo.deadlineHour,
            deadlineMinute = todo.deadlineMinute
        )
    }
}

data class TodoReminderNotification(
    val title: String,
    val notePreview: String,
    val startHour: Int?,
    val startMinute: Int?,
    val deadlineHour: Int?,
    val deadlineMinute: Int?
)

class LoadWidgetCompletionAnimationUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(todoId: Long, fallbackTitle: String): WidgetCompletionAnimationTodo? {
        val todo = repository.findTodoById(todoId) ?: return null
        return WidgetCompletionAnimationTodo(
            title = todoReminderTitle(todo, fallbackTitle)
        )
    }
}

data class WidgetCompletionAnimationTodo(
    val title: String
)

class LoadWidgetTodosUseCase(
    private val repository: TodoRepository
) {
    operator fun invoke(nowMillis: Long, limit: Int): List<TodoRecord> =
        repository.listWidgetTodos(nowMillis, limit)
}

private fun todoReminderTitle(todo: TodoRecord, fallbackTitle: String): String =
    TodoDisplayText.title(
        title = todo.title,
        note = todo.note,
        fallback = fallbackTitle
    )

private fun TodoRecord.notePreview(): String =
    note?.lineSequence()?.firstOrNull().orEmpty()

private fun TodoRecord.hasAnyReminder(): Boolean =
    remindStartAtMillis != null || remindAtMillis != null

data class TodoUseCases(
    val observeHome: ObserveHomeUseCase,
    val loadTodoEdit: LoadTodoEditUseCase,
    val updateHomePreferences: UpdateHomePreferencesUseCase,
    val saveTodo: SaveTodoUseCase,
    val completeTodo: CompleteTodoUseCase,
    val deleteTodo: DeleteTodoUseCase,
    val rescheduleReminders: RescheduleRemindersUseCase,
    val loadReminderDialog: LoadReminderDialogUseCase,
    val loadReminderNotification: LoadReminderNotificationUseCase,
    val loadWidgetCompletionAnimation: LoadWidgetCompletionAnimationUseCase,
    val loadWidgetTodos: LoadWidgetTodosUseCase
)

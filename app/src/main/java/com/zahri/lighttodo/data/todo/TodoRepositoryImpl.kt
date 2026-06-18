package com.zahri.lighttodo.data.todo

import com.zahri.lighttodo.data.local.TagDao
import com.zahri.lighttodo.data.local.TodoDao
import com.zahri.lighttodo.data.prefs.UserPrefs
import com.zahri.lighttodo.domain.todo.TodoInput
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

class TodoRepositoryImpl(
    private val todoDao: TodoDao,
    private val tagDao: TagDao,
    private val prefs: UserPrefs
) : TodoRepository {
    private val recordBuilder = TodoInputRecordBuilder(todoDao, tagDao)

    override fun observeHome(): Flow<HomeTodoSnapshot> =
        combine(todoDao.observeAll(), tagDao.observeAll(), prefs.flow) { todos, tags, p ->
            HomeTodoSnapshot(
                todos = todos.map { it.toTodoRecord().toHomeTodo() },
                tags = tags.map { HomeTag(id = it.id, name = it.name) },
                collapsedTagIds = p.collapsedTagIds,
                doneSectionExpanded = p.doneSectionExpanded
            )
        }

    override suspend fun prefsSnapshot(): TodoPreferencesSnapshot =
        prefs.flow.first().toTodoPreferencesSnapshot()

    override suspend fun listTags(): List<TodoTag> =
        tagDao.listAll().map { TodoTag(id = it.id, name = it.name) }

    override suspend fun buildTodo(
        input: TodoInput,
        prefsSnapshot: TodoPreferencesSnapshot,
        now: Long
    ): TodoRecord =
        recordBuilder.build(input, prefsSnapshot, now)

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

    override suspend fun listAllTodos(): List<TodoRecord> =
        todoDao.listAll().map { it.toTodoRecord() }

    override fun listWidgetTodos(nowMillis: Long, limit: Int): List<TodoRecord> =
        todoDao.listAllUndoneSync(nowMillis, limit).map { it.toTodoRecord() }

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

    override suspend fun deleteAllDoneTodos() {
        todoDao.deleteAllDone()
    }
}

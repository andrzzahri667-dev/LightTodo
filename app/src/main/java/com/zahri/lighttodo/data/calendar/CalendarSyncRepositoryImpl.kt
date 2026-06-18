package com.zahri.lighttodo.data.calendar

import com.zahri.lighttodo.data.local.TodoDao
import com.zahri.lighttodo.data.todo.toEntity
import com.zahri.lighttodo.data.todo.toTodoRecord
import com.zahri.lighttodo.usecase.calendar.CalendarSyncRepository
import com.zahri.lighttodo.usecase.todo.TodoRecord

class CalendarSyncRepositoryImpl(
    private val todoDao: TodoDao
) : CalendarSyncRepository {
    override suspend fun listUndoneCalendarEventIdsInWindow(fromMillis: Long, toMillis: Long): List<Long> =
        todoDao.listUndoneCalendarEventIdsInWindow(fromMillis, toMillis)

    override suspend fun findTodosByCalendarEventIds(eventIds: List<Long>): List<TodoRecord> =
        todoDao.findByCalendarEventIds(eventIds).map { it.toTodoRecord() }

    override suspend fun upsertTodos(todos: List<TodoRecord>) {
        todoDao.upsertAll(todos.map { it.toEntity() })
    }

    override suspend fun setTodoCalendarLink(id: Long, eventId: Long?, createdByApp: Boolean) {
        todoDao.setCalendarLink(id, eventId, createdByApp)
    }

    override suspend fun deleteLocalTodos(ids: List<Long>) {
        todoDao.deleteByIds(ids)
    }
}

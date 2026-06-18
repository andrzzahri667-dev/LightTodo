package com.zahri.lighttodo.integration.calendar

import android.content.Context
import com.zahri.lighttodo.usecase.todo.CalendarGateway
import com.zahri.lighttodo.usecase.todo.TodoRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidCalendarGateway(
    private val context: Context
) : CalendarGateway {
    override suspend fun <T> withSyncLock(block: suspend () -> T): T =
        CalendarSyncCoordinator.withLock(block)

    override suspend fun setCompleted(eventId: Long, done: Boolean) {
        withContext(Dispatchers.IO) {
            CalendarEventWriter.setCompleted(context, eventId, done)
        }
    }

    override suspend fun upsertFromTodo(todo: TodoRecord, accountName: String): Long? =
        withContext(Dispatchers.IO) {
            CalendarEventWriter.upsertFromTodo(context, todo, accountName)
        }

    override suspend fun deleteEvent(eventId: Long) {
        withContext(Dispatchers.IO) {
            CalendarEventWriter.deleteEvent(context, eventId)
        }
    }
}

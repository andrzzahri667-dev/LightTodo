package com.zahri.lighttodo.data

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryAlarmSourceTest {
    @Test
    fun deleteManyDeletesRowsBeforeCancellingAlarms() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/todo/TodoUseCases.kt").readText()

        assertTrue(source.indexOf("repository.deleteLocalTodos(ids)") < source.indexOf("ids.forEach(reminderGateway::cancel)"))
    }

    @Test
    fun clearDoneCancelsAnyDoneTodoAlarmsAfterDeletingRows() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/todo/TodoUseCases.kt").readText()

        assertTrue(source.contains("val doneReminderIds = repository.listDoneTodoIdsWithReminders()"))
        assertTrue(source.indexOf("repository.deleteAllDoneTodos()") < source.indexOf("doneReminderIds.forEach(reminderGateway::cancel)"))
    }

    @Test
    fun rescheduleAllAlarmsCancelsExistingTodoAlarmsBeforeSchedulingCurrentReminders() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/todo/TodoUseCases.kt").readText()

        assertTrue(source.contains("val allTodos = repository.listAllTodos()"))
        assertTrue(source.contains("allTodos.forEach { t -> reminderGateway.cancel(t.id) }"))
        assertTrue(source.contains("val list = allTodos.filter { !it.done && it.hasAnyReminder() }"))
    }

    @Test
    fun setDoneReusesUpdatedEntityWhenReschedulingAlarms() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/usecase/todo/TodoUseCases.kt").readText()

        val setDoneStart = source.indexOf("private suspend fun setDoneLocked")
        val deleteStart = source.indexOf("class DeleteTodoUseCase")
        val setDoneBody = source.substring(setDoneStart, deleteStart)

        assertFalse(setDoneBody.contains("repository.findTodoById(id)"))
        assertTrue(setDoneBody.contains("val t = updated"))
    }
}

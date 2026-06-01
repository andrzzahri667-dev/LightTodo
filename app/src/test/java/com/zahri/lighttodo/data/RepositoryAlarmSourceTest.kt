package com.zahri.lighttodo.data

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryAlarmSourceTest {
    @Test
    fun deleteManyDeletesRowsBeforeCancellingAlarms() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/Repository.kt").readText()

        assertTrue(source.indexOf("todoDao.deleteByIds(ids)") < source.indexOf("ids.forEach { id -> ReminderScheduler.cancel(context, id) }"))
    }

    @Test
    fun clearDoneCancelsAnyDoneTodoAlarmsAfterDeletingRows() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/Repository.kt").readText()

        assertTrue(source.contains("val doneReminderIds = todoDao.listDoneWithReminders().map { it.id }"))
        assertTrue(source.indexOf("todoDao.deleteAllDone()") < source.indexOf("doneReminderIds.forEach { id -> ReminderScheduler.cancel(context, id) }"))
    }

    @Test
    fun rescheduleAllAlarmsCancelsExistingTodoAlarmsBeforeSchedulingCurrentReminders() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/Repository.kt").readText()

        assertTrue(source.contains("val allTodos = todoDao.listAll()"))
        assertTrue(source.contains("allTodos.forEach { t -> ReminderScheduler.cancel(context, t.id) }"))
        assertTrue(source.contains("val list = allTodos.filter { !it.done && it.hasAnyReminder() }"))
    }

    @Test
    fun setDoneReusesUpdatedEntityWhenReschedulingAlarms() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/Repository.kt").readText()

        val setDoneStart = source.indexOf("private suspend fun setDoneLocked")
        val deleteStart = source.indexOf("suspend fun delete(id: Long)")
        val setDoneBody = source.substring(setDoneStart, deleteStart)

        assertFalse(setDoneBody.contains("val t = todoDao.findById(id)"))
        assertTrue(setDoneBody.contains("val t = updated"))
    }
}

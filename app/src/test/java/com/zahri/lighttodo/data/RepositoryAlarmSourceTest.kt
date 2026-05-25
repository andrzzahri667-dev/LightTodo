package com.zahri.lighttodo.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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

    private fun sourceFile(relativePath: String): File {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        var dir = File(userDir).absoluteFile
        while (true) {
            val candidate = File(dir, relativePath)
            if (candidate.exists()) return candidate
            dir = dir.parentFile ?: break
        }
        error("Could not find $relativePath from $userDir")
    }
}

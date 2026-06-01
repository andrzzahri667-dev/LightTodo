package com.zahri.lighttodo.data

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerSourceTest {
    @Test
    fun restoreCollectsOldTodoIdsInsideTransactionBeforeReplacingData() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/BackupManager.kt").readText()

        assertTrue(source.contains("val oldTodoIds = db.withTransaction {"))
        assertTrue(source.indexOf("val oldTodoIds = db.todoDao().listAll().map { it.id }") < source.indexOf("db.todoDao().deleteAll()"))
        assertTrue(source.indexOf("db.noteDao().deleteAll()") < source.indexOf("oldTodoIds.forEach { id -> ReminderScheduler.cancel(context, id) }"))
    }

    @Test
    fun restoreIfEmptyLogsDecodeOrRestoreFailures() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/BackupManager.kt").readText()

        assertTrue(source.contains("Log.w("))
        assertTrue(source.contains("Auto restore failed"))
    }
}

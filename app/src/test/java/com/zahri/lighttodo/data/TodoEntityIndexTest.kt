package com.zahri.lighttodo.data

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertTrue
import org.junit.Test

class TodoEntityIndexTest {
    @Test
    fun todoEntityDeclaresCompositeIndexesForHomeAndReminderQueries() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/local/Entities.kt").readText()

        assertTrue(source.contains("Index(value = [\"done\", \"dateMillis\", \"createdAtMillis\"])"))
        assertTrue(source.contains("Index(value = [\"done\", \"remindStartAtMillis\", \"remindAtMillis\"])"))
    }

    @Test
    fun databaseMigrationCreatesCompositeTodoIndexesForExistingInstalls() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/local/AppDatabase.kt").readText()

        assertTrue(source.contains("version = 6"))
        assertTrue(source.contains("CREATE INDEX IF NOT EXISTS index_todo_done_dateMillis_createdAtMillis"))
        assertTrue(source.contains("CREATE INDEX IF NOT EXISTS index_todo_done_remindStartAtMillis_remindAtMillis"))
        assertTrue(source.contains("ALTER TABLE todo ADD COLUMN calendarCreatedByApp INTEGER NOT NULL DEFAULT 0"))
        assertTrue(source.contains("MIGRATION_5_6"))
    }
}

package com.zahri.lighttodo.data

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TodoEntityIndexTest {
    @Test
    fun todoEntityDeclaresCompositeIndexesForHomeAndReminderQueries() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/Entities.kt").readText()

        assertTrue(source.contains("Index(value = [\"done\", \"dateMillis\", \"createdAtMillis\"])"))
        assertTrue(source.contains("Index(value = [\"done\", \"remindStartAtMillis\", \"remindAtMillis\"])"))
    }

    @Test
    fun databaseMigrationCreatesCompositeTodoIndexesForExistingInstalls() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/AppDatabase.kt").readText()

        assertTrue(source.contains("version = 5"))
        assertTrue(source.contains("CREATE INDEX IF NOT EXISTS index_todo_done_dateMillis_createdAtMillis"))
        assertTrue(source.contains("CREATE INDEX IF NOT EXISTS index_todo_done_remindStartAtMillis_remindAtMillis"))
        assertTrue(source.contains(".addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)"))
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

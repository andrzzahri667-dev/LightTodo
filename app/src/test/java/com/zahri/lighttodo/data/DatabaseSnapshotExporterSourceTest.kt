package com.zahri.lighttodo.data

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseSnapshotExporterSourceTest {

    @Test
    fun exportSerializesCheckpointAndFileCopy() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/local/DatabaseSnapshotExporter.kt")
            .readText()

        assertTrue(source.contains("private val exportLock = Any()"))
        assertTrue(source.contains("synchronized(exportLock)"))
        assertTrue(source.indexOf("PRAGMA wal_checkpoint(FULL)") < source.indexOf("source.copyTo(target, overwrite = true)"))
    }

    @Test
    fun exportUsesSharedDatabaseName() {
        val exporterSource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/local/DatabaseSnapshotExporter.kt")
            .readText()
        val databaseSource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/local/AppDatabase.kt")
            .readText()

        assertTrue(databaseSource.contains("const val DatabaseName = \"lighttodo.db\""))
        assertTrue(exporterSource.contains("val dbName = AppDatabase.DatabaseName"))
        assertFalse(exporterSource.contains("val dbName = \"lighttodo.db\""))
    }
}

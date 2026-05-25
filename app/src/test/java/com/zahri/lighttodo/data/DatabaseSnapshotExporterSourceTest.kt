package com.zahri.lighttodo.data

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseSnapshotExporterSourceTest {

    @Test
    fun exportSerializesCheckpointAndFileCopy() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/DatabaseSnapshotExporter.kt")
            .readText()

        assertTrue(source.contains("private val exportLock = Any()"))
        assertTrue(source.contains("synchronized(exportLock)"))
        assertTrue(source.indexOf("PRAGMA wal_checkpoint(FULL)") < source.indexOf("source.copyTo(target, overwrite = true)"))
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

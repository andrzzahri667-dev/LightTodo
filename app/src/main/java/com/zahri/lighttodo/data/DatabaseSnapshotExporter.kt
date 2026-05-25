package com.zahri.lighttodo.data

import android.content.Context
import java.io.File

object DatabaseSnapshotExporter {
    private val exportLock = Any()

    fun export(context: Context, db: AppDatabase): File {
        synchronized(exportLock) {
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            val appContext = context.applicationContext
            val targetDir = File(appContext.getExternalFilesDir(null), "database-snapshot").apply {
                mkdirs()
            }
            val dbName = "lighttodo.db"
            val sourceDir = appContext.getDatabasePath(dbName).parentFile
                ?: error("Database directory not found")

            listOf(dbName, "$dbName-wal", "$dbName-shm").forEach { fileName ->
                val source = File(sourceDir, fileName)
                val target = File(targetDir, fileName)
                if (source.exists()) {
                    source.copyTo(target, overwrite = true)
                } else if (target.exists()) {
                    target.delete()
                }
            }

            return targetDir
        }
    }
}

package com.zahri.lighttodo.data

import com.zahri.lighttodo.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStorageNamesTest {

    @Test
    fun labBuildUsesDistinctApplicationIdAndStorageNames() {
        assertEquals("com.zahri.lighttodo.lab", BuildConfig.APPLICATION_ID)
        assertEquals("lighttodo-lab.db", BuildConfig.DB_NAME)
        assertEquals("lighttodo-lab-auto-backup.json", BuildConfig.BACKUP_FILE_NAME)
        assertTrue(BuildConfig.DB_NAME.contains("lab"))
        assertTrue(BuildConfig.BACKUP_FILE_NAME.contains("lab"))
    }
}

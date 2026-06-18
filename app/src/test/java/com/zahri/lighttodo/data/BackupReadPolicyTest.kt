package com.zahri.lighttodo.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupReadPolicyTest {
    @Test
    fun canReadBackupSize_rejectsOversizedBackupFiles() {
        assertTrue(BackupReadPolicy.canReadBackupSize(BackupReadPolicy.MaxBackupBytes))
        assertFalse(BackupReadPolicy.canReadBackupSize(BackupReadPolicy.MaxBackupBytes + 1))
    }

    @Test
    fun canReadBackupSize_rejectsNegativeSizes() {
        assertFalse(BackupReadPolicy.canReadBackupSize(-1))
    }
}

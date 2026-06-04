package com.zahri.lighttodo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupStoragePolicyTest {

    @Test
    fun publicDocumentsMode_usesScopedStorageFromAndroid10() {
        assertEquals(
            BackupStoragePolicy.PublicDocumentsMode.MediaStore,
            BackupStoragePolicy.publicDocumentsMode(sdkInt = 29)
        )
        assertEquals(
            BackupStoragePolicy.PublicDocumentsMode.MediaStore,
            BackupStoragePolicy.publicDocumentsMode(sdkInt = 35)
        )
    }

    @Test
    fun publicDocumentsMode_usesLegacyDirectPathBeforeAndroid10() {
        assertEquals(
            BackupStoragePolicy.PublicDocumentsMode.LegacyDirectPath,
            BackupStoragePolicy.publicDocumentsMode(sdkInt = 28)
        )
    }

    @Test
    fun publicDownloadsMode_usesScopedStorageFromAndroid10() {
        assertEquals(
            BackupStoragePolicy.PublicDownloadsMode.MediaStore,
            BackupStoragePolicy.publicDownloadsMode(sdkInt = 29)
        )
        assertEquals(
            BackupStoragePolicy.PublicDownloadsMode.MediaStore,
            BackupStoragePolicy.publicDownloadsMode(sdkInt = 35)
        )
    }

    @Test
    fun publicDownloadsMode_usesLegacyDirectPathBeforeAndroid10() {
        assertEquals(
            BackupStoragePolicy.PublicDownloadsMode.LegacyDirectPath,
            BackupStoragePolicy.publicDownloadsMode(sdkInt = 28)
        )
    }

    @Test
    fun backupNeverNeedsAllFilesAccess() {
        assertFalse(BackupStoragePolicy.needsAllFilesAccessForRestore())
    }
}

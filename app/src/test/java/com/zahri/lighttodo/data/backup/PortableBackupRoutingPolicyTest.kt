package com.zahri.lighttodo.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class PortableBackupRoutingPolicyTest {
    @Test
    fun android10PlusWithoutWritableTreeSkipsPublicDocuments() {
        assertEquals(
            PortableBackupRoutingPolicy.Destination.SkipPublicDocuments,
            PortableBackupRoutingPolicy.destination(
                treeUri = null,
                treeUriHasWritePermission = false,
                publicDocumentsMode = BackupStoragePolicy.PublicDocumentsMode.MediaStore
            )
        )
        assertEquals(
            PortableBackupRoutingPolicy.Destination.SkipPublicDocuments,
            PortableBackupRoutingPolicy.destination(
                treeUri = "content://tree/stale",
                treeUriHasWritePermission = false,
                publicDocumentsMode = BackupStoragePolicy.PublicDocumentsMode.MediaStore
            )
        )
    }

    @Test
    fun writableTreeUsesSafWriter() {
        assertEquals(
            PortableBackupRoutingPolicy.Destination.DocumentTree,
            PortableBackupRoutingPolicy.destination(
                treeUri = "content://tree/backup",
                treeUriHasWritePermission = true,
                publicDocumentsMode = BackupStoragePolicy.PublicDocumentsMode.MediaStore
            )
        )
    }

    @Test
    fun legacyDirectPathStillWritesPublicDocumentsWithoutTree() {
        assertEquals(
            PortableBackupRoutingPolicy.Destination.LegacyPublicDocuments,
            PortableBackupRoutingPolicy.destination(
                treeUri = null,
                treeUriHasWritePermission = false,
                publicDocumentsMode = BackupStoragePolicy.PublicDocumentsMode.LegacyDirectPath
            )
        )
    }
}

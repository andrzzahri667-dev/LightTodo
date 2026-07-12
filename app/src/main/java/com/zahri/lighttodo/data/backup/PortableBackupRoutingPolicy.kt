package com.zahri.lighttodo.data.backup

internal object PortableBackupRoutingPolicy {
    enum class Destination {
        DocumentTree,
        LegacyPublicDocuments,
        SkipPublicDocuments
    }

    fun destination(
        treeUri: String?,
        treeUriHasWritePermission: Boolean,
        publicDocumentsMode: BackupStoragePolicy.PublicDocumentsMode
    ): Destination =
        if (treeUri != null && treeUriHasWritePermission) {
            Destination.DocumentTree
        } else {
            when (publicDocumentsMode) {
                BackupStoragePolicy.PublicDocumentsMode.MediaStore -> Destination.SkipPublicDocuments
                BackupStoragePolicy.PublicDocumentsMode.LegacyDirectPath -> Destination.LegacyPublicDocuments
            }
        }
}

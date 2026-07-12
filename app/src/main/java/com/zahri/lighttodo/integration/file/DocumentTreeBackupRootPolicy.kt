package com.zahri.lighttodo.integration.file

object DocumentTreeBackupRootPolicy {
    fun useSelectedTreeAsBackupRoot(
        selectedDisplayName: String?,
        selectedDocumentId: String?,
        rootDirName: String
    ): Boolean =
        selectedDisplayName == rootDirName ||
            (selectedDisplayName == null && selectedDocumentId.documentPathSegments()?.lastOrNull() == rootDirName)

    private fun String?.documentPathSegments(): List<String>? =
        this
            ?.substringAfter(':', missingDelimiterValue = this)
            ?.trim('/')
            ?.split('/')
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
}

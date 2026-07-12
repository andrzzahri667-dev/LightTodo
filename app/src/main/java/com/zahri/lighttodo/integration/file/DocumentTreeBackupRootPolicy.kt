package com.zahri.lighttodo.integration.file

object DocumentTreeBackupRootPolicy {
    fun useSelectedTreeAsBackupRoot(
        selectedDisplayName: String?,
        selectedDocumentId: String?,
        rootDirName: String
    ): Boolean {
        val segments = selectedDocumentId.documentPathSegments()
        val isDocumentsBackupRoot =
            segments != null &&
                segments.size >= 2 &&
                segments[segments.lastIndex - 1] == DocumentsDirName &&
                segments.last() == rootDirName
        return isDocumentsBackupRoot && (selectedDisplayName == null || selectedDisplayName == rootDirName)
    }

    private fun String?.documentPathSegments(): List<String>? =
        this
            ?.substringAfter(':', missingDelimiterValue = this)
            ?.trim('/')
            ?.split('/')
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }

    private const val DocumentsDirName = "Documents"
}

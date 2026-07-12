package com.zahri.lighttodo.usecase.file

import android.net.Uri
import java.io.File

interface FileGateway {
    fun providerUri(file: File): Uri
    fun copyUriToFile(uri: Uri, target: File)
    fun imageExtension(uri: Uri): String

    fun publicDocumentDir(rootDirName: String, subdir: String): File
    fun publicDocumentFile(rootDirName: String, subdir: String, displayName: String): File
    fun writePublicDocumentFile(
        rootDirName: String,
        subdir: String,
        displayName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean
    fun readPublicDocumentFile(rootDirName: String, subdir: String, displayName: String): ByteArray?
    fun deletePublicDocumentFile(rootDirName: String, subdir: String, displayName: String): Boolean
    fun findPublicDocumentFile(rootDirName: String, subdir: String, displayName: String): PublicFileInfo?
    fun listPublicDocumentFiles(
        rootDirName: String,
        subdir: String,
        requireMimeType: Boolean
    ): List<PublicFileInfo>

    fun publicDownloadFile(displayName: String): File
    fun findPublicDownloadFile(displayName: String): PublicFileInfo?
    fun readPublicDownloadFile(displayName: String): ByteArray?

    fun readDocumentTreeFile(
        treeUri: Uri,
        rootDirName: String,
        subdir: String,
        displayName: String
    ): ByteArray?
    fun writeDocumentTreeFile(
        treeUri: Uri,
        rootDirName: String,
        subdir: String,
        displayName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean
    fun deleteDocumentTreeFile(
        treeUri: Uri,
        rootDirName: String,
        subdir: String,
        displayName: String
    ): Boolean
    fun listDocumentTreeFiles(
        treeUri: Uri,
        rootDirName: String,
        subdir: String
    ): List<PublicFileInfo>
    fun hasPersistedDocumentTreeWritePermission(treeUri: Uri): Boolean
}

data class PublicFileInfo(
    val displayName: String,
    val sizeBytes: Long? = null,
    val modifiedAtMillis: Long? = null
)

package com.zahri.lighttodo.integration.file

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.zahri.lighttodo.usecase.file.FileGateway
import com.zahri.lighttodo.usecase.file.PublicFileInfo
import java.io.File

class AndroidFileGateway(
    context: Context
) : FileGateway {
    private val appContext = context.applicationContext ?: context

    override fun providerUri(file: File): Uri =
        FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)

    override fun copyUriToFile(uri: Uri, target: File) {
        appContext.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open selected image" }
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }

    override fun imageExtension(uri: Uri): String {
        val mimeType = appContext.contentResolver.getType(uri)
        val fromMime = mimeType?.let {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
        }
        return when (fromMime?.lowercase()) {
            "png", "webp", "gif", "jpg", "jpeg" -> fromMime.lowercase()
            else -> "jpg"
        }
    }

    override fun publicDocumentDir(rootDirName: String, subdir: String): File {
        val root = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            rootDirName
        )
        return if (subdir.isBlank()) root else File(root, subdir)
    }

    override fun publicDocumentFile(rootDirName: String, subdir: String, displayName: String): File =
        File(publicDocumentDir(rootDirName, subdir), displayName)

    override fun writePublicDocumentFile(
        rootDirName: String,
        subdir: String,
        displayName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val resolver = appContext.contentResolver
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            var inserted = false
            val uri = findPublicDocumentUri(rootDirName, subdir, displayName) ?: resolver.insert(
                collection,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath(rootDirName, subdir))
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            )?.also { inserted = true } ?: return false

            resolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } ?: return false
            if (inserted) {
                ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }.also { resolver.update(uri, it, null, null) }
            }
            true
        }.getOrDefault(false)
    }

    override fun readPublicDocumentFile(rootDirName: String, subdir: String, displayName: String): ByteArray? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val uri = findPublicDocumentUri(rootDirName, subdir, displayName) ?: return null
            appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
    }

    override fun deletePublicDocumentFile(rootDirName: String, subdir: String, displayName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val uri = findPublicDocumentUri(rootDirName, subdir, displayName) ?: return true
            appContext.contentResolver.delete(uri, null, null)
            true
        }.getOrDefault(false)
    }

    override fun findPublicDocumentFile(
        rootDirName: String,
        subdir: String,
        displayName: String
    ): PublicFileInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        val selection =
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        val args = arrayOf(displayName, relativePath(rootDirName, subdir))
        val sort = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        return appContext.contentResolver.query(collection, projection, selection, args, sort)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            PublicFileInfo(
                displayName = cursor.getString(0).orEmpty(),
                sizeBytes = if (cursor.isNull(1)) null else cursor.getLong(1),
                modifiedAtMillis = if (cursor.isNull(2)) null else cursor.getLong(2) * 1000
            )
        }
    }

    override fun listPublicDocumentFiles(
        rootDirName: String,
        subdir: String,
        requireMimeType: Boolean
    ): List<PublicFileInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        val selection = buildString {
            append("${MediaStore.MediaColumns.RELATIVE_PATH} = ?")
            if (requireMimeType) append(" AND ${MediaStore.MediaColumns.MIME_TYPE} IS NOT NULL")
        }
        val args = arrayOf(relativePath(rootDirName, subdir))
        return appContext.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        PublicFileInfo(
                            displayName = cursor.getString(0).orEmpty(),
                            sizeBytes = if (cursor.isNull(1)) null else cursor.getLong(1),
                            modifiedAtMillis = if (cursor.isNull(2)) null else cursor.getLong(2) * 1000
                        )
                    )
                }
            }
        }.orEmpty()
    }

    override fun publicDownloadFile(displayName: String): File =
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            displayName
        )

    override fun findPublicDownloadFile(displayName: String): PublicFileInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.Downloads.DISPLAY_NAME, MediaStore.MediaColumns.SIZE)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val args = arrayOf(displayName)
        val sort = "${MediaStore.Downloads.DATE_MODIFIED} DESC"
        return appContext.contentResolver.query(collection, projection, selection, args, sort)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            PublicFileInfo(
                displayName = cursor.getString(0).orEmpty(),
                sizeBytes = if (cursor.isNull(1)) null else cursor.getLong(1)
            )
        }
    }

    override fun readPublicDownloadFile(displayName: String): ByteArray? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val uri = findPublicDownloadUri(displayName) ?: return null
            appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
    }

    override fun readDocumentTreeFile(
        treeUri: Uri,
        rootDirName: String,
        subdir: String,
        displayName: String
    ): ByteArray? {
        val reader = TreeReader(appContext, treeUri, rootDirName)
        return reader.readBytes(subdir, displayName)
    }

    override fun writeDocumentTreeFile(
        treeUri: Uri,
        rootDirName: String,
        subdir: String,
        displayName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean {
        val tree = TreeReader(appContext, treeUri, rootDirName)
        return tree.writeBytes(subdir, displayName, mimeType, bytes)
    }

    override fun deleteDocumentTreeFile(
        treeUri: Uri,
        rootDirName: String,
        subdir: String,
        displayName: String
    ): Boolean {
        val tree = TreeReader(appContext, treeUri, rootDirName)
        return tree.delete(subdir, displayName)
    }

    override fun listDocumentTreeFiles(
        treeUri: Uri,
        rootDirName: String,
        subdir: String
    ): List<PublicFileInfo> {
        val tree = TreeReader(appContext, treeUri, rootDirName)
        return tree.listFiles(subdir)
    }

    override fun hasPersistedDocumentTreeWritePermission(treeUri: Uri): Boolean =
        appContext.contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == treeUri &&
                permission.isReadPermission &&
                permission.isWritePermission
        }

    private fun findPublicDocumentUri(rootDirName: String, subdir: String, displayName: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection =
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        val args = arrayOf(displayName, relativePath(rootDirName, subdir))
        val sort = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        return appContext.contentResolver.query(collection, projection, selection, args, sort)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            ContentUris.withAppendedId(collection, cursor.getLong(0))
        }
    }

    private fun findPublicDownloadUri(displayName: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val args = arrayOf(displayName)
        val sort = "${MediaStore.Downloads.DATE_MODIFIED} DESC"
        return appContext.contentResolver.query(collection, projection, selection, args, sort)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            ContentUris.withAppendedId(collection, cursor.getLong(0))
        }
    }

    private fun relativePath(rootDirName: String, subdir: String): String =
        buildString {
            append(Environment.DIRECTORY_DOCUMENTS)
            append('/')
            append(rootDirName)
            append('/')
            if (subdir.isNotBlank()) {
                append(subdir)
                append('/')
            }
        }

    private class TreeReader(
        private val context: Context,
        private val treeUri: Uri,
        private val rootDirName: String
    ) {
        private val resolver = context.contentResolver
        private val backupRoot: Uri? = backupRoot(create = false)

        fun readBytes(subdir: String, displayName: String): ByteArray? {
            val root = backupRoot ?: return null
            val parent = findParent(root, subdir) ?: return null
            val file = child(parent, displayName, directory = false) ?: return null
            return resolver.openInputStream(file)?.use { it.readBytes() }
        }

        fun writeBytes(
            subdir: String,
            displayName: String,
            mimeType: String,
            bytes: ByteArray
        ): Boolean = runCatching {
            val parent = ensureParent(subdir) ?: return false
            val file = child(parent, displayName, directory = false)
                ?: DocumentsContract.createDocument(resolver, parent, mimeType, displayName)
                ?: return false
            resolver.openOutputStream(file, "wt")?.use { it.write(bytes) } ?: return false
            true
        }.getOrDefault(false)

        fun delete(subdir: String, displayName: String): Boolean = runCatching {
            val root = backupRoot ?: return true
            val parent = findParent(root, subdir) ?: return true
            val file = child(parent, displayName, directory = false) ?: return true
            DocumentsContract.deleteDocument(resolver, file)
        }.getOrDefault(false)

        fun listFiles(subdir: String): List<PublicFileInfo> {
            val root = backupRoot ?: return emptyList()
            val parent = findParent(root, subdir) ?: return emptyList()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(
                parent,
                DocumentsContract.getDocumentId(parent)
            )
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            return resolver.query(children, projection, null, null, null)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val mimeType = cursor.getString(3).orEmpty()
                        val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                        if (!isDirectory) {
                            add(
                                PublicFileInfo(
                                    displayName = cursor.getString(0).orEmpty(),
                                    sizeBytes = if (cursor.isNull(1)) null else cursor.getLong(1),
                                    modifiedAtMillis = if (cursor.isNull(2)) null else cursor.getLong(2)
                                )
                            )
                        }
                    }
                }
            }.orEmpty()
        }

        private fun backupRoot(create: Boolean): Uri? {
            val selectedDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
            val root = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                selectedDocumentId
            )
            if (child(root, "manifest.json", directory = false) != null) return root
            if (
                DocumentTreeBackupRootPolicy.useSelectedTreeAsBackupRoot(
                    selectedDisplayName = displayName(root),
                    selectedDocumentId = selectedDocumentId,
                    rootDirName = rootDirName
                )
            ) {
                return root
            }
            val existing = child(root, rootDirName, directory = true)
            if (existing != null) return existing
            if (!create) return null
            return DocumentsContract.createDocument(
                resolver,
                root,
                DocumentsContract.Document.MIME_TYPE_DIR,
                rootDirName
            )
        }

        private fun findParent(root: Uri, subdir: String): Uri? =
            subdir.split('/')
                .filter { it.isNotBlank() }
                .fold(root) { current, segment ->
                    child(current, segment, directory = true) ?: return null
                }

        private fun ensureParent(subdir: String): Uri? {
            val root = backupRoot(create = true) ?: return null
            return subdir.split('/')
                .filter { it.isNotBlank() }
                .fold(root) { current, segment ->
                    child(current, segment, directory = true)
                        ?: DocumentsContract.createDocument(
                            resolver,
                            current,
                            DocumentsContract.Document.MIME_TYPE_DIR,
                            segment
                        )
                        ?: return null
                }
        }

        private fun displayName(document: Uri): String? {
            val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            return resolver.query(document, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                cursor.getString(0)
            }
        }

        private fun child(parent: Uri, name: String, directory: Boolean): Uri? {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(
                parent,
                DocumentsContract.getDocumentId(parent)
            )
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            return resolver.query(children, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(0)
                    val displayName = cursor.getString(1).orEmpty()
                    val mimeType = cursor.getString(2).orEmpty()
                    val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                    if (displayName == name && isDirectory == directory) {
                        return@use DocumentsContract.buildDocumentUriUsingTree(parent, documentId)
                    }
                }
                null
            }
        }
    }
}

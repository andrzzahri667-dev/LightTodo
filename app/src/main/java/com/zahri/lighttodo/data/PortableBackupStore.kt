package com.zahri.lighttodo.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.zahri.lighttodo.ui.note.NoteAttachmentMarkdown
import com.zahri.lighttodo.ui.note.NoteAttachmentStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.File

internal class PortableBackupStore(
    private val context: Context
) {
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    fun write(bundle: BackupBundle, settings: UserPrefs.Snapshot) {
        val exportedAtMillis = System.currentTimeMillis()
        val generation = "$exportedAtMillis-${java.util.UUID.randomUUID()}"
        if (!writeJson(
                "",
                "manifest.json",
                PortableManifest(
                    generation = generation,
                    exportedAtMillis = exportedAtMillis,
                    tagCount = bundle.tags.size,
                    todoCount = bundle.todos.size,
                    noteCount = bundle.notes.size,
                    noteFiles = bundle.notes.map { noteFileName(it) },
                    complete = false
                )
            )
        ) {
            return
        }
        var allWritesSucceeded = true
        fun recordWrite(succeeded: Boolean) {
            allWritesSucceeded = allWritesSucceeded && succeeded
        }

        val exportedAttachments = mutableSetOf<String>()
        val notes = bundle.notes.map { note ->
            val fileName = noteFileName(note)
            val markdown = note.content.toPortableMarkdown(exportedAttachments, ::recordWrite)
            recordWrite(
                writeFile(
                    subdir = NotesDir,
                    displayName = fileName,
                    mimeType = "text/markdown",
                    bytes = markdown.toByteArray(Charsets.UTF_8)
                )
            )
            PortableNoteEntry(
                id = note.id,
                title = note.title,
                tagId = note.tagId,
                createdAtMillis = note.createdAtMillis,
                updatedAtMillis = note.updatedAtMillis,
                file = fileName
            )
        }
        val noteIndex = PortableNoteIndex(
            generation = generation,
            exportedAtMillis = exportedAtMillis,
            notes = notes
        )

        recordWrite(
            writeJson(
                "",
                "tags.json",
                PortableTagsFile(
                    generation = generation,
                    exportedAtMillis = exportedAtMillis,
                    items = bundle.tags
                )
            )
        )
        recordWrite(
            writeJson(
                "",
                "todos.json",
                PortableTodosFile(
                    generation = generation,
                    exportedAtMillis = exportedAtMillis,
                    items = bundle.todos
                )
            )
        )
        recordWrite(writeJson("", "settings.json", settings))
        recordWrite(writeJson(NotesDir, "index.json", noteIndex))

        if (!allWritesSucceeded) return
        writeJson(
            "",
            "manifest.json",
            PortableManifest(
                generation = generation,
                exportedAtMillis = exportedAtMillis,
                tagCount = bundle.tags.size,
                todoCount = bundle.todos.size,
                noteCount = notes.size,
                noteFiles = notes.map { it.file },
                complete = true
            )
        )
    }

    fun read(): PortableRestore? = runCatching { readUsing(::readBytes) }.getOrNull()

    fun readFromTree(treeUri: Uri): PortableRestore? = runCatching {
        val reader = TreeReader(context, treeUri)
        readUsing(reader::readBytes)
    }.getOrNull()

    private fun readUsing(readFile: (String, String) -> ByteArray?): PortableRestore? {
        val manifest = readText(readFile, "", "manifest.json")
            ?.let { json.decodeFromString(PortableManifest.serializer(), it) }
            ?: return null
        val tags = readText(readFile, "", "tags.json")
            ?.let { json.decodeFromString(PortableTagsFile.serializer(), it) }
            ?: return null
        val todos = readText(readFile, "", "todos.json")
            ?.let { json.decodeFromString(PortableTodosFile.serializer(), it) }
            ?: return null
        val noteIndex = readText(readFile, NotesDir, "index.json")
            ?.let { json.decodeFromString(PortableNoteIndex.serializer(), it) }
            ?: return null
        if (!validateTags(manifest, tags)) return null
        if (!validateManifest(manifest, todos, noteIndex)) return null
        val settings = readText(readFile, "", "settings.json")
            ?.let { runCatching { json.decodeFromString(UserPrefs.Snapshot.serializer(), it) }.getOrNull() }

        val importedAttachments = mutableMapOf<String, String>()
        val notes = noteIndex.notes.map { entry ->
            val markdown = readText(readFile, NotesDir, entry.file) ?: return null
            BackupNote(
                id = entry.id,
                title = entry.title,
                content = markdown.fromPortableMarkdown(importedAttachments, readFile),
                tagId = entry.tagId,
                createdAtMillis = entry.createdAtMillis,
                updatedAtMillis = entry.updatedAtMillis
            )
        }

        return PortableRestore(
            bundle = BackupBundle(
                version = PortableBundleVersion,
                tags = tags.items,
                todos = todos.items,
                notes = notes
            ),
            settings = settings
        )
    }

    private inline fun <reified T> writeJson(subdir: String, displayName: String, value: T): Boolean =
        writeFile(
            subdir = subdir,
            displayName = displayName,
            mimeType = "application/json",
            bytes = json.encodeToString(value).toByteArray(Charsets.UTF_8)
        )

    private fun validateTags(manifest: PortableManifest, tags: PortableTagsFile): Boolean =
        manifest.generation == tags.generation &&
            manifest.exportedAtMillis == tags.exportedAtMillis &&
            manifest.tagCount == tags.items.size

    private fun validateManifest(
        manifest: PortableManifest,
        todos: PortableTodosFile,
        noteIndex: PortableNoteIndex
    ): Boolean {
        if (!manifest.complete) return false
        if (manifest.bundleVersion != PortableBundleVersion) return false
        if (manifest.generation != todos.generation) return false
        if (manifest.generation != noteIndex.generation) return false
        if (manifest.exportedAtMillis != todos.exportedAtMillis) return false
        if (manifest.exportedAtMillis != noteIndex.exportedAtMillis) return false
        if (manifest.todoCount != todos.items.size) return false
        if (manifest.noteCount != noteIndex.notes.size) return false
        val indexFiles = noteIndex.notes.map { it.file }.toSet()
        if (indexFiles.size != noteIndex.notes.size) return false
        return manifest.noteFiles.size == noteIndex.notes.size &&
            manifest.noteFiles.toSet() == indexFiles
    }

    private fun String.toPortableMarkdown(
        exportedAttachments: MutableSet<String>,
        recordWrite: (Boolean) -> Unit
    ): String =
        lineSequence().joinToString("\n") { line ->
            val attachment = NoteAttachmentMarkdown.parseLine(line) ?: return@joinToString line
            val source = NoteAttachmentStore.resolve(context, attachment.ref) ?: return@joinToString line
            val kindDir = attachment.kind.dirName()
            val targetSubdir = "$AttachmentsDir/$kindDir"
            val portableRef = "../$targetSubdir/${source.name}"
            val attachmentKey = "$targetSubdir/${source.name}"
            if (exportedAttachments.add(attachmentKey)) {
                recordWrite(
                    writeFile(
                        subdir = targetSubdir,
                        displayName = source.name,
                        mimeType = mimeTypeFor(source.name, attachment.kind),
                        bytes = source.readBytes()
                    )
                )
            }
            when (attachment.kind) {
                NoteAttachmentMarkdown.Kind.Image -> NoteAttachmentMarkdown.image(portableRef)
                NoteAttachmentMarkdown.Kind.Audio -> NoteAttachmentMarkdown.audio(
                    ref = portableRef,
                    durationLabel = attachment.label
                )
            }
        }

    private fun String.fromPortableMarkdown(
        importedAttachments: MutableMap<String, String>,
        readFile: (String, String) -> ByteArray?
    ): String =
        lineSequence().joinToString("\n") { line ->
            val attachment = NoteAttachmentMarkdown.parseLine(line) ?: return@joinToString line
            val portableRef = attachment.ref.normalizedPortableRef() ?: return@joinToString line
            val kind = when {
                portableRef.startsWith("$AttachmentsDir/image/") -> NoteAttachmentMarkdown.Kind.Image
                portableRef.startsWith("$AttachmentsDir/audio/") -> NoteAttachmentMarkdown.Kind.Audio
                else -> return@joinToString line
            }
            val fileName = portableRef.substringAfterLast('/').takeIf { it.isNotBlank() }
                ?: return@joinToString line
            val internalRef = importedAttachments.getOrPut(portableRef) {
                val subdir = portableRef.substringBeforeLast('/')
                val bytes = readFile(subdir, fileName) ?: return@joinToString line
                NoteAttachmentStore.importAttachment(
                    context = context,
                    kind = kind,
                    fileName = fileName,
                    input = ByteArrayInputStream(bytes)
                )
            }
            when (kind) {
                NoteAttachmentMarkdown.Kind.Image -> NoteAttachmentMarkdown.image(internalRef)
                NoteAttachmentMarkdown.Kind.Audio -> NoteAttachmentMarkdown.audio(
                    ref = internalRef,
                    durationLabel = attachment.label
                )
            }
        }

    private fun writeFile(
        subdir: String,
        displayName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean =
        when (BackupStoragePolicy.publicDocumentsMode()) {
            BackupStoragePolicy.PublicDocumentsMode.MediaStore ->
                writeMediaStoreFile(subdir, displayName, mimeType, bytes)
            BackupStoragePolicy.PublicDocumentsMode.LegacyDirectPath ->
                writeDirectFile(subdir, displayName, bytes)
        }

    private fun readText(
        readFile: (String, String) -> ByteArray?,
        subdir: String,
        displayName: String
    ): String? =
        readFile(subdir, displayName)?.toString(Charsets.UTF_8)

    private fun readBytes(subdir: String, displayName: String): ByteArray? =
        when (BackupStoragePolicy.publicDocumentsMode()) {
            BackupStoragePolicy.PublicDocumentsMode.MediaStore ->
                readMediaStoreFile(subdir, displayName)
            BackupStoragePolicy.PublicDocumentsMode.LegacyDirectPath ->
                readDirectFile(subdir, displayName)
        }

    private fun writeMediaStoreFile(
        subdir: String,
        displayName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val resolver = context.contentResolver
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            var inserted = false
            val uri = findMediaStoreFile(subdir, displayName) ?: resolver.insert(
                collection,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath(subdir))
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

    private fun readMediaStoreFile(subdir: String, displayName: String): ByteArray? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val uri = findMediaStoreFile(subdir, displayName) ?: return null
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
    }

    private fun findMediaStoreFile(subdir: String, displayName: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection =
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        val args = arrayOf(displayName, relativePath(subdir))
        val sort = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        return context.contentResolver.query(collection, projection, selection, args, sort)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            ContentUris.withAppendedId(collection, cursor.getLong(0))
        }
    }

    private fun writeDirectFile(subdir: String, displayName: String, bytes: ByteArray): Boolean =
        runCatching {
            val file = directFile(subdir, displayName)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            true
        }.getOrDefault(false)

    private fun readDirectFile(subdir: String, displayName: String): ByteArray? =
        runCatching {
            val file = directFile(subdir, displayName)
            if (file.exists() && file.canRead()) file.readBytes() else null
        }.getOrNull()

    private fun directFile(subdir: String, displayName: String): File {
        val root = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            RootDir
        )
        val dir = if (subdir.isBlank()) root else File(root, subdir)
        return File(dir, displayName)
    }

    private fun relativePath(subdir: String): String =
        buildString {
            append(Environment.DIRECTORY_DOCUMENTS)
            append('/')
            append(RootDir)
            append('/')
            if (subdir.isNotBlank()) {
                append(subdir)
                append('/')
            }
        }

    private fun noteFileName(note: BackupNote): String {
        val title = note.title?.takeIf { it.isNotBlank() } ?: "note"
        val safeTitle = title
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .trim()
            .take(48)
            .ifBlank { "note" }
        return "${note.id}-$safeTitle.md"
    }

    private fun NoteAttachmentMarkdown.Kind.dirName(): String = when (this) {
        NoteAttachmentMarkdown.Kind.Image -> "image"
        NoteAttachmentMarkdown.Kind.Audio -> "audio"
    }

    private fun String.normalizedPortableRef(): String? {
        val normalized = removePrefix("./").removePrefix("../")
        return normalized.takeIf { it.startsWith("$AttachmentsDir/") }
    }

    private fun mimeTypeFor(fileName: String, kind: NoteAttachmentMarkdown.Kind): String =
        when (kind) {
            NoteAttachmentMarkdown.Kind.Image -> when (fileName.substringAfterLast('.', "").lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                else -> "image/jpeg"
            }
            NoteAttachmentMarkdown.Kind.Audio -> "audio/mp4"
        }

    data class PortableRestore(
        val bundle: BackupBundle,
        val settings: UserPrefs.Snapshot?
    )

    private class TreeReader(
        private val context: Context,
        treeUri: Uri
    ) {
        private val resolver = context.contentResolver
        private val backupRoot: Uri? = backupRoot(treeUri)

        fun readBytes(subdir: String, displayName: String): ByteArray? {
            val root = backupRoot ?: return null
            val parent = subdir.split('/')
                .filter { it.isNotBlank() }
                .fold(root) { current, segment ->
                    child(current, segment, directory = true) ?: return null
                }
            val file = child(parent, displayName, directory = false) ?: return null
            return resolver.openInputStream(file)?.use { it.readBytes() }
        }

        private fun backupRoot(treeUri: Uri): Uri? {
            val root = android.provider.DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                android.provider.DocumentsContract.getTreeDocumentId(treeUri)
            )
            if (child(root, "manifest.json", directory = false) != null) return root
            return child(root, RootDir, directory = true)
        }

        private fun child(parent: Uri, name: String, directory: Boolean): Uri? {
            val children = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
                parent,
                android.provider.DocumentsContract.getDocumentId(parent)
            )
            val projection = arrayOf(
                android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            return resolver.query(children, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val displayName = cursor.getString(1)
                    val mimeType = cursor.getString(2)
                    val isDirectory = mimeType == android.provider.DocumentsContract.Document.MIME_TYPE_DIR
                    if (displayName == name && isDirectory == directory) {
                        return@use android.provider.DocumentsContract.buildDocumentUriUsingTree(
                            parent,
                            cursor.getString(0)
                        )
                    }
                }
                null
            }
        }
    }

    @Serializable
    private data class PortableManifest(
        val version: Int = 2,
        val bundleVersion: Int = PortableBundleVersion,
        val generation: String,
        val exportedAtMillis: Long,
        val tagCount: Int,
        val todoCount: Int,
        val noteCount: Int,
        val noteFiles: List<String>,
        val complete: Boolean = false
    )

    @Serializable
    private data class PortableTagsFile(
        val generation: String,
        val exportedAtMillis: Long,
        val items: List<BackupTag>
    )

    @Serializable
    private data class PortableTodosFile(
        val generation: String,
        val exportedAtMillis: Long,
        val items: List<BackupTodo>
    )

    @Serializable
    private data class PortableNoteIndex(
        val generation: String,
        val exportedAtMillis: Long,
        val notes: List<PortableNoteEntry>
    )

    @Serializable
    private data class PortableNoteEntry(
        val id: Long,
        val title: String?,
        val tagId: Long?,
        val createdAtMillis: Long,
        val updatedAtMillis: Long,
        val file: String
    )

    private companion object {
        const val PortableBundleVersion = 2
        const val RootDir = "LightTodo"
        const val NotesDir = "notes"
        const val AttachmentsDir = "attachments"
    }
}

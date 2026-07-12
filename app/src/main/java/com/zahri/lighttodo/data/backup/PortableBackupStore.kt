package com.zahri.lighttodo.data.backup

import android.net.Uri
import com.zahri.lighttodo.data.prefs.UserPrefs
import com.zahri.lighttodo.domain.backup.BackupBundle
import com.zahri.lighttodo.domain.backup.BackupNote
import com.zahri.lighttodo.domain.backup.BackupTag
import com.zahri.lighttodo.domain.backup.BackupTodo
import com.zahri.lighttodo.domain.note.NoteAttachmentMarkdown
import com.zahri.lighttodo.usecase.file.FileGateway
import com.zahri.lighttodo.usecase.file.PublicFileInfo
import com.zahri.lighttodo.usecase.note.NoteAttachmentGateway
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.File

internal class PortableBackupStore(
    private val fileGateway: FileGateway,
    private val noteAttachmentGateway: NoteAttachmentGateway
) {
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    fun write(bundle: BackupBundle, settings: UserPrefs.Snapshot) {
        val exportedAtMillis = System.currentTimeMillis()
        writeNomedia()
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
        if (writeJson(
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
        ) {
            cleanupCurrentBackupOrphans(
                currentNoteFiles = notes.map { it.file }.toSet(),
                currentAttachmentKeys = exportedAttachments.toSet()
            )
            cleanupLegacyBackupAttachments()
        }
    }

    fun read(): PortableRestore? = runCatching { readUsing(::readBytes) }.getOrNull()

    fun readFromTree(treeUri: Uri): PortableRestore? = runCatching {
        readUsing { subdir, displayName ->
            fileGateway.readDocumentTreeFile(treeUri, RootDir, subdir, displayName)
        }
    }.getOrNull()

    fun writeToTree(treeUri: Uri, bundle: BackupBundle, settings: UserPrefs.Snapshot) {
        writeToTree(treeFiles(treeUri), bundle, settings)
    }

    internal fun writeToTree(
        treeFiles: PortableTreeFiles,
        bundle: BackupBundle,
        settings: UserPrefs.Snapshot
    ) {
        val exportedAtMillis = System.currentTimeMillis()
        treeFiles.write("", ".nomedia", "application/octet-stream", ByteArray(0))
        val generation = "$exportedAtMillis-${java.util.UUID.randomUUID()}"
        if (!writeTreeJson(
                treeFiles,
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
            val markdown = note.content.toPortableMarkdownForTree(
                treeFiles = treeFiles,
                exportedAttachments = exportedAttachments,
                recordWrite = ::recordWrite
            )
            recordWrite(
                treeFiles.write(
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

        recordWrite(writeTreeJson(treeFiles, "", "tags.json", PortableTagsFile(generation, exportedAtMillis, bundle.tags)))
        recordWrite(writeTreeJson(treeFiles, "", "todos.json", PortableTodosFile(generation, exportedAtMillis, bundle.todos)))
        recordWrite(writeTreeJson(treeFiles, "", "settings.json", settings))
        recordWrite(writeTreeJson(treeFiles, NotesDir, "index.json", noteIndex))

        if (!allWritesSucceeded) return
        if (writeTreeJson(
                treeFiles,
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
        ) {
            cleanupTreeCurrentBackupOrphans(
                treeFiles = treeFiles,
                currentNoteFiles = notes.map { it.file }.toSet(),
                currentAttachmentKeys = exportedAttachments.toSet()
            )
        }
    }

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

    private inline fun <reified T> writeTreeJson(
        treeFiles: PortableTreeFiles,
        subdir: String,
        displayName: String,
        value: T
    ): Boolean =
        treeFiles.write(
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
            val source = noteAttachmentGateway.resolveAttachment(attachment.ref) ?: return@joinToString line
            val kindDir = attachment.kind.dirName()
            val targetSubdir = "$HiddenAttachmentsDir/$kindDir"
            val legacySubdir = "$AttachmentsDir/$kindDir"
            val sanitizedHiddenSubdir = "$SanitizedHiddenAttachmentsDir/$kindDir"
            val legacyRootSubdir = ""
            val portableRef = "../$targetSubdir/${source.name}"
            val attachmentKey = "$targetSubdir/${source.name}"
            if (exportedAttachments.add(attachmentKey)) {
                if (!attachmentUpToDate(
                        subdir = targetSubdir,
                        displayName = source.name,
                        sourceSize = source.length(),
                        sourceLastModified = source.lastModified()
                    )
                ) {
                    val wrote = writeFile(
                        subdir = targetSubdir,
                        displayName = source.name,
                        mimeType = mimeTypeFor(source.name, attachment.kind),
                        bytes = source.readBytes()
                    )
                    recordWrite(wrote)
                    if (wrote) {
                        deleteLegacyAttachment(legacySubdir, source.name)
                        deleteLegacyAttachment(sanitizedHiddenSubdir, source.name)
                        deleteLegacyAttachment(legacyRootSubdir, source.name)
                    }
                } else {
                    deleteLegacyAttachment(legacySubdir, source.name)
                    deleteLegacyAttachment(sanitizedHiddenSubdir, source.name)
                    deleteLegacyAttachment(legacyRootSubdir, source.name)
                }
            }
            when (attachment.kind) {
                NoteAttachmentMarkdown.Kind.Image -> NoteAttachmentMarkdown.image(portableRef)
                NoteAttachmentMarkdown.Kind.Audio -> NoteAttachmentMarkdown.audio(
                    ref = portableRef,
                    durationLabel = attachment.label
                )
            }
        }

    private fun String.toPortableMarkdownForTree(
        treeFiles: PortableTreeFiles,
        exportedAttachments: MutableSet<String>,
        recordWrite: (Boolean) -> Unit
    ): String =
        lineSequence().joinToString("\n") { line ->
            val attachment = NoteAttachmentMarkdown.parseLine(line) ?: return@joinToString line
            val source = noteAttachmentGateway.resolveAttachment(attachment.ref) ?: return@joinToString line
            val kindDir = attachment.kind.dirName()
            val targetSubdir = "$HiddenAttachmentsDir/$kindDir"
            val portableRef = "../$targetSubdir/${source.name}"
            val attachmentKey = "$targetSubdir/${source.name}"
            if (exportedAttachments.add(attachmentKey)) {
                recordWrite(
                    treeFiles.write(
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
            val importTarget = portableAttachmentImportTarget(portableRef, attachment.kind)
                ?: return@joinToString line
            val internalRef = importedAttachments.getOrPut(portableRef) {
                val bytes = readFile(importTarget.subdir, importTarget.fileName) ?: return@joinToString line
                noteAttachmentGateway.importAttachment(
                    kind = importTarget.kind,
                    fileName = importTarget.fileName,
                    input = ByteArrayInputStream(bytes)
                )
            }
            when (importTarget.kind) {
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
    ): Boolean {
        if (usesDirectBackupFile(subdir)) return writeDirectFile(subdir, displayName, bytes)
        return when (BackupStoragePolicy.publicDocumentsMode()) {
            BackupStoragePolicy.PublicDocumentsMode.MediaStore ->
                writePublicDocumentFile(subdir, displayName, mimeType, bytes)
            BackupStoragePolicy.PublicDocumentsMode.LegacyDirectPath ->
                writeDirectFile(subdir, displayName, bytes)
        }
    }

    private fun cleanupCurrentBackupOrphans(
        currentNoteFiles: Set<String>,
        currentAttachmentKeys: Set<String>
    ) {
        deleteOrphanNoteFiles(currentNoteFiles)
        deleteOrphanCurrentAttachmentFiles(currentAttachmentKeys)
    }

    private fun cleanupTreeCurrentBackupOrphans(
        treeFiles: PortableTreeFiles,
        currentNoteFiles: Set<String>,
        currentAttachmentKeys: Set<String>
    ) {
        treeFiles.list(NotesDir)
            .filter { file -> file.displayName.endsWith(".md") && file.displayName !in currentNoteFiles }
            .forEach { file ->
                treeFiles.delete(NotesDir, file.displayName)
            }
        listOf(
            "$HiddenAttachmentsDir/image",
            "$HiddenAttachmentsDir/audio"
        ).forEach { subdir ->
            treeFiles.list(subdir)
                .filter { file -> "$subdir/${file.displayName}" !in currentAttachmentKeys }
                .forEach { file ->
                    treeFiles.delete(subdir, file.displayName)
                }
        }
    }

    private fun deleteOrphanNoteFiles(currentNoteFiles: Set<String>) {
        runCatching {
            fileGateway.listPublicDocumentFiles(RootDir, NotesDir, requireMimeType = false)
                .filter { file -> file.displayName.endsWith(".md") && file.displayName !in currentNoteFiles }
                .forEach { file ->
                    fileGateway.deletePublicDocumentFile(RootDir, NotesDir, file.displayName)
                }
        }
        runCatching {
            directDir(NotesDir).listFiles()
                ?.filter { file -> file.isFile && file.name.endsWith(".md") && file.name !in currentNoteFiles }
                ?.forEach { file ->
                    deleteFile(subdir = NotesDir, displayName = file.name)
                    deleteDirectFile(NotesDir, file.name)
                }
        }
    }

    private fun deleteOrphanCurrentAttachmentFiles(currentAttachmentKeys: Set<String>) {
        listOf(
            "$HiddenAttachmentsDir/image",
            "$HiddenAttachmentsDir/audio"
        ).forEach { subdir ->
            runCatching {
                directDir(subdir).listFiles()
                    ?.filter { file -> file.isFile && file.toPortableAttachmentKey(subdir) !in currentAttachmentKeys }
                    ?.forEach { file ->
                        deleteDirectFile(subdir, file.name)
                    }
                deleteEmptyBackupDir(subdir)
            }
        }
    }

    private fun File.toPortableAttachmentKey(subdir: String): String =
        "$subdir/$name"

    private fun cleanupLegacyBackupAttachments() {
        val legacyAttachmentSubdirs = listOf(
            "$AttachmentsDir/image",
            "$AttachmentsDir/audio",
            "$SanitizedHiddenAttachmentsDir/image",
            "$SanitizedHiddenAttachmentsDir/audio"
        )
        legacyAttachmentSubdirs.forEach { subdir ->
            deleteLegacyPublicDocumentFilesInDir(subdir)
            deleteLegacyBackupDirContents(subdir)
        }
        deleteLegacyRootPublicDocumentAttachmentFiles()
        deleteLegacyRootAttachmentFiles()
    }

    private fun deleteLegacyPublicDocumentFilesInDir(subdir: String) {
        runCatching {
            fileGateway.listPublicDocumentFiles(RootDir, subdir, requireMimeType = true)
                .forEach { file ->
                    fileGateway.deletePublicDocumentFile(RootDir, subdir, file.displayName)
                }
        }
    }

    private fun deleteLegacyRootPublicDocumentAttachmentFiles() {
        runCatching {
            fileGateway.listPublicDocumentFiles(RootDir, "", requireMimeType = true)
                .filter { it.displayName.isLegacyRootAttachmentName() }
                .forEach { file ->
                    fileGateway.deletePublicDocumentFile(RootDir, "", file.displayName)
                }
        }
    }

    private fun deleteLegacyBackupDirContents(subdir: String) {
        runCatching {
            directDir(subdir).listFiles()
                ?.filter { it.isFile }
                ?.forEach { file -> deleteLegacyAttachment(subdir, file.name) }
            deleteEmptyBackupDir(subdir)
        }
    }

    private fun deleteLegacyRootAttachmentFiles() {
        runCatching {
            directDir("").listFiles()
                ?.filter { file -> file.isFile && file.name.isLegacyRootAttachmentName() }
                ?.forEach { file ->
                    deleteFile("", file.name)
                    deleteDirectFile("", file.name)
                }
        }
    }

    private fun String.isLegacyRootAttachmentName(): Boolean =
        when (substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg", "png", "webp", "gif",
            "m4a", "mp4", "aac", "mp3", "wav", "ogg" -> true
            else -> false
        }

    private fun deleteLegacyAttachment(subdir: String, displayName: String) {
        deleteFile(subdir, displayName)
        deleteDirectFile(subdir, displayName)
        deleteEmptyBackupDir(subdir)
    }

    private fun deleteEmptyBackupDir(subdir: String) {
        if (subdir.isBlank()) return
        runCatching {
            val dir = directDir(subdir)
            if (dir.exists() && dir.isDirectory && dir.list().isNullOrEmpty()) {
                dir.delete()
            }
            val parentSubdir = subdir.substringBeforeLast('/', "")
            if (parentSubdir.isNotBlank()) {
                val parent = directDir(parentSubdir)
                if (parent.exists() && parent.isDirectory && parent.list().isNullOrEmpty()) {
                    parent.delete()
                }
            }
        }
    }

    private fun deleteFile(subdir: String, displayName: String): Boolean {
        if (usesDirectBackupFile(subdir)) return deleteDirectFile(subdir, displayName)
        return when (BackupStoragePolicy.publicDocumentsMode()) {
            BackupStoragePolicy.PublicDocumentsMode.MediaStore ->
                deletePublicDocumentFile(subdir, displayName)
            BackupStoragePolicy.PublicDocumentsMode.LegacyDirectPath ->
                deleteDirectFile(subdir, displayName)
        }
    }

    private fun writeNomedia() {
        runCatching {
            val file = directFile("", ".nomedia")
            file.parentFile?.mkdirs()
            if (!file.exists()) file.writeBytes(ByteArray(0))
        }
    }

    private fun usesDirectBackupFile(subdir: String): Boolean =
        subdir == HiddenAttachmentsDir || subdir.startsWith("$HiddenAttachmentsDir/")

    private fun attachmentUpToDate(
        subdir: String,
        displayName: String,
        sourceSize: Long,
        sourceLastModified: Long
    ): Boolean {
        if (usesDirectBackupFile(subdir)) {
            return runCatching {
                val file = directFile(subdir, displayName)
                file.exists() &&
                    file.length() == sourceSize &&
                    file.lastModified() >= sourceLastModified
            }.getOrDefault(false)
        }
        return when (BackupStoragePolicy.publicDocumentsMode()) {
            BackupStoragePolicy.PublicDocumentsMode.MediaStore -> {
                val existing = fileGateway.findPublicDocumentFile(RootDir, subdir, displayName)
                    ?: return false
                existing.sizeBytes == sourceSize &&
                    (existing.modifiedAtMillis ?: 0L) >= sourceLastModified
            }
            BackupStoragePolicy.PublicDocumentsMode.LegacyDirectPath ->
                runCatching {
                    val file = directFile(subdir, displayName)
                    file.exists() &&
                        file.length() == sourceSize &&
                        file.lastModified() >= sourceLastModified
                }.getOrDefault(false)
        }
    }

    private fun readText(
        readFile: (String, String) -> ByteArray?,
        subdir: String,
        displayName: String
    ): String? =
        readFile(subdir, displayName)?.toString(Charsets.UTF_8)

    private fun readBytes(subdir: String, displayName: String): ByteArray? {
        if (usesDirectBackupFile(subdir)) return readDirectFile(subdir, displayName)
        return when (BackupStoragePolicy.publicDocumentsMode()) {
            BackupStoragePolicy.PublicDocumentsMode.MediaStore ->
                readPublicDocumentFile(subdir, displayName)
            BackupStoragePolicy.PublicDocumentsMode.LegacyDirectPath ->
                readDirectFile(subdir, displayName)
        }
    }

    private fun writePublicDocumentFile(
        subdir: String,
        displayName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean =
        fileGateway.writePublicDocumentFile(RootDir, subdir, displayName, mimeType, bytes)

    private fun readPublicDocumentFile(subdir: String, displayName: String): ByteArray? =
        fileGateway.readPublicDocumentFile(RootDir, subdir, displayName)

    private fun deletePublicDocumentFile(subdir: String, displayName: String): Boolean =
        fileGateway.deletePublicDocumentFile(RootDir, subdir, displayName)

    private fun writeDirectFile(subdir: String, displayName: String, bytes: ByteArray): Boolean =
        runCatching {
            val file = directFile(subdir, displayName)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            true
        }.getOrDefault(false)

    private fun deleteDirectFile(subdir: String, displayName: String): Boolean =
        runCatching {
            val file = directFile(subdir, displayName)
            !file.exists() || file.delete()
        }.getOrDefault(false)

    private fun readDirectFile(subdir: String, displayName: String): ByteArray? =
        runCatching {
            val file = directFile(subdir, displayName)
            if (file.exists() && file.canRead()) file.readBytes() else null
        }.getOrNull()

    private fun directFile(subdir: String, displayName: String): File =
        fileGateway.publicDocumentFile(RootDir, subdir, displayName)

    private fun directDir(subdir: String): File =
        fileGateway.publicDocumentDir(RootDir, subdir)

    private fun treeFiles(treeUri: Uri): PortableTreeFiles =
        object : PortableTreeFiles {
            override fun write(
                subdir: String,
                displayName: String,
                mimeType: String,
                bytes: ByteArray
            ): Boolean =
                fileGateway.writeDocumentTreeFile(treeUri, RootDir, subdir, displayName, mimeType, bytes)

            override fun list(subdir: String): List<PublicFileInfo> =
                fileGateway.listDocumentTreeFiles(treeUri, RootDir, subdir)

            override fun delete(subdir: String, displayName: String): Boolean =
                fileGateway.deleteDocumentTreeFile(treeUri, RootDir, subdir, displayName)
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
        return when {
            normalized.isBlank() -> null
            normalized.startsWith("$HiddenAttachmentsDir/") -> normalized
            normalized.startsWith("$AttachmentsDir/") -> normalized
            normalized.contains('/').not() -> normalized
            else -> null
        }
    }

    private fun portableAttachmentImportTarget(
        portableRef: String,
        markerKind: NoteAttachmentMarkdown.Kind
    ): PortableAttachmentTarget? {
        val kind = when {
            portableRef.startsWith("$HiddenAttachmentsDir/image/") -> NoteAttachmentMarkdown.Kind.Image
            portableRef.startsWith("$HiddenAttachmentsDir/audio/") -> NoteAttachmentMarkdown.Kind.Audio
            portableRef.startsWith("$AttachmentsDir/image/") -> NoteAttachmentMarkdown.Kind.Image
            portableRef.startsWith("$AttachmentsDir/audio/") -> NoteAttachmentMarkdown.Kind.Audio
            portableRef.contains('/').not() -> markerKind
            else -> return null
        }
        val fileName = portableRef.substringAfterLast('/').takeIf { it.isNotBlank() } ?: return null
        val subdir = if (portableRef.contains('/')) portableRef.substringBeforeLast('/') else ""
        return PortableAttachmentTarget(kind = kind, subdir = subdir, fileName = fileName)
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

    private data class PortableAttachmentTarget(
        val kind: NoteAttachmentMarkdown.Kind,
        val subdir: String,
        val fileName: String
    )

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
        const val HiddenAttachmentsDir = ".attachments"
        const val SanitizedHiddenAttachmentsDir = "_.attachments"
        const val AttachmentsDir = "attachments"
    }
}

internal interface PortableTreeFiles {
    fun write(subdir: String, displayName: String, mimeType: String, bytes: ByteArray): Boolean
    fun list(subdir: String): List<PublicFileInfo>
    fun delete(subdir: String, displayName: String): Boolean
}

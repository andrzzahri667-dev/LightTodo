package com.zahri.lighttodo.data.backup

import android.net.Uri
import com.zahri.lighttodo.data.prefs.UserPrefs
import com.zahri.lighttodo.domain.backup.BackupBundle
import com.zahri.lighttodo.domain.backup.BackupNote
import com.zahri.lighttodo.domain.note.NoteAttachmentMarkdown
import com.zahri.lighttodo.usecase.file.FileGateway
import com.zahri.lighttodo.usecase.file.PublicFileInfo
import com.zahri.lighttodo.usecase.note.NoteAttachmentGateway
import java.io.File
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableBackupStoreBehaviorTest {
    @Test
    fun failedTreeExportKeepsPreviousBackupReadableAndAllowsRetry() {
        val treeFiles = FakePortableTreeFiles()
        val store = PortableBackupStore(NoopFileGateway, FakeNoteAttachmentGateway())

        store.writeToTree(treeFiles, bundle(noteContent = "first"), UserPrefs.Snapshot())
        assertEquals(
            "first",
            store.readFromTree(treeFiles)?.bundle?.notes?.single()?.content
        )

        treeFiles.failNextWrite("todos.json")
        store.writeToTree(treeFiles, bundle(noteContent = "second"), UserPrefs.Snapshot())

        assertEquals(listOf("todos.json"), treeFiles.failedWrites)
        assertEquals(
            "first",
            store.readFromTree(treeFiles)?.bundle?.notes?.single()?.content
        )

        store.writeToTree(treeFiles, bundle(noteContent = "second"), UserPrefs.Snapshot())
        assertEquals(
            "second",
            store.readFromTree(treeFiles)?.bundle?.notes?.single()?.content
        )
    }

    @Test
    fun failedFinalManifestWriteKeepsPreviousBackupReadable() {
        val treeFiles = FakePortableTreeFiles()
        val store = PortableBackupStore(NoopFileGateway, FakeNoteAttachmentGateway())

        store.writeToTree(treeFiles, bundle(noteContent = "first"), UserPrefs.Snapshot())
        treeFiles.failNextCompleteManifest()
        store.writeToTree(treeFiles, bundle(noteContent = "second"), UserPrefs.Snapshot())

        assertEquals(listOf("manifest.json"), treeFiles.failedWrites)
        assertEquals(
            "first",
            store.readFromTree(treeFiles)?.bundle?.notes?.single()?.content
        )
    }

    @Test
    fun failedExportReusesCorruptSlotInsteadOfOverwritingLastReadableBackup() {
        val treeFiles = FakePortableTreeFiles()
        val store = PortableBackupStore(NoopFileGateway, FakeNoteAttachmentGateway())

        store.writeToTree(treeFiles, bundle(noteContent = "first"), UserPrefs.Snapshot())
        store.writeToTree(treeFiles, bundle(noteContent = "second"), UserPrefs.Snapshot())
        treeFiles.remove("snapshots/b", "tags.json")
        assertEquals(
            "first",
            store.readFromTree(treeFiles)?.bundle?.notes?.single()?.content
        )

        treeFiles.failNextWrite("todos.json")
        store.writeToTree(treeFiles, bundle(noteContent = "third"), UserPrefs.Snapshot())

        assertEquals(
            "first",
            store.readFromTree(treeFiles)?.bundle?.notes?.single()?.content
        )
    }

    @Test
    fun providerReadFailureInNewestSlotFallsBackToPreviousBackup() {
        val treeFiles = FakePortableTreeFiles()
        val store = PortableBackupStore(NoopFileGateway, FakeNoteAttachmentGateway())

        store.writeToTree(treeFiles, bundle(noteContent = "first"), UserPrefs.Snapshot())
        store.writeToTree(treeFiles, bundle(noteContent = "second"), UserPrefs.Snapshot())
        treeFiles.throwOnRead("snapshots/b", "tags.json")

        assertEquals(
            "first",
            store.readFromTree(treeFiles)?.bundle?.notes?.single()?.content
        )
    }

    @Test
    fun corruptSettingsInNewestSlotFallsBackToPreviousBackup() {
        val treeFiles = FakePortableTreeFiles()
        val store = PortableBackupStore(NoopFileGateway, FakeNoteAttachmentGateway())

        store.writeToTree(treeFiles, bundle(noteContent = "first"), UserPrefs.Snapshot())
        store.writeToTree(treeFiles, bundle(noteContent = "second"), UserPrefs.Snapshot())
        treeFiles.replaceText("snapshots/b", "settings.json", "not-json")

        assertEquals(
            "first",
            store.readFromTree(treeFiles)?.bundle?.notes?.single()?.content
        )
    }

    @Test
    fun interruptedSlotIsNotReportedAsCompleteAfterPartialPayloadOverwrite() {
        val treeFiles = FakePortableTreeFiles()
        val store = PortableBackupStore(NoopFileGateway, FakeNoteAttachmentGateway())

        store.writeToTree(treeFiles, bundle(noteContent = "first"), UserPrefs.Snapshot())
        store.writeToTree(treeFiles, bundle(noteContent = "second"), UserPrefs.Snapshot())
        treeFiles.failWrites("tags.json", "todos.json", "settings.json", "index.json")
        store.writeToTree(treeFiles, bundle(noteContent = "partial"), UserPrefs.Snapshot())

        treeFiles.remove("snapshots/b", "tags.json")

        assertNull(store.readFromTree(treeFiles))
    }

    @Test
    fun writeToTree_usesTwoFixedSlotsInsteadOfCreatingRenamedCopies() {
        val treeFiles = FakePortableTreeFiles()
        val store = PortableBackupStore(NoopFileGateway, FakeNoteAttachmentGateway())

        store.writeToTree(treeFiles, bundle(noteContent = "first"), UserPrefs.Snapshot())
        store.writeToTree(treeFiles, bundle(noteContent = "second"), UserPrefs.Snapshot())

        assertEquals("second", treeFiles.text("snapshots/b/$NotesDir", "7-Plan.md"))
        assertTrue(treeFiles.exists("snapshots/a", "manifest.json"))
        assertTrue(treeFiles.exists("snapshots/b", "manifest.json"))
        assertTrue(treeFiles.exists("snapshots/b/$NotesDir", "index.json"))
        assertFalse(treeFiles.displayNames().any { it.contains(" (1)") })
    }

    @Test
    fun writeToTree_cleansOnlyStaleBackupMarkdownNotes() {
        val treeFiles = FakePortableTreeFiles().apply {
            write("snapshots/a/$NotesDir", "old.md", "text/markdown", "old".toByteArray())
            write("snapshots/a/$NotesDir", ".megaignore", "application/octet-stream", "keep".toByteArray())
        }
        val store = PortableBackupStore(NoopFileGateway, FakeNoteAttachmentGateway())

        store.writeToTree(treeFiles, bundle(noteContent = "current"), UserPrefs.Snapshot())

        assertFalse(treeFiles.exists("snapshots/a/$NotesDir", "old.md"))
        assertTrue(treeFiles.exists("snapshots/a/$NotesDir", ".megaignore"))
        assertTrue(treeFiles.exists("snapshots/a/$NotesDir", "7-Plan.md"))
    }

    @Test
    fun cleanupFailureDoesNotHideNewlyCommittedBackup() {
        val treeFiles = FakePortableTreeFiles()
        val store = PortableBackupStore(NoopFileGateway, FakeNoteAttachmentGateway())

        store.writeToTree(treeFiles, bundle(noteContent = "first", title = "One"), UserPrefs.Snapshot())
        store.writeToTree(treeFiles, bundle(noteContent = "second", title = "Two"), UserPrefs.Snapshot())
        treeFiles.failNextDelete()
        store.writeToTree(treeFiles, bundle(noteContent = "third", title = "Three"), UserPrefs.Snapshot())

        assertEquals(listOf("7-One.md"), treeFiles.failedDeletes)
        assertTrue(treeFiles.exists("snapshots/a/$NotesDir", "7-One.md"))
        assertEquals(
            "third",
            store.readFromTree(treeFiles)?.bundle?.notes?.single()?.content
        )
    }

    @Test
    fun readsLegacyFlatV2BackupWhenSnapshotSlotsAreAbsent() {
        val treeFiles = FakePortableTreeFiles()
        val store = PortableBackupStore(NoopFileGateway, FakeNoteAttachmentGateway())

        store.writeToTree(treeFiles, bundle(noteContent = "legacy"), UserPrefs.Snapshot())
        treeFiles.flattenSlot("snapshots/a")
        treeFiles.replaceText(
            "",
            "manifest.json",
            treeFiles.text("", "manifest.json")
                .lineSequence()
                .filterNot { line -> line.contains("\"sequence\"") }
                .joinToString("\n")
        )
        treeFiles.remove("", "settings.json")

        assertEquals(
            "legacy",
            store.readFromTree(treeFiles)?.bundle?.notes?.single()?.content
        )
    }

    @Test
    fun snapshotRoundTripKeepsNoteAttachmentsInsideSelectedSlot() {
        val attachment = File.createTempFile("portable-backup-", ".jpg")
        try {
            val attachmentBytes = byteArrayOf(1, 3, 5, 7)
            attachment.writeBytes(attachmentBytes)
            val attachmentGateway = FakeNoteAttachmentGateway(attachment)
            val treeFiles = FakePortableTreeFiles()
            val store = PortableBackupStore(NoopFileGateway, attachmentGateway)
            val originalRef = "lighttodo://attachment/image/${attachment.name}"

            store.writeToTree(
                treeFiles,
                bundle(noteContent = NoteAttachmentMarkdown.image(originalRef)),
                UserPrefs.Snapshot()
            )

            val restored = store.readFromTree(treeFiles)
            assertTrue(
                treeFiles.exists(
                    "snapshots/a/.attachments/image",
                    attachment.name
                )
            )
            assertEquals(attachmentBytes.toList(), attachmentGateway.importedBytes?.toList())
            assertEquals(
                NoteAttachmentMarkdown.image(
                    "lighttodo://attachment/image/imported-${attachment.name}"
                ),
                restored?.bundle?.notes?.single()?.content
            )
        } finally {
            attachment.delete()
        }
    }

    @Test
    fun snapshotWithMissingReferencedAttachmentIsNotReadable() {
        val attachment = File.createTempFile("portable-backup-missing-", ".jpg")
        try {
            attachment.writeBytes(byteArrayOf(2, 4, 6))
            val treeFiles = FakePortableTreeFiles()
            val store = PortableBackupStore(
                NoopFileGateway,
                FakeNoteAttachmentGateway(attachment)
            )
            val originalRef = "lighttodo://attachment/image/${attachment.name}"
            store.writeToTree(
                treeFiles,
                bundle(noteContent = NoteAttachmentMarkdown.image(originalRef)),
                UserPrefs.Snapshot()
            )

            treeFiles.remove("snapshots/a/.attachments/image", attachment.name)

            assertNull(store.readFromTree(treeFiles))
        } finally {
            attachment.delete()
        }
    }

    @Test
    fun directExportUsesTheSameTwoSlotsAsSafExport() {
        val tempRoot = File.createTempFile("portable-backup-direct-", "")
        tempRoot.delete()
        tempRoot.mkdirs()
        try {
            val store = PortableBackupStore(
                TemporaryDirectFileGateway(tempRoot),
                FakeNoteAttachmentGateway()
            )

            store.write(bundle(noteContent = "first"), UserPrefs.Snapshot())
            store.write(bundle(noteContent = "second"), UserPrefs.Snapshot())

            assertTrue(File(tempRoot, "LightTodo/snapshots/a/manifest.json").isFile)
            assertTrue(File(tempRoot, "LightTodo/snapshots/b/manifest.json").isFile)
            assertEquals("second", store.read()?.bundle?.notes?.single()?.content)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    private fun bundle(noteContent: String, title: String = "Plan"): BackupBundle =
        BackupBundle(
            version = 2,
            tags = emptyList(),
            todos = emptyList(),
            notes = listOf(
                BackupNote(
                    id = 7L,
                    title = title,
                    content = noteContent,
                    tagId = null,
                    createdAtMillis = 100L,
                    updatedAtMillis = 200L
                )
            )
        )

    private companion object {
        const val NotesDir = "notes"
    }
}

private class FakePortableTreeFiles : PortableTreeFiles {
    private val files = linkedMapOf<String, ByteArray>()
    private var nextFailedDisplayName: String? = null
    private val failedDisplayNames = mutableSetOf<String>()
    private var failCompleteManifest = false
    private var failDelete = false
    private val throwingReads = mutableSetOf<String>()

    val failedWrites = mutableListOf<String>()
    val failedDeletes = mutableListOf<String>()

    fun failNextWrite(displayName: String) {
        nextFailedDisplayName = displayName
    }

    fun failWrites(vararg displayNames: String) {
        failedDisplayNames += displayNames
    }

    fun failNextCompleteManifest() {
        failCompleteManifest = true
    }

    fun failNextDelete() {
        failDelete = true
    }

    fun exists(subdir: String, displayName: String): Boolean =
        key(subdir, displayName) in files

    fun text(subdir: String, displayName: String): String =
        files.getValue(key(subdir, displayName)).toString(Charsets.UTF_8)

    fun displayNames(): List<String> =
        files.keys.map { it.substringAfterLast('/') }

    fun remove(subdir: String, displayName: String) {
        files.remove(key(subdir, displayName))
    }

    fun replaceText(subdir: String, displayName: String, text: String) {
        files[key(subdir, displayName)] = text.toByteArray()
    }

    fun throwOnRead(subdir: String, displayName: String) {
        throwingReads += key(subdir, displayName)
    }

    fun flattenSlot(prefix: String) {
        val slotPrefix = "$prefix/"
        val flattened = files
            .filterKeys { path -> path.startsWith(slotPrefix) }
            .mapKeys { (path, _) -> path.removePrefix(slotPrefix) }
        files.keys.removeAll { path -> path.startsWith("snapshots/") }
        files.putAll(flattened)
    }

    override fun read(subdir: String, displayName: String): ByteArray? =
        key(subdir, displayName).let { path ->
            if (path in throwingReads) error("Injected read failure: $path")
            files[path]?.copyOf()
        }

    override fun write(
        subdir: String,
        displayName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean {
        val isCompleteManifest = displayName == "manifest.json" &&
            bytes.toString(Charsets.UTF_8).contains("\"complete\": true")
        if (
            displayName == nextFailedDisplayName ||
            displayName in failedDisplayNames ||
            (failCompleteManifest && isCompleteManifest)
        ) {
            failedWrites += displayName
            if (displayName == nextFailedDisplayName) nextFailedDisplayName = null
            if (isCompleteManifest) failCompleteManifest = false
            return false
        }
        files[key(subdir, displayName)] = bytes
        return true
    }

    override fun delete(
        subdir: String,
        displayName: String
    ): Boolean {
        if (failDelete) {
            failDelete = false
            failedDeletes += displayName
            return false
        }
        files.remove(key(subdir, displayName))
        return true
    }

    override fun list(subdir: String): List<PublicFileInfo> =
        files.keys
            .filter { it.substringBeforeLast('/', "") == subdir }
            .map { PublicFileInfo(displayName = it.substringAfterLast('/')) }

    private fun key(subdir: String, displayName: String): String =
        if (subdir.isBlank()) displayName else "$subdir/$displayName"
}

private object NoopFileGateway : FileGateway {
    override fun readDocumentTreeFile(
        treeUri: Uri,
        rootDirName: String,
        subdir: String,
        displayName: String
    ): ByteArray? = null
    override fun writeDocumentTreeFile(
        treeUri: Uri,
        rootDirName: String,
        subdir: String,
        displayName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean = false
    override fun deleteDocumentTreeFile(
        treeUri: Uri,
        rootDirName: String,
        subdir: String,
        displayName: String
    ): Boolean = false
    override fun listDocumentTreeFiles(
        treeUri: Uri,
        rootDirName: String,
        subdir: String
    ): List<PublicFileInfo> = emptyList()
    override fun hasPersistedDocumentTreeWritePermission(treeUri: Uri): Boolean = false
    override fun providerUri(file: File): Uri = error("unused")
    override fun copyUriToFile(uri: Uri, target: File) = Unit
    override fun imageExtension(uri: Uri): String = "jpg"
    override fun publicDocumentDir(rootDirName: String, subdir: String): File = File("unused")
    override fun publicDocumentFile(rootDirName: String, subdir: String, displayName: String): File = File("unused")
    override fun writePublicDocumentFile(
        rootDirName: String,
        subdir: String,
        displayName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean = false
    override fun readPublicDocumentFile(rootDirName: String, subdir: String, displayName: String): ByteArray? = null
    override fun deletePublicDocumentFile(rootDirName: String, subdir: String, displayName: String): Boolean = false
    override fun findPublicDocumentFile(rootDirName: String, subdir: String, displayName: String): PublicFileInfo? = null
    override fun listPublicDocumentFiles(
        rootDirName: String,
        subdir: String,
        requireMimeType: Boolean
    ): List<PublicFileInfo> = emptyList()
    override fun publicDownloadFile(displayName: String): File = File("unused")
    override fun findPublicDownloadFile(displayName: String): PublicFileInfo? = null
    override fun readPublicDownloadFile(displayName: String): ByteArray? = null
}

private class TemporaryDirectFileGateway(
    private val root: File
) : FileGateway by NoopFileGateway {
    override fun publicDocumentDir(rootDirName: String, subdir: String): File =
        File(File(root, rootDirName), subdir)

    override fun publicDocumentFile(
        rootDirName: String,
        subdir: String,
        displayName: String
    ): File = File(publicDocumentDir(rootDirName, subdir), displayName)

    override fun writePublicDocumentFile(
        rootDirName: String,
        subdir: String,
        displayName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean = runCatching {
        publicDocumentFile(rootDirName, subdir, displayName).apply {
            parentFile?.mkdirs()
            writeBytes(bytes)
        }
        true
    }.getOrDefault(false)

    override fun readPublicDocumentFile(
        rootDirName: String,
        subdir: String,
        displayName: String
    ): ByteArray? = publicDocumentFile(rootDirName, subdir, displayName)
        .takeIf { it.isFile }
        ?.readBytes()

    override fun deletePublicDocumentFile(
        rootDirName: String,
        subdir: String,
        displayName: String
    ): Boolean {
        val file = publicDocumentFile(rootDirName, subdir, displayName)
        return !file.exists() || file.delete()
    }

    override fun listPublicDocumentFiles(
        rootDirName: String,
        subdir: String,
        requireMimeType: Boolean
    ): List<PublicFileInfo> = publicDocumentDir(rootDirName, subdir)
        .listFiles()
        ?.filter { it.isFile }
        ?.map { file ->
            PublicFileInfo(
                displayName = file.name,
                sizeBytes = file.length(),
                modifiedAtMillis = file.lastModified()
            )
        }
        .orEmpty()
}

private class FakeNoteAttachmentGateway(
    private val resolvedAttachment: File? = null
) : NoteAttachmentGateway {
    var importedBytes: ByteArray? = null

    override fun createImageFile(): File = File("unused-image")
    override fun createAudioFile(): File = File("unused-audio")
    override fun fileProviderUri(file: File): Uri = error("unused")
    override fun copyImageFromUri(uri: Uri): File = File("unused-copy")
    override fun imageRef(file: File): String = "lighttodo://attachment/image/${file.name}"
    override fun audioRef(file: File): String = "lighttodo://attachment/audio/${file.name}"
    override fun resolveAttachment(ref: String): File? = resolvedAttachment
    override fun importAttachment(
        kind: NoteAttachmentMarkdown.Kind,
        fileName: String,
        input: InputStream
    ): String {
        importedBytes = input.readBytes()
        return "lighttodo://attachment/${kind.name.lowercase()}/imported-$fileName"
    }
    override fun deleteRefs(refs: Iterable<String>) = Unit
    override fun deleteRemovedRefs(previousContent: String, currentContent: String) = Unit
    override fun deleteUnreferenced(referencedRefs: Set<String>) = Unit
}

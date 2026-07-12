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
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableBackupStoreBehaviorTest {
    @Test
    fun writeToTree_overwritesSameBackupFilesInsteadOfCreatingRenamedCopies() {
        val treeFiles = FakePortableTreeFiles()
        val store = PortableBackupStore(NoopFileGateway, FakeNoteAttachmentGateway())

        store.writeToTree(treeFiles, bundle(noteContent = "first"), UserPrefs.Snapshot())
        store.writeToTree(treeFiles, bundle(noteContent = "second"), UserPrefs.Snapshot())

        assertEquals("second", treeFiles.text(NotesDir, "7-Plan.md"))
        assertTrue(treeFiles.exists("", "manifest.json"))
        assertTrue(treeFiles.exists(NotesDir, "index.json"))
        assertFalse(treeFiles.displayNames().any { it.contains(" (1)") })
    }

    @Test
    fun writeToTree_cleansOnlyStaleBackupMarkdownNotes() {
        val treeFiles = FakePortableTreeFiles().apply {
            write(NotesDir, "old.md", "text/markdown", "old".toByteArray())
            write(NotesDir, ".megaignore", "application/octet-stream", "keep".toByteArray())
        }
        val store = PortableBackupStore(NoopFileGateway, FakeNoteAttachmentGateway())

        store.writeToTree(treeFiles, bundle(noteContent = "current"), UserPrefs.Snapshot())

        assertFalse(treeFiles.exists(NotesDir, "old.md"))
        assertTrue(treeFiles.exists(NotesDir, ".megaignore"))
        assertTrue(treeFiles.exists(NotesDir, "7-Plan.md"))
    }

    private fun bundle(noteContent: String): BackupBundle =
        BackupBundle(
            version = 2,
            tags = emptyList(),
            todos = emptyList(),
            notes = listOf(
                BackupNote(
                    id = 7L,
                    title = "Plan",
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

    fun exists(subdir: String, displayName: String): Boolean =
        key(subdir, displayName) in files

    fun text(subdir: String, displayName: String): String =
        files.getValue(key(subdir, displayName)).toString(Charsets.UTF_8)

    fun displayNames(): List<String> =
        files.keys.map { it.substringAfterLast('/') }

    override fun write(
        subdir: String,
        displayName: String,
        mimeType: String,
        bytes: ByteArray
    ): Boolean {
        files[key(subdir, displayName)] = bytes
        return true
    }

    override fun delete(
        subdir: String,
        displayName: String
    ): Boolean {
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

private class FakeNoteAttachmentGateway : NoteAttachmentGateway {
    override fun createImageFile(): File = File("unused-image")
    override fun createAudioFile(): File = File("unused-audio")
    override fun fileProviderUri(file: File): Uri = error("unused")
    override fun copyImageFromUri(uri: Uri): File = File("unused-copy")
    override fun imageRef(file: File): String = "lighttodo://attachment/image/${file.name}"
    override fun audioRef(file: File): String = "lighttodo://attachment/audio/${file.name}"
    override fun resolveAttachment(ref: String): File? = null
    override fun importAttachment(
        kind: NoteAttachmentMarkdown.Kind,
        fileName: String,
        input: InputStream
    ): String = "lighttodo://attachment/${kind.name.lowercase()}/$fileName"
    override fun deleteRefs(refs: Iterable<String>) = Unit
    override fun deleteRemovedRefs(previousContent: String, currentContent: String) = Unit
    override fun deleteUnreferenced(referencedRefs: Set<String>) = Unit
}

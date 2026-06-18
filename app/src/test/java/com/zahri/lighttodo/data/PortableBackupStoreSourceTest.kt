package com.zahri.lighttodo.data

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertTrue
import org.junit.Test

class PortableBackupStoreSourceTest {
    private val source by lazy {
        sourceFile("app/src/main/java/com/zahri/lighttodo/data/backup/PortableBackupStore.kt").readText()
    }

    @Test
    fun skipsRewritingAttachmentsWhenSizeAndMtimeMatched() {
        val markdownFunction = source
            .substringAfter("private fun String.toPortableMarkdown")
            .substringBefore("\n    private fun String.fromPortableMarkdown")

        assertTrue(source.contains("private fun attachmentUpToDate("))
        assertTrue(markdownFunction.contains("attachmentUpToDate("))
        assertTrue(markdownFunction.contains("source.length()"))
        assertTrue(markdownFunction.contains("source.lastModified()"))
    }

    @Test
    fun writesAttachmentsAtHiddenKindFoldersWithOriginalMediaNames() {
        val markdownFunction = source
            .substringAfter("private fun String.toPortableMarkdown")
            .substringBefore("\n    private fun String.fromPortableMarkdown")

        assertTrue(markdownFunction.contains("val targetSubdir = \"\$HiddenAttachmentsDir/\$kindDir\""))
        assertTrue(markdownFunction.contains("val legacySubdir = \"\$AttachmentsDir/\$kindDir\""))
        assertTrue(markdownFunction.contains("val sanitizedHiddenSubdir = \"\$SanitizedHiddenAttachmentsDir/\$kindDir\""))
        assertTrue(markdownFunction.contains("val legacyRootSubdir = \"\""))
        assertTrue(markdownFunction.contains("val portableRef = \"../\$targetSubdir/\${source.name}\""))
        assertTrue(markdownFunction.contains("mimeTypeFor(source.name, attachment.kind)"))
        assertTrue(markdownFunction.contains("subdir = targetSubdir"))
    }

    @Test
    fun deletesLegacyNestedAndRootAttachmentsAfterHiddenCopyExists() {
        val markdownFunction = source
            .substringAfter("private fun String.toPortableMarkdown")
            .substringBefore("\n    private fun String.fromPortableMarkdown")

        assertTrue(source.contains("private fun deleteLegacyAttachment("))
        assertTrue(source.contains("private fun deleteFile("))
        assertTrue(source.contains("private fun deleteMediaStoreFile("))
        assertTrue(markdownFunction.contains("deleteLegacyAttachment(legacySubdir, source.name)"))
        assertTrue(markdownFunction.contains("deleteLegacyAttachment(sanitizedHiddenSubdir, source.name)"))
        assertTrue(markdownFunction.contains("deleteLegacyAttachment(legacyRootSubdir, source.name)"))
    }

    @Test
    fun hiddenAttachmentsUseDirectFilePathToAvoidMediaStoreSanitization() {
        val writeFileFunction = source
            .substringAfter("private fun writeFile(")
            .substringBefore("\n    private fun deleteLegacyAttachment")
        val readBytesFunction = source
            .substringAfter("private fun readBytes(")
            .substringBefore("\n    private fun writeMediaStoreFile")
        val upToDateFunction = source
            .substringAfter("private fun attachmentUpToDate(")
            .substringBefore("\n    private fun readText")

        assertTrue(source.contains("private fun usesDirectBackupFile("))
        assertTrue(source.contains("subdir == HiddenAttachmentsDir"))
        assertTrue(source.contains("subdir.startsWith(\"\$HiddenAttachmentsDir/\")"))
        assertTrue(writeFileFunction.contains("if (usesDirectBackupFile(subdir)) return writeDirectFile(subdir, displayName, bytes)"))
        assertTrue(readBytesFunction.contains("if (usesDirectBackupFile(subdir)) return readDirectFile(subdir, displayName)"))
        assertTrue(upToDateFunction.contains("if (usesDirectBackupFile(subdir))"))
        assertTrue(upToDateFunction.contains("directFile(subdir, displayName)"))
    }

    @Test
    fun removesEmptyLegacyAttachmentDirectoriesAfterDeletingFiles() {
        val deleteLegacyHelper = source
            .substringAfter("private fun deleteLegacyAttachment(")
            .substringBefore("\n    private fun deleteFile")

        assertTrue(deleteLegacyHelper.contains("deleteEmptyBackupDir(subdir)"))
        assertTrue(source.contains("private fun deleteEmptyBackupDir("))
        assertTrue(source.contains("if (subdir.isBlank()) return"))
        assertTrue(source.contains("val parentSubdir = subdir.substringBeforeLast('/', \"\")"))
    }

    @Test
    fun sweepsStaleLegacyAttachmentDirectoriesAfterCompleteExport() {
        val writeFunction = source
            .substringAfter("fun write(bundle")
            .substringBefore("\n    fun read()")
        val cleanupHelper = source
            .substringAfter("private fun cleanupLegacyBackupAttachments()")
            .substringBefore("\n    private fun deleteLegacyAttachment")

        assertTrue(writeFunction.contains("if (writeJson("))
        assertTrue(writeFunction.contains("complete = true"))
        assertTrue(writeFunction.contains("cleanupLegacyBackupAttachments()"))
        assertTrue(source.contains("private fun cleanupLegacyBackupAttachments()"))
        assertTrue(cleanupHelper.contains("\"\$AttachmentsDir/image\""))
        assertTrue(cleanupHelper.contains("\"\$AttachmentsDir/audio\""))
        assertTrue(cleanupHelper.contains("\"\$SanitizedHiddenAttachmentsDir/image\""))
        assertTrue(cleanupHelper.contains("\"\$SanitizedHiddenAttachmentsDir/audio\""))
        assertTrue(cleanupHelper.contains("deleteLegacyMediaStoreFilesInDir"))
        assertTrue(cleanupHelper.contains("deleteLegacyBackupDirContents"))
        assertTrue(cleanupHelper.contains("deleteLegacyRootAttachmentFiles()"))
    }

    @Test
    fun sweepsBackupNotesThatAreMissingFromCurrentIndexAfterCompleteExport() {
        val writeFunction = source
            .substringAfter("fun write(bundle")
            .substringBefore("\n    fun read()")
        val cleanupHelper = source
            .substringAfter("private fun cleanupCurrentBackupOrphans(")
            .substringBefore("\n    private fun cleanupLegacyBackupAttachments")

        assertTrue(writeFunction.contains("cleanupCurrentBackupOrphans("))
        assertTrue(writeFunction.contains("noteFiles = notes.map { it.file }"))
        assertTrue(source.contains("private fun cleanupCurrentBackupOrphans("))
        assertTrue(cleanupHelper.contains("deleteOrphanNoteFiles(currentNoteFiles)"))
        assertTrue(source.contains("private fun deleteOrphanNoteFiles("))
        assertTrue(source.contains("displayName.endsWith(\".md\")"))
        assertTrue(source.contains("displayName !in currentNoteFiles"))
        assertTrue(source.contains("subdir = NotesDir"))
    }

    @Test
    fun sweepsBackupAttachmentsThatAreMissingFromCurrentMarkdownAfterCompleteExport() {
        val writeFunction = source
            .substringAfter("fun write(bundle")
            .substringBefore("\n    fun read()")
        val cleanupHelper = source
            .substringAfter("private fun cleanupCurrentBackupOrphans(")
            .substringBefore("\n    private fun cleanupLegacyBackupAttachments")
        val attachmentCleanup = source
            .substringAfter("private fun deleteOrphanCurrentAttachmentFiles(")
            .substringBefore("\n    private fun cleanupLegacyBackupAttachments")

        assertTrue(writeFunction.contains("exportedAttachments"))
        assertTrue(writeFunction.contains("cleanupCurrentBackupOrphans("))
        assertTrue(cleanupHelper.contains("deleteOrphanCurrentAttachmentFiles(currentAttachmentKeys)"))
        assertTrue(source.contains("private fun deleteOrphanCurrentAttachmentFiles("))
        assertTrue(attachmentCleanup.contains("\"\$HiddenAttachmentsDir/image\""))
        assertTrue(attachmentCleanup.contains("\"\$HiddenAttachmentsDir/audio\""))
        assertTrue(attachmentCleanup.contains("file.toPortableAttachmentKey(subdir)"))
        assertTrue(attachmentCleanup.contains("!in currentAttachmentKeys"))
        assertTrue(source.contains("private fun File.toPortableAttachmentKey("))
    }

    @Test
    fun sweepsRootLegacyMediaCopiesByExtension() {
        val rootCleanupHelper = source
            .substringAfter("private fun deleteLegacyRootAttachmentFiles()")
            .substringBefore("\n    private fun String.isLegacyRootAttachmentName")

        assertTrue(rootCleanupHelper.contains("directDir(\"\")"))
        assertTrue(rootCleanupHelper.contains("isLegacyRootAttachmentName()"))
        assertTrue(rootCleanupHelper.contains("deleteFile(\"\", file.name)"))
        assertTrue(source.contains("\"jpg\", \"jpeg\", \"png\", \"webp\", \"gif\""))
        assertTrue(source.contains("\"m4a\", \"mp4\", \"aac\", \"mp3\", \"wav\", \"ogg\""))
    }

    @Test
    fun sweepsLegacyMediaStoreRowsByRelativePath() {
        val mediaStoreDirCleanup = source
            .substringAfter("private fun deleteLegacyMediaStoreFilesInDir(")
            .substringBefore("\n    private fun deleteLegacyBackupDirContents")
        val mediaStoreRootCleanup = source
            .substringAfter("private fun deleteLegacyRootMediaStoreAttachmentFiles()")
            .substringBefore("\n    private fun deleteLegacyRootAttachmentFiles")

        assertTrue(mediaStoreDirCleanup.contains("MediaStore.MediaColumns.RELATIVE_PATH"))
        assertTrue(mediaStoreDirCleanup.contains("relativePath(subdir)"))
        assertTrue(mediaStoreDirCleanup.contains("MediaStore.MediaColumns.MIME_TYPE"))
        assertTrue(mediaStoreDirCleanup.contains("context.contentResolver.delete(uri, null, null)"))
        assertTrue(mediaStoreRootCleanup.contains("relativePath(\"\")"))
        assertTrue(mediaStoreRootCleanup.contains("isLegacyRootAttachmentName()"))
    }

    @Test
    fun restoreAcceptsHiddenRootAndLegacyPortableAttachments() {
        val importFunction = source
            .substringAfter("private fun String.fromPortableMarkdown")
            .substringBefore("\n    private fun writeFile(")

        assertTrue(importFunction.contains("val importTarget = portableAttachmentImportTarget("))
        assertTrue(source.contains("private fun portableAttachmentImportTarget("))
        assertTrue(source.contains("portableRef.startsWith(\"\$HiddenAttachmentsDir/image/\")"))
        assertTrue(source.contains("portableRef.startsWith(\"\$HiddenAttachmentsDir/audio/\")"))
        assertTrue(source.contains("portableRef.startsWith(\"\$AttachmentsDir/image/\")"))
        assertTrue(source.contains("portableRef.startsWith(\"\$AttachmentsDir/audio/\")"))
        assertTrue(source.contains("portableRef.contains('/').not()"))
    }

    @Test
    fun writesNomediaAtBackupRootOnEveryExport() {
        val writeFunction = source
            .substringAfter("fun write(bundle")
            .substringBefore("\n    fun read()")

        assertTrue(writeFunction.contains("writeNomedia()"))
        assertTrue(source.contains("\".nomedia\""))
    }

    @Test
    fun writesNomediaWithoutMediaStoreDisplayNameSanitization() {
        val helper = source
            .substringAfter("private fun writeNomedia()")
            .substringBefore("\n    private fun attachmentUpToDate(")

        assertTrue(helper.contains("directFile(\"\", \".nomedia\")"))
        assertTrue(helper.contains("writeFile(").not())
    }

    @Test
    fun comparesExistingSizeAndModifiedDateFromMediaStore() {
        val helper = source.substringAfter("private fun attachmentUpToDate(")

        assertTrue(helper.contains("MediaStore.MediaColumns.SIZE"))
        assertTrue(helper.contains("MediaStore.MediaColumns.DATE_MODIFIED"))
        assertTrue(helper.contains("existingSize == sourceSize"))
        assertTrue(helper.contains("existingModifiedSec * 1000 >= sourceLastModified"))
    }

    @Test
    fun comparesExistingSizeAndLastModifiedForLegacyDirectPath() {
        val helper = source.substringAfter("private fun attachmentUpToDate(")

        assertTrue(helper.contains("BackupStoragePolicy.PublicDocumentsMode.LegacyDirectPath"))
        assertTrue(helper.contains("file.length() == sourceSize"))
        assertTrue(helper.contains("file.lastModified() >= sourceLastModified"))
    }
}

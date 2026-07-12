package com.zahri.lighttodo.data

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableBackupStoreSourceTest {
    private val source by lazy {
        sourceFile("app/src/main/java/com/zahri/lighttodo/data/backup/PortableBackupStore.kt").readText()
    }

    @Test
    fun portableTreeAdapterCanReadBackFilesForRecoverySelection() {
        assertTrue(source.contains("internal fun readFromTree(treeFiles: PortableTreeFiles)"))
        assertTrue(source.contains("fun read(subdir: String, displayName: String): ByteArray?"))
        assertTrue(source.contains("override fun read("))
        assertTrue(source.contains("fileGateway.readDocumentTreeFile("))
    }

    @Test
    fun directAndSafExportsUseTheSameTwoSlotProtocol() {
        val directWrite = source
            .substringAfter("fun write(bundle")
            .substringBefore("\n    fun read()")
        val directRead = source
            .substringAfter("fun read()")
            .substringBefore("\n    fun readFromTree")

        assertTrue(directWrite.contains("writeToTree(directFiles(), bundle, settings)"))
        assertTrue(directRead.contains("readFromTree(directFiles())"))
        assertTrue(source.contains("val SnapshotSlots = listOf(\"snapshots/a\", \"snapshots/b\")"))
    }

    @Test
    fun slotManifestInvalidatesTargetBeforePayloadAndCommitsLast() {
        val writeFunction = source
            .substringAfter("internal fun writeToTree(")
            .substringBefore("\n    private fun readableTreeSlots")
        val pendingManifest = writeFunction.indexOf("complete = false")
        val tagsWrite = writeFunction.indexOf("\"tags.json\"")
        val notesIndexWrite = writeFunction.indexOf("\"index.json\"")
        val completeManifest = writeFunction.indexOf("complete = true")

        assertTrue(pendingManifest >= 0 && pendingManifest < tagsWrite)
        assertTrue(completeManifest > tagsWrite)
        assertTrue(completeManifest > notesIndexWrite)
        assertTrue(writeFunction.contains("sequence = sequence"))
    }

    @Test
    fun slotSelectionProtectsOnlyPayloadsThatCanActuallyBeRead() {
        val selection = source
            .substringAfter("private fun readableTreeSlots")
            .substringBefore("private fun readManifest")

        assertTrue(selection.contains("hasReadablePayload(files::read, manifest)"))
        assertTrue(source.contains("validateTags(manifest, tags)"))
        assertTrue(source.contains("validateManifest(manifest, todos, noteIndex)"))
        assertTrue(source.contains("noteIndex.notes.all"))
    }

    @Test
    fun attachmentsStayInsideTheirSnapshotAndLegacyRefsRemainReadable() {
        val export = source
            .substringAfter("private fun String.toPortableMarkdown(")
            .substringBefore("private fun String.fromPortableMarkdown")

        assertTrue(export.contains("val targetSubdir = \"\$HiddenAttachmentsDir/\$kindDir\""))
        assertTrue(export.contains("treeFiles.write("))
        assertTrue(export.contains("val portableRef = \"../\$targetSubdir/\${source.name}\""))
        assertTrue(source.contains("portableRef.startsWith(\"\$HiddenAttachmentsDir/image/\")"))
        assertTrue(source.contains("portableRef.startsWith(\"\$AttachmentsDir/image/\")"))
        assertTrue(source.contains("noteAttachmentGateway.importAttachment("))
    }

    @Test
    fun directAdapterKeepsNomediaAndHiddenSlotAttachmentsOffMediaStore() {
        val directAdapter = source
            .substringAfter("private fun directFiles()")
            .substringBefore("private fun listBackupFiles")
        val directPolicy = source
            .substringAfter("private fun usesDirectBackupFile")
            .substringBefore("private fun readText")

        assertTrue(directAdapter.contains("displayName == \".nomedia\""))
        assertTrue(directAdapter.contains("writeNomedia()"))
        assertTrue(directPolicy.contains("subdir.contains(\"/\$HiddenAttachmentsDir/\")"))
    }
}

package com.zahri.lighttodo.integration.file

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentTreeBackupRootPolicyTest {
    @Test
    fun usesSelectedTreeOnlyWhenItIsLightTodoDirectory() {
        assertTrue(
            DocumentTreeBackupRootPolicy.useSelectedTreeAsBackupRoot(
                selectedDisplayName = "LightTodo",
                selectedDocumentId = "primary:Documents/LightTodo",
                rootDirName = "LightTodo"
            )
        )
        assertTrue(
            DocumentTreeBackupRootPolicy.useSelectedTreeAsBackupRoot(
                selectedDisplayName = null,
                selectedDocumentId = "primary:Documents/LightTodo",
                rootDirName = "LightTodo"
            )
        )
        assertFalse(
            DocumentTreeBackupRootPolicy.useSelectedTreeAsBackupRoot(
                selectedDisplayName = "Documents",
                selectedDocumentId = "primary:Documents",
                rootDirName = "LightTodo"
            )
        )
        assertFalse(
            DocumentTreeBackupRootPolicy.useSelectedTreeAsBackupRoot(
                selectedDisplayName = null,
                selectedDocumentId = "primary:Documents",
                rootDirName = "LightTodo"
            )
        )
        assertTrue(
            DocumentTreeBackupRootPolicy.useSelectedTreeAsBackupRoot(
                selectedDisplayName = "LightTodo",
                selectedDocumentId = "primary:Download/LightTodo",
                rootDirName = "LightTodo"
            )
        )
        assertTrue(
            DocumentTreeBackupRootPolicy.useSelectedTreeAsBackupRoot(
                selectedDisplayName = "LightTodo",
                selectedDocumentId = "opaque-cloud-document-id",
                rootDirName = "LightTodo"
            )
        )
    }

    @Test
    fun existingManifestMakesAnySelectedTreeTheBackupRoot() {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/integration/file/AndroidFileGateway.kt"
        ).readText()
        val rootResolver = source
            .substringAfter("private fun backupRoot(create: Boolean)")
            .substringBefore("private fun findParent")
        val manifestCheck = rootResolver.indexOf("child(root, \"manifest.json\", directory = false)")
        val pathPolicy = rootResolver.indexOf("DocumentTreeBackupRootPolicy.useSelectedTreeAsBackupRoot")

        assertTrue(manifestCheck >= 0 && manifestCheck < pathPolicy)
    }

    @Test
    fun existingSnapshotSlotMakesAnySelectedTreeTheBackupRoot() {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/integration/file/AndroidFileGateway.kt"
        ).readText()
        val rootResolver = source
            .substringAfter("private fun backupRoot(create: Boolean)")
            .substringBefore("private fun findParent")
        val snapshotCheck = rootResolver.indexOf("hasSnapshotBackup(root)")
        val pathPolicy = rootResolver.indexOf("DocumentTreeBackupRootPolicy.useSelectedTreeAsBackupRoot")

        assertTrue(snapshotCheck >= 0 && snapshotCheck < pathPolicy)
        assertTrue(source.contains("child(root, \"snapshots\", directory = true)"))
        assertTrue(source.contains("child(slot, \"manifest.json\", directory = false)"))
    }
}

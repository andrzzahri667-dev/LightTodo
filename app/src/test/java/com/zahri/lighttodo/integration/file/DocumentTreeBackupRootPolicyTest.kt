package com.zahri.lighttodo.integration.file

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
        assertFalse(
            DocumentTreeBackupRootPolicy.useSelectedTreeAsBackupRoot(
                selectedDisplayName = "LightTodo",
                selectedDocumentId = "primary:Download/LightTodo",
                rootDirName = "LightTodo"
            )
        )
    }
}

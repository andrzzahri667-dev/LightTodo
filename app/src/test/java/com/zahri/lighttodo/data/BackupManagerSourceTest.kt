package com.zahri.lighttodo.data

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerSourceTest {
    @Test
    fun restoreCollectsOldTodoIdsInsideTransactionBeforeReplacingData() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/backup/BackupManager.kt").readText()

        assertTrue(source.contains("val oldTodoIds = db.withTransaction {"))
        assertTrue(source.indexOf("val oldTodoIds = db.todoDao().listAll().map { it.id }") < source.indexOf("db.todoDao().deleteAll()"))
        assertTrue(source.indexOf("db.noteDao().deleteAll()") < source.indexOf("oldTodoIds.forEach(cancelTodoReminder)"))
        assertTrue(source.contains("rescheduleTodoReminders()"))
    }

    @Test
    fun restoreIfEmptyLogsDecodeOrRestoreFailures() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/backup/BackupManager.kt").readText()

        assertTrue(source.contains("Log.w("))
        assertTrue(source.contains("Auto restore failed"))
    }

    @Test
    fun portableBackupUsesUserGrantedTreeForAutomaticExternalWrites() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/backup/BackupManager.kt").readText()
        val portableSource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/backup/PortableBackupStore.kt").readText()
        val fileGatewaySource = sourceFile("app/src/main/java/com/zahri/lighttodo/integration/file/AndroidFileGateway.kt").readText()
        val settingsSource = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/settings/SettingsScreen.kt").readText()
        val settingsViewModelSource = sourceFile("app/src/main/java/com/zahri/lighttodo/feature/settings/SettingsViewModel.kt").readText()
        val prefsSource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/prefs/UserPrefs.kt").readText()

        assertTrue(source.contains("writeToAppExternal(bytes)"))
        assertTrue(source.contains("prefs.portableBackupTreeUri()"))
        assertTrue(source.contains("PortableBackupRoutingPolicy.destination("))
        assertTrue(source.contains("fileGateway::hasPersistedDocumentTreeWritePermission"))
        assertTrue(source.contains("prefs.clearPortableBackupTreeUri()"))
        assertTrue(source.contains("PortableBackupRoutingPolicy.Destination.DocumentTree ->"))
        assertTrue(source.contains("portableBackupStore.writeToTree(requireNotNull(parsedTreeUri), bundle, settings)"))
        assertTrue(source.contains("PortableBackupRoutingPolicy.Destination.LegacyPublicDocuments ->"))
        assertTrue(source.contains("PortableBackupRoutingPolicy.Destination.SkipPublicDocuments -> Unit"))
        assertTrue(source.contains("portableBackupStore.write(bundle, settings)"))
        assertTrue(source.contains("val portableBackup = portableBackupStore.read()"))
        assertTrue(source.contains("restorePortableBackup(portableBackup)"))
        assertTrue(source.contains("fileGateway.hasPersistedDocumentTreeWritePermission(uri)"))
        assertTrue(source.contains("prefs.setPortableBackupTreeUri(uri.toString())"))
        val treeRestore = source
            .substringAfter("suspend fun restorePortableFromTree(uri: Uri)")
            .substringBefore("private suspend fun writeBackup")
        val treeRead = treeRestore.indexOf("portableBackupStore.readFromTree(uri)")
        val bundleRestore = treeRestore.indexOf("restorePortableBackup(portableBackup)")
        val treePersist = treeRestore.indexOf("prefs.setPortableBackupTreeUri(uri.toString())")
        assertTrue(treeRead >= 0 && treeRead < bundleRestore && bundleRestore < treePersist)
        assertTrue(portableSource.contains("const val RootDir = \"LightTodo\""))
        assertTrue(portableSource.contains("const val NotesDir = \"notes\""))
        assertTrue(portableSource.contains("const val AttachmentsDir = \"attachments\""))
        assertTrue(portableSource.contains("PortableTodosFile("))
        assertTrue(portableSource.contains("items = bundle.todos"))
        assertTrue(portableSource.contains("writeJson(\"\", \"settings.json\", settings)"))
        assertTrue(portableSource.contains("fun writeToTree(treeUri: Uri, bundle: BackupBundle, settings: UserPrefs.Snapshot)"))
        assertTrue(portableSource.contains("\"text/markdown\""))
        assertTrue(portableSource.contains("noteAttachmentGateway.importAttachment("))
        assertTrue(portableSource.contains("fun readFromTree(treeUri: Uri): PortableRestore?"))
        assertTrue(portableSource.contains("fileGateway.readDocumentTreeFile("))
        assertTrue(portableSource.contains("fileGateway.writeDocumentTreeFile("))
        assertTrue(fileGatewaySource.contains("DocumentsContract"))
        assertTrue(fileGatewaySource.contains("fun writeDocumentTreeFile("))
        assertTrue(fileGatewaySource.contains("hasPersistedDocumentTreeWritePermission"))
        assertTrue(fileGatewaySource.contains("MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)"))
        assertTrue(fileGatewaySource.contains("Environment.DIRECTORY_DOCUMENTS"))
        assertTrue(settingsSource.contains("ActivityResultContracts.OpenDocumentTree()"))
        assertTrue(settingsSource.contains("Intent.FLAG_GRANT_WRITE_URI_PERMISSION"))
        assertTrue(settingsSource.contains("settings_import_portable"))
        assertTrue(settingsViewModelSource.contains("fun importPortableFrom("))
        assertTrue(prefsSource.contains("fun portableBackupTreeUri()"))
        assertTrue(prefsSource.contains("fun setPortableBackupTreeUri("))
        assertTrue(prefsSource.contains("fun clearPortableBackupTreeUri()"))
        assertTrue(source.contains("private fun readFromPublicDownloads()"))
        assertTrue(source.contains("readFromPublicDownloads()?.let { text ->"))
        assertTrue(source.contains("writeToPublicDownloads(bytes)").not())
    }

    @Test
    fun restoreIfEmptyPrioritizesAppExternalJsonBeforeBestEffortPortableDocuments() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/backup/BackupManager.kt").readText()
        val appExternalRead = source.indexOf("readFromAppExternal()")
        val portableRead = source.indexOf("portableBackupStore.read()")
        val downloadsRead = source.indexOf("readFromPublicDownloads()")

        assertTrue(appExternalRead >= 0)
        assertTrue(portableRead >= 0)
        assertTrue(downloadsRead >= 0)
        assertTrue(appExternalRead < portableRead)
        assertTrue(portableRead < downloadsRead)
        assertTrue(source.contains("Best-effort Documents/LightTodo restore"))
    }

    @Test
    fun autoBackupRefreshesPortableSettingsWhenPrefsChange() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/backup/BackupManager.kt").readText()

        assertTrue(source.contains("prefs.flow"))
        assertTrue(source.contains("writeBackup(bundle, snapshot.settings)"))
        assertTrue(source.contains("PortableBackupRoutingPolicy.destination("))
        assertTrue(source.contains("PortableBackupRoutingPolicy.Destination.SkipPublicDocuments -> Unit"))
        assertFalse(source.contains("portableBackupStore.write(bundle, prefs.snapshot())"))
    }

    @Test
    fun portableRestoreAppliesSettingsOnlyAfterBundleRestoreSucceeds() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/backup/BackupManager.kt").readText()
        val restoreFunction = source.substringAfter("private suspend fun restorePortableBackup")
        val restoreBundle = restoreFunction.indexOf("restoreFromBundle(portableBackup.bundle)")
        val restoreSettings = restoreFunction.indexOf("prefs.restore(it)")

        assertTrue(restoreBundle >= 0)
        assertTrue(restoreSettings >= 0)
        assertTrue(restoreBundle < restoreSettings)
    }

    @Test
    fun portableReadFailuresFallBackToLegacyJsonInsteadOfFailingCoroutine() {
        val portableSource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/backup/PortableBackupStore.kt").readText()
        val managerSource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/backup/BackupManager.kt").readText()

        assertTrue(portableSource.contains("runCatching { readUsing("))
        assertTrue(portableSource.contains("}.getOrNull()"))
        assertTrue(managerSource.contains("readFromPublicDownloads()?.let"))
        assertTrue(managerSource.indexOf("portableBackupStore.read()") < managerSource.indexOf("readFromPublicDownloads()"))
    }

    @Test
    fun portableBackupUsesManifestIntegritySignalAndWritesManifestLast() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/backup/PortableBackupStore.kt").readText()
        val writeFunction = source.substringAfter("fun write(bundle").substringBefore("\n    fun read()")
        val pendingManifestFileWrite = writeFunction.indexOf("\"manifest.json\"")
        val completeManifestFileWrite = writeFunction.lastIndexOf("\"manifest.json\"")
        val pendingManifestWrite = writeFunction.indexOf("complete = false")
        val completeManifestWrite = writeFunction.indexOf("complete = true")
        val tagsWrite = writeFunction.indexOf("\"tags.json\"")
        val todosWrite = writeFunction.indexOf("\"todos.json\"")
        val settingsWrite = writeFunction.indexOf("\"settings.json\"")
        val notesIndexWrite = writeFunction.indexOf("\"index.json\"")

        assertTrue(source.contains("val generation ="))
        assertTrue(source.contains("generation = generation"))
        assertTrue(source.contains("if (!manifest.complete) return false"))
        assertTrue(pendingManifestWrite >= 0)
        assertTrue(completeManifestWrite >= 0)
        assertTrue(pendingManifestWrite < tagsWrite)
        assertTrue(pendingManifestFileWrite < tagsWrite)
        assertTrue(completeManifestFileWrite > tagsWrite)
        assertTrue(completeManifestFileWrite > todosWrite)
        assertTrue(completeManifestFileWrite > settingsWrite)
        assertTrue(completeManifestFileWrite > notesIndexWrite)
        assertTrue(completeManifestWrite > notesIndexWrite)
        assertTrue(source.contains("validateManifest(manifest, todos, noteIndex)"))
    }

    @Test
    fun portableAttachmentImportCreatesUniqueFilesInsteadOfOverwritingByOriginalName() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/data/note/NoteAttachmentStore.kt").readText()
        val importFunction = source.substringAfter("fun importAttachment(").substringBefore("\n    fun resolve")

        assertTrue(importFunction.contains("uniqueImportedFile("))
        assertFalse(importFunction.contains("File(attachmentDir(context, type), safeName)"))
    }
}

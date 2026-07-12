package com.zahri.lighttodo

import com.zahri.lighttodo.test.sourceFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupStartupSourceTest {
    @Test
    fun applicationWaitsForRestoreBeforeStartingAutoBackup() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/App.kt").readText()
        val onCreate = source
            .substringAfter("override fun onCreate()")
            .substringBefore("fun retryRestore()")
        val startup = source.substringAfter("private fun startBackupLifecycle()")
        val launch = startup.indexOf("appScope.launch(Dispatchers.IO)")
        val restore = startup.indexOf("container.backupManager.restoreIfEmpty()")
        val autoBackup = startup.indexOf("container.backupManager.startAutoBackup()")

        assertTrue(onCreate.contains("startBackupLifecycle()"))
        assertFalse(onCreate.contains("container.backupManager.restoreIfEmpty()"))
        assertFalse(onCreate.contains("container.backupManager.startAutoBackup()"))
        assertTrue(launch >= 0 && launch < restore && restore < autoBackup)
    }

    @Test
    fun activityOnlyRetriesRestoreAfterStoragePermissionGrant() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/MainActivity.kt").readText()
        val permissionCallback = source
            .substringAfter("registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())")
            .substringBefore("override fun onCreate")
        val onCreateBeforeContent = source
            .substringAfter("override fun onCreate(savedInstanceState: Bundle?)")
            .substringBefore("setContent")

        assertTrue(permissionCallback.contains("if (storageGranted) app.retryRestore()"))
        assertFalse(onCreateBeforeContent.contains("retryRestore()"))
    }

    @Test
    fun restoreIfEmptyIsAwaitableInsteadOfLaunchingAnotherJob() {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/data/backup/BackupManager.kt"
        ).readText()
        val restore = source
            .substringAfter("suspend fun restoreIfEmpty()")
            .substringBefore("suspend fun restorePortableFromTree")

        assertFalse(restore.contains("scope.launch"))
    }
}

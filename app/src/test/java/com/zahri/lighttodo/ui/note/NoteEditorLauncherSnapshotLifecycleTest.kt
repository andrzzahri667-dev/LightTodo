package com.zahri.lighttodo.ui.note

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NoteEditorLauncherSnapshotLifecycleTest {
    @Test
    fun miuiSnapshotIsNotRecycledImmediatelyAfterBundleHandoff() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/NoteEditorLauncher.kt")
            .readText()

        assertTrue(
            "launcher must track when the snapshot has been handed to MIUI ActivityOptions",
            source.contains("snapshotHandedToSystem")
        )
        assertTrue(
            "launcher may recycle only snapshots that were not accepted by MIUI options",
            source.contains("if (!snapshotHandedToSystem)")
        )
        assertFalse(
            "unconditional recycle in finally races MIUI's asynchronous transition renderer",
            source.contains("finally {\n            snapshot?.recycle()\n        }")
        )
    }

    private fun sourceFile(relativePath: String): File {
        val userDir = requireNotNull(System.getProperty("user.dir"))
        var dir = File(userDir).absoluteFile
        while (true) {
            val candidate = File(dir, relativePath)
            if (candidate.exists()) return candidate
            dir = dir.parentFile ?: break
        }
        error("Could not find $relativePath from $userDir")
    }
}

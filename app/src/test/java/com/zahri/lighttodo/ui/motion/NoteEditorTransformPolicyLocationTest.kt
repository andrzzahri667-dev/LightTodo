package com.zahri.lighttodo.ui.motion

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteEditorTransformPolicyLocationTest {
    @Test
    fun noteEditorTransformPolicyLivesInMotionPackage() {
        val motionSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/NoteEditorContainerTransformPolicy.kt").readText()
        val noteSource = File(sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/NoteEditorWindowPolicy.kt").parentFile, "NoteEditorContainerTransformPolicy.kt")

        assertTrue(motionSource.startsWith("package com.zahri.lighttodo.ui.motion"))
        assertTrue(motionSource.contains("object NoteEditorContainerTransformPolicy"))
        assertFalse("note package must not own animation policy", noteSource.exists())
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

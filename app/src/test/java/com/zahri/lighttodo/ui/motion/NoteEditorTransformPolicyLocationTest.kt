package com.zahri.lighttodo.ui.motion

import com.zahri.lighttodo.test.sourceFile

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteEditorTransformPolicyLocationTest {
    @Test
    fun noteEditorTransformPolicyLivesInMotionPackage() {
        val motionSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/NoteEditorContainerTransformPolicy.kt").readText()
        val noteSource = File(sourceFile("app/src/main/java/com/zahri/lighttodo/feature/noteeditor/NoteEditorWindowPolicy.kt").parentFile, "NoteEditorContainerTransformPolicy.kt")

        assertTrue(motionSource.startsWith("package com.zahri.lighttodo.ui.motion"))
        assertTrue(motionSource.contains("object NoteEditorContainerTransformPolicy"))
        assertFalse("note package must not own animation policy", noteSource.exists())
    }
}

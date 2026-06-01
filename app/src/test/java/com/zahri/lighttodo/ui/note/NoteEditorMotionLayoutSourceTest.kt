package com.zahri.lighttodo.ui.note

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteEditorMotionLayoutSourceTest {
    @Test
    fun customContainerTransformUsesMotionLayoutFallbackEngine() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/note/NoteEditActivity.kt")
            .readText()
        val layerSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionNoteEditorTransformLayer.kt")
            .readText()

        assertTrue(source.contains("import androidx.constraintlayout.compose.MotionLayout"))
        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.components.motionNoteEditorTransformLayer"))
        assertTrue(source.contains("NoteEditorMotionLayoutTransformHost("))
        assertTrue(source.contains("MotionLayout("))
        assertTrue(source.contains("animationSpec = NoteEditorContainerTransformPolicy.entryTween()"))
        assertTrue(source.contains("animationSpec = NoteEditorContainerTransformPolicy.exitTween()"))
        assertTrue(source.contains("geometryProgress = NoteEditorContainerTransformPolicy.geometryProgressFor(progress.value)"))
        assertTrue(source.contains(".motionNoteEditorTransformLayer("))
        assertFalse(source.contains("import androidx.compose.ui.graphics.graphicsLayer"))
        assertFalse(source.contains("transformOrigin = TransformOrigin(0f, 0f)"))
        assertFalse(source.contains("import androidx.compose.animation.core.tween"))
        assertFalse(source.contains("durationMillis = NoteEditorContainerTransformPolicy.EntryDurationMillis"))
        assertFalse(source.contains("durationMillis = NoteEditorContainerTransformPolicy.ExitDurationMillis"))
        assertFalse(source.contains("private fun NoteEditorContainerTransformHost("))
        assertTrue(layerSource.contains("fun Modifier.motionNoteEditorTransformLayer("))
        assertTrue(layerSource.contains("graphicsLayer"))
        assertTrue(layerSource.contains("transformOrigin = TransformOrigin(0f, 0f)"))
        assertTrue(layerSource.contains("this.alpha = alpha"))
        assertTrue(layerSource.contains("clip = cornerRadius > 0.dp"))
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

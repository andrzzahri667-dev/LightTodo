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
        val hostSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionNoteEditorTransformHost.kt")
            .readText()
        val layerSource = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionNoteEditorTransformLayer.kt")
            .readText()

        assertTrue(source.contains("import com.zahri.lighttodo.ui.motion.components.NoteEditorMotionLayoutTransformHost"))
        assertTrue(source.contains("NoteEditorMotionLayoutTransformHost("))
        assertFalse(source.contains("import androidx.constraintlayout.compose.MotionLayout"))
        assertFalse(source.contains("import androidx.constraintlayout.compose.ConstraintSet"))
        assertFalse(source.contains("import androidx.compose.animation.core.Animatable"))
        assertFalse(source.contains("import com.zahri.lighttodo.ui.motion.components.motionNoteEditorTransformLayer"))
        assertFalse(source.contains("private fun NoteEditorMotionLayoutTransformHost("))
        assertFalse(source.contains("private fun noteEditorMotionConstraintSet("))
        assertFalse(source.contains("private object NoteEditorMotionIds"))
        assertTrue(hostSource.contains("fun NoteEditorMotionLayoutTransformHost("))
        assertTrue(hostSource.contains("MotionLayout("))
        assertTrue(hostSource.contains("animationSpec = NoteEditorContainerTransformPolicy.entryTween()"))
        assertTrue(hostSource.contains("animationSpec = NoteEditorContainerTransformPolicy.exitTween()"))
        assertTrue(hostSource.contains("geometryProgress = NoteEditorContainerTransformPolicy.geometryProgressFor(progress.value)"))
        assertTrue(hostSource.contains(".motionNoteEditorTransformLayer("))
        assertFalse(source.contains("import androidx.compose.ui.graphics.graphicsLayer"))
        assertFalse(source.contains("transformOrigin = TransformOrigin(0f, 0f)"))
        assertFalse(source.contains("import androidx.compose.animation.core.tween"))
        assertFalse(source.contains("durationMillis = NoteEditorContainerTransformPolicy.EntryDurationMillis"))
        assertFalse(source.contains("durationMillis = NoteEditorContainerTransformPolicy.ExitDurationMillis"))
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

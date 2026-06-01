package com.zahri.lighttodo.ui.note

import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertTrue
import org.junit.Test

class NoteEditActivityStateRetentionTest {
    @Test
    fun customTransformEntryAnimationStateSurvivesActivityRecreation() {
        val source = sourceFile("app/src/main/java/com/zahri/lighttodo/ui/motion/components/MotionNoteEditorTransformHost.kt")
            .readText()

        assertTrue(
            "entryPlayed must use rememberSaveable so rotation does not replay the entry transform",
            source.contains("var entryPlayed by rememberSaveable { mutableStateOf(false) }")
        )
    }
}

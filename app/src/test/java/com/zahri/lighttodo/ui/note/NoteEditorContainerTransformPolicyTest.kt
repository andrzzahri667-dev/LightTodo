package com.zahri.lighttodo.ui.note

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteEditorContainerTransformPolicyTest {
    @Test
    fun shouldRender_waitsUntilRootHasRealSize() {
        assertEquals(false, NoteEditorContainerTransformPolicy.shouldRender(rootWidth = 0, rootHeight = 2000))
        assertEquals(false, NoteEditorContainerTransformPolicy.shouldRender(rootWidth = 1000, rootHeight = 0))
        assertEquals(true, NoteEditorContainerTransformPolicy.shouldRender(rootWidth = 1000, rootHeight = 2000))
    }

    @Test
    fun timingKeepsEntryDeliberateAndExitVisible() {
        assertEquals(640, NoteEditorContainerTransformPolicy.EntryDurationMillis)
        assertEquals(480, NoteEditorContainerTransformPolicy.ExitDurationMillis)
        assertEquals(40, NoteEditorContainerTransformPolicy.SourceRevealAfterEntryDelayMillis)
    }

    @Test
    fun frameFor_startMatchesSourceBounds() {
        val frame = NoteEditorContainerTransformPolicy.frameFor(
            rootWidth = 1000,
            rootHeight = 2000,
            sourceLeft = 120,
            sourceTop = 320,
            sourceWidth = 250,
            sourceHeight = 180,
            sourceCornerRadiusPx = 36,
            progress = 0f
        )

        assertEquals(0.25f, frame.scaleX, 0.0001f)
        assertEquals(0.09f, frame.scaleY, 0.0001f)
        assertEquals(120f, frame.translationX, 0.0001f)
        assertEquals(320f, frame.translationY, 0.0001f)
        assertEquals(36f, frame.cornerRadiusPx, 0.0001f)
    }

    @Test
    fun frameFor_endMatchesFullScreenEditor() {
        val frame = NoteEditorContainerTransformPolicy.frameFor(
            rootWidth = 1000,
            rootHeight = 2000,
            sourceLeft = 120,
            sourceTop = 320,
            sourceWidth = 250,
            sourceHeight = 180,
            sourceCornerRadiusPx = 36,
            progress = 1f
        )

        assertEquals(1f, frame.scaleX, 0.0001f)
        assertEquals(1f, frame.scaleY, 0.0001f)
        assertEquals(0f, frame.translationX, 0.0001f)
        assertEquals(0f, frame.translationY, 0.0001f)
        assertEquals(0f, frame.cornerRadiusPx, 0.0001f)
    }
}

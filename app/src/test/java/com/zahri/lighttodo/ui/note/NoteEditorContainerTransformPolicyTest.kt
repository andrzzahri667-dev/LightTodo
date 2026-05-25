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

    @Test
    fun contentPhaseFor_keepsSourcePreviewUntilMidpoint() {
        assertEquals(
            NoteEditorTransformContentPhase.SourcePreview,
            NoteEditorContainerTransformPolicy.contentPhaseFor(0f)
        )
        assertEquals(
            NoteEditorTransformContentPhase.SourcePreview,
            NoteEditorContainerTransformPolicy.contentPhaseFor(0.49f)
        )
        assertEquals(
            NoteEditorTransformContentPhase.Editor,
            NoteEditorContainerTransformPolicy.contentPhaseFor(0.5f)
        )
        assertEquals(
            NoteEditorTransformContentPhase.Editor,
            NoteEditorContainerTransformPolicy.contentPhaseFor(1f)
        )
    }

    @Test
    fun contentAlphaFor_crossfadesAroundMidpointInsteadOfHardCutting() {
        val before = NoteEditorContainerTransformPolicy.contentAlphaFor(0.39f)
        val middle = NoteEditorContainerTransformPolicy.contentAlphaFor(0.5f)
        val after = NoteEditorContainerTransformPolicy.contentAlphaFor(0.61f)

        assertEquals(1f, before.sourcePreviewAlpha, 0.0001f)
        assertEquals(0f, before.editorAlpha, 0.0001f)
        assertEquals(0.5f, middle.sourcePreviewAlpha, 0.0001f)
        assertEquals(0.5f, middle.editorAlpha, 0.0001f)
        assertEquals(0f, after.sourcePreviewAlpha, 0.0001f)
        assertEquals(1f, after.editorAlpha, 0.0001f)
    }

    @Test
    fun sourcePreviewFrameFor_startKeepsSourceContentUnscaled() {
        val frame = NoteEditorContainerTransformPolicy.sourcePreviewFrameFor(
            rootWidth = 1000,
            rootHeight = 2000,
            sourceLeft = 120,
            sourceTop = 320,
            sourceWidth = 250,
            sourceHeight = 180,
            sourceCornerRadiusPx = 36,
            progress = 0f
        )

        assertEquals(1f, frame.scaleX, 0.0001f)
        assertEquals(1f, frame.scaleY, 0.0001f)
        assertEquals(120f, frame.translationX, 0.0001f)
        assertEquals(320f, frame.translationY, 0.0001f)
        assertEquals(36f, frame.cornerRadiusPx, 0.0001f)
    }

    @Test
    fun sourcePreviewFrameFor_matchesContainerOuterBoundsWhileGrowing() {
        val container = NoteEditorContainerTransformPolicy.frameFor(
            rootWidth = 1000,
            rootHeight = 2000,
            sourceLeft = 120,
            sourceTop = 320,
            sourceWidth = 250,
            sourceHeight = 200,
            sourceCornerRadiusPx = 36,
            progress = 0.5f
        )
        val preview = NoteEditorContainerTransformPolicy.sourcePreviewFrameFor(
            rootWidth = 1000,
            rootHeight = 2000,
            sourceLeft = 120,
            sourceTop = 320,
            sourceWidth = 250,
            sourceHeight = 200,
            sourceCornerRadiusPx = 36,
            progress = 0.5f
        )

        assertEquals(container.translationX, preview.translationX, 0.0001f)
        assertEquals(container.translationY, preview.translationY, 0.0001f)
        assertEquals(1000f * container.scaleX, 250f * preview.scaleX, 0.0001f)
        assertEquals(2000f * container.scaleY, 200f * preview.scaleY, 0.0001f)
    }

    @Test
    fun sourcePreviewFrameFor_canReusePrecomputedContainerFrame() {
        val container = NoteEditorContainerTransformPolicy.frameFor(
            rootWidth = 1000,
            rootHeight = 2000,
            sourceLeft = 120,
            sourceTop = 320,
            sourceWidth = 250,
            sourceHeight = 200,
            sourceCornerRadiusPx = 36,
            progress = 0.65f
        )

        val preview = NoteEditorContainerTransformPolicy.sourcePreviewFrameFor(
            containerFrame = container,
            rootWidth = 1000,
            rootHeight = 2000,
            sourceWidth = 250,
            sourceHeight = 200
        )

        assertEquals(container.translationX, preview.translationX, 0.0001f)
        assertEquals(container.translationY, preview.translationY, 0.0001f)
        assertEquals(1000f * container.scaleX, 250f * preview.scaleX, 0.0001f)
        assertEquals(2000f * container.scaleY, 200f * preview.scaleY, 0.0001f)
    }
}

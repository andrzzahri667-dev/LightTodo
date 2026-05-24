package com.zahri.lighttodo.ui.note

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteContainerTransformPolicyTest {
    @Test
    fun boundsAt_startMatchesSourceBounds() {
        val bounds = NoteContainerTransformPolicy.boundsAt(
            progress = 0f,
            sourceBounds = Rect(left = 40f, top = 120f, right = 200f, bottom = 280f),
            rootSize = IntSize(width = 400, height = 800)
        )

        assertEquals(40f, bounds.left, 0.001f)
        assertEquals(120f, bounds.top, 0.001f)
        assertEquals(200f, bounds.right, 0.001f)
        assertEquals(280f, bounds.bottom, 0.001f)
    }

    @Test
    fun boundsAt_endFillsRoot() {
        val bounds = NoteContainerTransformPolicy.boundsAt(
            progress = 1f,
            sourceBounds = Rect(left = 40f, top = 120f, right = 200f, bottom = 280f),
            rootSize = IntSize(width = 400, height = 800)
        )

        assertEquals(0f, bounds.left, 0.001f)
        assertEquals(0f, bounds.top, 0.001f)
        assertEquals(400f, bounds.right, 0.001f)
        assertEquals(800f, bounds.bottom, 0.001f)
    }

    @Test
    fun boundsAt_midpointInterpolatesPositionAndSize() {
        val bounds = NoteContainerTransformPolicy.boundsAt(
            progress = 0.5f,
            sourceBounds = Rect(left = 40f, top = 120f, right = 200f, bottom = 280f),
            rootSize = IntSize(width = 400, height = 800)
        )

        assertEquals(20f, bounds.left, 0.001f)
        assertEquals(60f, bounds.top, 0.001f)
        assertEquals(300f, bounds.right, 0.001f)
        assertEquals(540f, bounds.bottom, 0.001f)
    }

    @Test
    fun cornerRadiusAt_roundsFromSourceRadiusToFullScreen() {
        assertEquals(28f, NoteContainerTransformPolicy.cornerRadiusAt(progress = 0f, sourceRadius = 28f), 0.001f)
        assertEquals(14f, NoteContainerTransformPolicy.cornerRadiusAt(progress = 0.5f, sourceRadius = 28f), 0.001f)
        assertEquals(0f, NoteContainerTransformPolicy.cornerRadiusAt(progress = 1f, sourceRadius = 28f), 0.001f)
    }

    @Test
    fun overlayAlphaAt_keepsEnterSurfaceOpaqueUntilRouteIsCovered() {
        assertEquals(
            1f,
            NoteContainerTransformPolicy.overlayAlphaAt(NoteContainerTransformDirection.Enter, progress = 0.7f),
            0.001f
        )
        assertEquals(
            1f,
            NoteContainerTransformPolicy.overlayAlphaAt(NoteContainerTransformDirection.Enter, progress = 1f),
            0.001f
        )
    }

    @Test
    fun overlayAlphaAt_keepsExitSurfaceOpaqueUntilItNearlyReachesSource() {
        assertEquals(
            1f,
            NoteContainerTransformPolicy.overlayAlphaAt(NoteContainerTransformDirection.Exit, progress = 1f),
            0.001f
        )
        assertEquals(
            1f,
            NoteContainerTransformPolicy.overlayAlphaAt(NoteContainerTransformDirection.Exit, progress = 0.22f),
            0.001f
        )
        assertEquals(
            0f,
            NoteContainerTransformPolicy.overlayAlphaAt(NoteContainerTransformDirection.Exit, progress = 0f),
            0.001f
        )
    }

    @Test
    fun sourceSnapshotAlphaAt_fadesSourceSnapshotOutDuringEnter() {
        assertEquals(
            1f,
            NoteContainerTransformPolicy.sourceSnapshotAlphaAt(NoteContainerTransformDirection.Enter, progress = 0.24f),
            0.001f
        )
        assertEquals(
            0.5f,
            NoteContainerTransformPolicy.sourceSnapshotAlphaAt(NoteContainerTransformDirection.Enter, progress = 0.4f),
            0.001f
        )
        assertEquals(
            0f,
            NoteContainerTransformPolicy.sourceSnapshotAlphaAt(NoteContainerTransformDirection.Enter, progress = 0.56f),
            0.001f
        )
    }
}

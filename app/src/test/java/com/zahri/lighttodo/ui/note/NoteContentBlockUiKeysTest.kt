package com.zahri.lighttodo.feature.noteeditor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NoteContentBlockUiKeysTest {
    @Test
    fun keyFor_usesMediaRefForStableImageIdentity() {
        assertEquals(
            "image:lighttodo://attachment/image/photo.jpg",
            NoteContentBlockUiKeys.keyFor(
                index = 4,
                block = NoteContentBlock.Image("lighttodo://attachment/image/photo.jpg")
            )
        )
    }

    @Test
    fun keyFor_usesAudioRefForStableAudioIdentity() {
        assertEquals(
            "audio:lighttodo://attachment/audio/clip.m4a",
            NoteContentBlockUiKeys.keyFor(
                index = 2,
                block = NoteContentBlock.Audio(
                    ref = "lighttodo://attachment/audio/clip.m4a",
                    durationLabel = "00:08"
                )
            )
        )
    }

    @Test
    fun keyFor_keepsTextIdentityIndependentFromTextEdits() {
        val before = NoteContentBlockUiKeys.keyFor(1, NoteContentBlock.Text("draft"))
        val after = NoteContentBlockUiKeys.keyFor(1, NoteContentBlock.Text("draft updated"))

        assertEquals(before, after)
        assertNotEquals(before, NoteContentBlockUiKeys.keyFor(2, NoteContentBlock.Text("draft updated")))
    }
}

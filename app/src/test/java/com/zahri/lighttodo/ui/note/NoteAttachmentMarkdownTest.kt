package com.zahri.lighttodo.domain.note

import com.zahri.lighttodo.feature.noteeditor.MarkdownSpanApplier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteAttachmentMarkdownTest {

    @Test
    fun image_buildsStableMarkdownMarker() {
        assertEquals(
            "![image](lighttodo://attachment/image/photo.jpg)",
            NoteAttachmentMarkdown.image("lighttodo://attachment/image/photo.jpg")
        )
    }

    @Test
    fun audio_buildsStableMarkdownMarkerWithDuration() {
        assertEquals(
            "[audio 01:05](lighttodo://attachment/audio/clip.m4a)",
            NoteAttachmentMarkdown.audio("lighttodo://attachment/audio/clip.m4a", 65_000L)
        )
    }

    @Test
    fun audio_buildsParseableMarkdownMarkerForLongDuration() {
        val marker = NoteAttachmentMarkdown.audio(
            "lighttodo://attachment/audio/long.m4a",
            6_000_000L
        )

        val parsed = NoteAttachmentMarkdown.parseLine(marker)

        assertEquals("[audio 100:00](lighttodo://attachment/audio/long.m4a)", marker)
        assertEquals(NoteAttachmentMarkdown.Kind.Audio, parsed?.kind)
        assertEquals("100:00", parsed?.label)
        assertEquals("lighttodo://attachment/audio/long.m4a", parsed?.ref)
    }

    @Test
    fun parseLine_recognizesImageMarker() {
        val parsed = NoteAttachmentMarkdown.parseLine(
            "![image](lighttodo://attachment/image/photo.jpg)"
        )

        assertEquals(NoteAttachmentMarkdown.Kind.Image, parsed?.kind)
        assertEquals("lighttodo://attachment/image/photo.jpg", parsed?.ref)
    }

    @Test
    fun parseLine_recognizesAudioMarker() {
        val parsed = NoteAttachmentMarkdown.parseLine(
            "[audio 00:08](lighttodo://attachment/audio/clip.m4a)"
        )

        assertEquals(NoteAttachmentMarkdown.Kind.Audio, parsed?.kind)
        assertEquals("00:08", parsed?.label)
        assertEquals("lighttodo://attachment/audio/clip.m4a", parsed?.ref)
    }

    @Test
    fun parseLine_ignoresRegularMarkdownLink() {
        assertNull(NoteAttachmentMarkdown.parseLine("[site](https://example.com)"))
    }

    @Test
    fun parseLine_ignoresMalformedAudioDuration() {
        assertNull(NoteAttachmentMarkdown.parseLine("[audio 10:7](lighttodo://attachment/audio/clip.m4a)"))
        assertNull(NoteAttachmentMarkdown.parseLine("[audio xx:07](lighttodo://attachment/audio/clip.m4a)"))
        assertNull(NoteAttachmentMarkdown.parseLine("[audio 10:99](lighttodo://attachment/audio/clip.m4a)"))
    }

    @Test
    fun formatDuration_clampsNegativeValues() {
        assertEquals("00:00", NoteAttachmentMarkdown.formatDuration(-1L))
    }

    @Test
    fun stripMarkdown_replacesAttachmentLinesForPreview() {
        val preview = MarkdownSpanApplier.stripMarkdown(
            "before\n" +
                "![image](lighttodo://attachment/image/photo.jpg)\n" +
                "[audio 00:08](lighttodo://attachment/audio/clip.m4a)"
        )

        assertEquals("before\nImage\nAudio 00:08", preview)
    }

    @Test
    fun refsIn_returnsAttachmentRefsInContentOrder() {
        val refs = NoteAttachmentMarkdown.refsIn(
            "a\n" +
                "![image](lighttodo://attachment/image/photo.jpg)\n" +
                "[audio 00:08](lighttodo://attachment/audio/clip.m4a)\n" +
                "[site](https://example.com)"
        )

        assertEquals(
            listOf(
                "lighttodo://attachment/image/photo.jpg",
                "lighttodo://attachment/audio/clip.m4a"
            ),
            refs
        )
    }

    @Test
    fun removedRefs_returnsOnlyRefsMissingFromCurrentContent() {
        val removed = NoteAttachmentMarkdown.removedRefs(
            previousMarkdown = "a\n" +
                "![image](lighttodo://attachment/image/photo.jpg)\n" +
                "[audio 00:08](lighttodo://attachment/audio/clip.m4a)",
            currentMarkdown = "a\n" +
                "[audio 00:08](lighttodo://attachment/audio/clip.m4a)"
        )

        assertEquals(listOf("lighttodo://attachment/image/photo.jpg"), removed)
    }

    @Test
    fun removeAttachment_removesOnlyMatchingAttachmentLine() {
        val updated = NoteAttachmentMarkdown.removeAttachment(
            "a\n" +
                "![image](lighttodo://attachment/image/photo.jpg)\n" +
                "[audio 00:08](lighttodo://attachment/audio/clip.m4a)\n" +
                "b",
            "lighttodo://attachment/image/photo.jpg"
        )

        assertEquals(
            "a\n" +
                "[audio 00:08](lighttodo://attachment/audio/clip.m4a)\n" +
                "b",
            updated
        )
    }
}

package com.zahri.lighttodo.feature.noteeditor

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteContentBlocksTest {

    @Test
    fun parse_preservesTextImageAndAudioBlockOrder() {
        val blocks = NoteContentBlocks.parse(
            "hello\n" +
                "![image](lighttodo://attachment/image/photo.jpg)\n" +
                "middle\n" +
                "[audio 00:08](lighttodo://attachment/audio/clip.m4a)\n" +
                "tail"
        )

        assertEquals(
            listOf(
                NoteContentBlock.Text("hello"),
                NoteContentBlock.Image("lighttodo://attachment/image/photo.jpg"),
                NoteContentBlock.Text("middle"),
                NoteContentBlock.Audio("lighttodo://attachment/audio/clip.m4a", "00:08"),
                NoteContentBlock.Text("tail")
            ),
            blocks
        )
    }

    @Test
    fun parse_keepsAdjacentTextLinesTogether() {
        val blocks = NoteContentBlocks.parse(
            "line 1\n" +
                "line 2\n" +
                "\n" +
                "line 4"
        )

        assertEquals(
            listOf(NoteContentBlock.Text("line 1\nline 2\n\nline 4")),
            blocks
        )
    }

    @Test
    fun parseAndSerialize_preservesPlainTextTrailingNewlines() {
        val markdown = "line 1\nline 2\n\n"

        assertEquals(markdown, NoteContentBlocks.serialize(NoteContentBlocks.parse(markdown)))
    }

    @Test
    fun serialize_roundTripsMixedBlocksToMarkdown() {
        val markdown = NoteContentBlocks.serialize(
            listOf(
                NoteContentBlock.Text("hello"),
                NoteContentBlock.Image("lighttodo://attachment/image/photo.jpg"),
                NoteContentBlock.Audio("lighttodo://attachment/audio/clip.m4a", "00:08"),
                NoteContentBlock.Text("tail")
            )
        )

        assertEquals(
            "hello\n" +
                "![image](lighttodo://attachment/image/photo.jpg)\n" +
                "[audio 00:08](lighttodo://attachment/audio/clip.m4a)\n" +
                "tail",
            markdown
        )
    }

    @Test
    fun insertAfterTextCursor_addsEditableTextLineAfterInsertedMedia() {
        val result = NoteContentBlocks.insertAfterTextCursor(
            blocks = listOf(NoteContentBlock.Text("hello")),
            textBlockIndex = 0,
            cursor = 5,
            insertedBlock = NoteContentBlock.Image("lighttodo://attachment/image/photo.jpg")
        )

        assertEquals(
            NoteContentBlocks.InsertResult(
                blocks = listOf(
                    NoteContentBlock.Text("hello"),
                    NoteContentBlock.Image("lighttodo://attachment/image/photo.jpg"),
                    NoteContentBlock.Text("")
                ),
                focusTextIndex = 2
            ),
            result
        )
    }

    @Test
    fun insertAfterTextCursor_serializesWithSingleNewlineAfterInsertedMedia() {
        val result = NoteContentBlocks.insertAfterTextCursor(
            blocks = listOf(NoteContentBlock.Text("hello")),
            textBlockIndex = 0,
            cursor = 5,
            insertedBlock = NoteContentBlock.Image("lighttodo://attachment/image/photo.jpg")
        )

        assertEquals(
            "hello\n![image](lighttodo://attachment/image/photo.jpg)\n",
            NoteContentBlocks.serialize(result.blocks)
        )
    }

    @Test
    fun parse_preservesEditableTextBlockAfterTrailingMediaNewline() {
        val blocks = NoteContentBlocks.parse(
            "hello\n![image](lighttodo://attachment/image/photo.jpg)\n"
        )

        assertEquals(
            listOf(
                NoteContentBlock.Text("hello"),
                NoteContentBlock.Image("lighttodo://attachment/image/photo.jpg"),
                NoteContentBlock.Text("")
            ),
            blocks
        )
    }

    @Test
    fun removeMediaBeforeTextCursor_deletesPreviousImageAndMergesText() {
        val result = NoteContentBlocks.removeMediaBeforeTextCursor(
            blocks = listOf(
                NoteContentBlock.Text("hello"),
                NoteContentBlock.Image("lighttodo://attachment/image/photo.jpg"),
                NoteContentBlock.Text("\n")
            ),
            textBlockIndex = 2,
            cursor = 0
        )

        assertEquals(
            NoteContentBlocks.RemoveResult(
                blocks = listOf(NoteContentBlock.Text("hello\n")),
                focusTextIndex = 0,
                removedRef = "lighttodo://attachment/image/photo.jpg"
            ),
            result
        )
    }

    @Test
    fun mediaRefBeforeTextCursor_findsPreviousMediaWithoutChangingBlocks() {
        val ref = NoteContentBlocks.mediaRefBeforeTextCursor(
            blocks = listOf(
                NoteContentBlock.Image("lighttodo://attachment/image/photo.jpg"),
                NoteContentBlock.Text("line 2")
            ),
            textBlockIndex = 1,
            cursor = 0
        )

        assertEquals("lighttodo://attachment/image/photo.jpg", ref)
    }

    @Test
    fun removeMediaBeforeTextCursor_deletesPreviousAudioAndMergesText() {
        val result = NoteContentBlocks.removeMediaBeforeTextCursor(
            blocks = listOf(
                NoteContentBlock.Text("before"),
                NoteContentBlock.Audio("lighttodo://attachment/audio/clip.m4a", "00:08"),
                NoteContentBlock.Text("after")
            ),
            textBlockIndex = 2,
            cursor = 0
        )

        assertEquals(
            NoteContentBlocks.RemoveResult(
                blocks = listOf(NoteContentBlock.Text("before\nafter")),
                focusTextIndex = 0,
                removedRef = "lighttodo://attachment/audio/clip.m4a"
            ),
            result
        )
    }

    @Test
    fun removeAttachment_removesOnlyMatchingMediaBlock() {
        val markdown = NoteContentBlocks.removeAttachment(
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
            markdown
        )
    }
}

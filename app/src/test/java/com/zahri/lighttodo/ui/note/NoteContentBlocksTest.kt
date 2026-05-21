package com.zahri.lighttodo.ui.note

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
                    NoteContentBlock.Text("\n")
                ),
                focusTextIndex = 2
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

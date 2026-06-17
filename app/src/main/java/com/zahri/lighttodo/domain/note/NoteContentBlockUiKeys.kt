package com.zahri.lighttodo.domain.note

object NoteContentBlockUiKeys {
    fun keyFor(index: Int, block: NoteContentBlock): String =
        when (block) {
            is NoteContentBlock.Text -> "text:$index"
            is NoteContentBlock.Image -> "image:${block.ref}"
            is NoteContentBlock.Audio -> "audio:${block.ref}"
        }
}

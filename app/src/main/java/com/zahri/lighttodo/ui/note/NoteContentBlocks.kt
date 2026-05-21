package com.zahri.lighttodo.ui.note

sealed interface NoteContentBlock {
    data class Text(val text: String) : NoteContentBlock
    data class Image(val ref: String) : NoteContentBlock
    data class Audio(val ref: String, val durationLabel: String) : NoteContentBlock
}

object NoteContentBlocks {

    data class InsertResult(
        val blocks: List<NoteContentBlock>,
        val focusTextIndex: Int
    )

    fun parse(markdown: String): List<NoteContentBlock> {
        if (markdown.isEmpty()) return listOf(NoteContentBlock.Text(""))
        if (markdown.lineSequence().none { NoteAttachmentMarkdown.parseLine(it) != null }) {
            return listOf(NoteContentBlock.Text(markdown))
        }

        val blocks = mutableListOf<NoteContentBlock>()
        val textBuffer = StringBuilder()

        fun flushText(dropSeparatorNewline: Boolean) {
            if (dropSeparatorNewline && textBuffer.endsWith('\n')) {
                textBuffer.deleteAt(textBuffer.lastIndex)
            }
            if (textBuffer.isNotEmpty()) {
                blocks += NoteContentBlock.Text(textBuffer.toString())
                textBuffer.clear()
            }
        }

        var position = 0
        while (position <= markdown.length) {
            val newlineIndex = markdown.indexOf('\n', position)
            val lineEnd = if (newlineIndex == -1) markdown.length else newlineIndex
            val line = markdown.substring(position, lineEnd)
            val hasNewline = newlineIndex != -1
            val attachment = NoteAttachmentMarkdown.parseLine(line)
            if (attachment == null) {
                textBuffer.append(line)
                if (hasNewline) textBuffer.append('\n')
            } else {
                flushText(dropSeparatorNewline = true)
                blocks += when (attachment.kind) {
                    NoteAttachmentMarkdown.Kind.Image -> NoteContentBlock.Image(attachment.ref)
                    NoteAttachmentMarkdown.Kind.Audio -> NoteContentBlock.Audio(
                        ref = attachment.ref,
                        durationLabel = attachment.label
                    )
                }
            }
            if (!hasNewline) break
            position = newlineIndex + 1
        }

        flushText(dropSeparatorNewline = false)
        return blocks.ifEmpty { listOf(NoteContentBlock.Text("")) }
    }

    fun serialize(blocks: List<NoteContentBlock>): String =
        blocks
            .filterNot { it is NoteContentBlock.Text && it.text.isEmpty() && blocks.size > 1 }
            .joinToString("\n") { block ->
                when (block) {
                    is NoteContentBlock.Text -> block.text
                    is NoteContentBlock.Image -> NoteAttachmentMarkdown.image(block.ref)
                    is NoteContentBlock.Audio -> NoteAttachmentMarkdown.audio(
                        ref = block.ref,
                        durationLabel = block.durationLabel
                    )
                }
            }

    fun removeAttachment(markdown: String, ref: String): String =
        serialize(parse(markdown).filterNot { block ->
            when (block) {
                is NoteContentBlock.Image -> block.ref == ref
                is NoteContentBlock.Audio -> block.ref == ref
                is NoteContentBlock.Text -> false
            }
        })

    fun insertAfterTextCursor(
        blocks: List<NoteContentBlock>,
        textBlockIndex: Int,
        cursor: Int,
        insertedBlock: NoteContentBlock
    ): InsertResult {
        val safeBlocks = blocks.ifEmpty { listOf(NoteContentBlock.Text("")) }
        val index = textBlockIndex.coerceIn(0, safeBlocks.lastIndex)
        val current = safeBlocks[index]
        val nextBlocks = mutableListOf<NoteContentBlock>()
        var focusAfterInsert = safeBlocks.size

        if (current is NoteContentBlock.Text) {
            val safeCursor = cursor.coerceIn(0, current.text.length)
            val before = current.text.substring(0, safeCursor)
            val after = current.text.substring(safeCursor).ifEmpty { "\n" }
            safeBlocks.forEachIndexed { blockIndex, existing ->
                if (blockIndex != index) {
                    nextBlocks += existing
                } else {
                    if (before.isNotEmpty()) nextBlocks += NoteContentBlock.Text(before)
                    nextBlocks += insertedBlock
                    focusAfterInsert = nextBlocks.size
                    nextBlocks += NoteContentBlock.Text(after)
                }
            }
        } else {
            nextBlocks += safeBlocks
            nextBlocks += insertedBlock
            focusAfterInsert = nextBlocks.size
            nextBlocks += NoteContentBlock.Text("\n")
        }

        return InsertResult(nextBlocks, focusAfterInsert)
    }
}

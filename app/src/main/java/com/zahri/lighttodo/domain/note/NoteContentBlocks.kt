package com.zahri.lighttodo.domain.note

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

    data class RemoveResult(
        val blocks: List<NoteContentBlock>,
        val focusTextIndex: Int,
        val removedRef: String
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
        if (markdown.endsWith('\n') && blocks.lastOrNull() !is NoteContentBlock.Text) {
            blocks += NoteContentBlock.Text("")
        }
        return blocks.ifEmpty { listOf(NoteContentBlock.Text("")) }
    }

    fun serialize(blocks: List<NoteContentBlock>): String =
        blocks.joinToString("\n") { block ->
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
            val after = current.text.substring(safeCursor)
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

    fun removeMediaBeforeTextCursor(
        blocks: List<NoteContentBlock>,
        textBlockIndex: Int,
        cursor: Int
    ): RemoveResult? {
        val removedRef = mediaRefBeforeTextCursor(blocks, textBlockIndex, cursor) ?: return null
        val index = textBlockIndex
        val current = blocks[index] as NoteContentBlock.Text
        val mediaIndex = index - 1

        val previousTextIndex = mediaIndex - 1
        val previousText = blocks.getOrNull(previousTextIndex) as? NoteContentBlock.Text
        val nextBlocks = mutableListOf<NoteContentBlock>()
        val focusTextIndex: Int

        if (previousText != null) {
            nextBlocks += blocks.take(previousTextIndex)
            focusTextIndex = nextBlocks.size
            nextBlocks += NoteContentBlock.Text(joinTextAroundRemovedMedia(previousText.text, current.text))
            nextBlocks += blocks.drop(index + 1)
        } else {
            nextBlocks += blocks.take(mediaIndex)
            focusTextIndex = nextBlocks.size
            nextBlocks += current
            nextBlocks += blocks.drop(index + 1)
        }

        return RemoveResult(
            blocks = nextBlocks.ifEmpty { listOf(NoteContentBlock.Text("")) },
            focusTextIndex = focusTextIndex.coerceAtLeast(0),
            removedRef = removedRef
        )
    }

    fun mediaRefBeforeTextCursor(
        blocks: List<NoteContentBlock>,
        textBlockIndex: Int,
        cursor: Int
    ): String? {
        if (cursor != 0) return null
        val index = textBlockIndex.takeIf { it in blocks.indices } ?: return null
        blocks[index] as? NoteContentBlock.Text ?: return null
        return when (val media = blocks.getOrNull(index - 1)) {
            is NoteContentBlock.Image -> media.ref
            is NoteContentBlock.Audio -> media.ref
            is NoteContentBlock.Text, null -> null
        }
    }

    private fun joinTextAroundRemovedMedia(before: String, after: String): String {
        if (before.isEmpty()) return after
        if (after.isEmpty()) return before
        val separator = if (before.endsWith('\n') || after.startsWith('\n')) "" else "\n"
        return before + separator + after
    }
}

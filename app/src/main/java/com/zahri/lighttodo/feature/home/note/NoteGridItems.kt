package com.zahri.lighttodo.feature.home.note

import com.zahri.lighttodo.feature.noteeditor.MarkdownSpanApplier
import com.zahri.lighttodo.usecase.note.NoteListItem

data class NoteGridItem(
    val id: Long,
    val title: String?,
    val preview: String?,
    val previewMaxLines: Int,
    val showEmptyPlaceholder: Boolean
)

fun buildNoteGridItems(notes: List<NoteListItem>): List<NoteGridItem> =
    notes.map(::buildNoteGridItem)

class NoteGridItemMemoizer {
    private val cache = mutableMapOf<NoteGridItemCacheKey, NoteGridItem>()

    fun itemsFor(notes: List<NoteListItem>): List<NoteGridItem> {
        val liveKeys = notes.map { it.cacheKey() }.toSet()
        cache.keys.retainAll(liveKeys)
        return notes.map { note ->
            cache.getOrPut(note.cacheKey()) { buildNoteGridItem(note) }
        }
    }
}

private fun buildNoteGridItem(note: NoteListItem): NoteGridItem {
    val title = note.title?.takeIf { it.isNotBlank() }
    val preview = note.content
        .takeIf { it.isNotBlank() }
        ?.let { MarkdownSpanApplier.stripMarkdown(it) }
        ?.takeIf { it.isNotBlank() }

    return NoteGridItem(
        id = note.id,
        title = title,
        preview = preview,
        previewMaxLines = if (title != null) 5 else 7,
        showEmptyPlaceholder = title == null && preview == null
    )
}

private data class NoteGridItemCacheKey(
    val id: Long,
    val title: String?,
    val content: String
)

private fun NoteListItem.cacheKey(): NoteGridItemCacheKey =
    NoteGridItemCacheKey(id = id, title = title, content = content)

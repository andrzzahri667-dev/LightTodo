package com.zahri.lighttodo.ui.home.note

import com.zahri.lighttodo.data.NoteEntity
import com.zahri.lighttodo.ui.note.MarkdownSpanApplier

data class NoteGridItem(
    val id: Long,
    val title: String?,
    val preview: String?,
    val previewMaxLines: Int,
    val showEmptyPlaceholder: Boolean
)

fun buildNoteGridItems(notes: List<NoteEntity>): List<NoteGridItem> =
    notes.map { note ->
        val title = note.title?.takeIf { it.isNotBlank() }
        val preview = note.content
            .takeIf { it.isNotBlank() }
            ?.let { MarkdownSpanApplier.stripMarkdown(it) }
            ?.takeIf { it.isNotBlank() }

        NoteGridItem(
            id = note.id,
            title = title,
            preview = preview,
            previewMaxLines = if (title != null) 5 else 7,
            showEmptyPlaceholder = title == null && preview == null
        )
    }

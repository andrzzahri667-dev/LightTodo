package com.zahri.lighttodo.feature.noteeditor

import com.zahri.lighttodo.data.NoteEntity

data class NoteEditLaunchSeed(
    val id: Long,
    val title: String?,
    val content: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    companion object {
        fun from(note: NoteEntity): NoteEditLaunchSeed =
            NoteEditLaunchSeed(
                id = note.id,
                title = note.title,
                content = note.content,
                createdAtMillis = note.createdAtMillis,
                updatedAtMillis = note.updatedAtMillis
            )
    }
}

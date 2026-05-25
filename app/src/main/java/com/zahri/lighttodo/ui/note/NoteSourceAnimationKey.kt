package com.zahri.lighttodo.ui.note

data class NoteSourceAnimationKey private constructor(private val noteId: Long?) {
    fun matches(noteId: Long?): Boolean = this.noteId == noteId

    companion object {
        fun forNewNote(): NoteSourceAnimationKey = NoteSourceAnimationKey(null)

        fun forExistingNote(noteId: Long): NoteSourceAnimationKey = NoteSourceAnimationKey(noteId)
    }
}

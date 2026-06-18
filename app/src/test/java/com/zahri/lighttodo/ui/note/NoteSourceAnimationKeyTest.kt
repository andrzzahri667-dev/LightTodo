package com.zahri.lighttodo.feature.noteeditor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSourceAnimationKeyTest {
    @Test
    fun forNewNote_matchesOnlyCreateSource() {
        val key = NoteSourceAnimationKey.forNewNote()

        assertTrue(key.matches(noteId = null))
        assertFalse(key.matches(noteId = 42L))
    }

    @Test
    fun forExistingNote_matchesOnlySameNoteId() {
        val key = NoteSourceAnimationKey.forExistingNote(42L)

        assertTrue(key.matches(noteId = 42L))
        assertFalse(key.matches(noteId = 7L))
        assertFalse(key.matches(noteId = null))
    }
}

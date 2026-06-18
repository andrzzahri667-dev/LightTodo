package com.zahri.lighttodo.feature.home.note

import com.zahri.lighttodo.usecase.note.NoteListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteGridItemsTest {

    @Test
    fun buildNoteGridItems_precomputesMarkdownPreview() {
        val items = buildNoteGridItems(
            listOf(
                NoteListItem(
                    id = 7L,
                    title = "Plan",
                    content = "# Heading\n- **Done**",
                    createdAtMillis = 0L,
                    updatedAtMillis = 0L
                )
            )
        )

        assertEquals(1, items.size)
        assertEquals(7L, items[0].id)
        assertEquals("Plan", items[0].title)
        assertEquals("Heading\nDone", items[0].preview)
        assertEquals(5, items[0].previewMaxLines)
        assertFalse(items[0].showEmptyPlaceholder)
    }

    @Test
    fun buildNoteGridItems_usesMorePreviewLinesWhenTitleMissing() {
        val item = buildNoteGridItems(
            listOf(note(id = 8L, title = " ", content = "Body"))
        ).single()

        assertNull(item.title)
        assertEquals("Body", item.preview)
        assertEquals(7, item.previewMaxLines)
        assertFalse(item.showEmptyPlaceholder)
    }

    @Test
    fun buildNoteGridItems_marksEmptyUntitledNotes() {
        val item = buildNoteGridItems(
            listOf(note(id = 9L, title = null, content = "   "))
        ).single()

        assertNull(item.title)
        assertNull(item.preview)
        assertTrue(item.showEmptyPlaceholder)
    }

    @Test
    fun noteGridItemMemoizer_reusesUnchangedItemsAcrossNewListInstances() {
        val memoizer = NoteGridItemMemoizer()
        val note = note(id = 10L, title = "Plan", content = "**Body**")

        val first = memoizer.itemsFor(listOf(note)).single()
        val second = memoizer.itemsFor(listOf(note.copy(updatedAtMillis = note.updatedAtMillis + 1))).single()
        val changed = memoizer.itemsFor(listOf(note.copy(content = "Changed"))).single()

        assertSame(first, second)
        assertNotSame(first, changed)
        assertEquals("Changed", changed.preview)
    }

    private fun note(id: Long, title: String?, content: String): NoteListItem =
        NoteListItem(
            id = id,
            title = title,
            content = content,
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )
}

package com.zahri.lighttodo.ui.home.note

import com.zahri.lighttodo.data.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteGridItemsTest {

    @Test
    fun buildNoteGridItems_precomputesMarkdownPreview() {
        val items = buildNoteGridItems(
            listOf(
                NoteEntity(
                    id = 7L,
                    title = "Plan",
                    content = "# Heading\n- **Done**"
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
            listOf(NoteEntity(id = 8L, title = " ", content = "Body"))
        ).single()

        assertNull(item.title)
        assertEquals("Body", item.preview)
        assertEquals(7, item.previewMaxLines)
        assertFalse(item.showEmptyPlaceholder)
    }

    @Test
    fun buildNoteGridItems_marksEmptyUntitledNotes() {
        val item = buildNoteGridItems(
            listOf(NoteEntity(id = 9L, title = null, content = "   "))
        ).single()

        assertNull(item.title)
        assertNull(item.preview)
        assertTrue(item.showEmptyPlaceholder)
    }
}

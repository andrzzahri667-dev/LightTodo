package com.zahri.lighttodo.ui.home

import com.zahri.lighttodo.data.TodoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTodoListItemsTest {

    @Test
    fun buildHomeTodoListItems_keepsCollapsedRowsForAnimatedExit() {
        val state = HomeUiState(
            groups = listOf(
                TagGroup(tagId = 1L, name = "Work", items = listOf(todo(1L), todo(2L))),
                TagGroup(tagId = null, name = "Inbox", items = listOf(todo(3L)))
            ),
            doneItems = listOf(todo(4L, done = true)),
            collapsedTagIds = setOf("tag-1"),
            doneExpanded = false
        )

        val items = buildHomeTodoListItems(state)

        assertEquals(
            listOf("header-tag-1", "todo-1", "todo-2", "header-uncat", "todo-3", "done-header", "done-4"),
            items.map { it.key }
        )
        assertFalse((items[0] as HomeTodoListItem.Header).expanded)
        assertFalse((items[1] as HomeTodoListItem.TodoRow).visible)
        assertFalse((items[2] as HomeTodoListItem.TodoRow).visible)
        assertTrue((items[3] as HomeTodoListItem.Header).expanded)
        assertTrue((items[4] as HomeTodoListItem.TodoRow).visible)
        assertFalse((items[6] as HomeTodoListItem.TodoRow).visible)
    }

    @Test
    fun buildHomeTodoListItems_marksDoneRowsAndDividers() {
        val state = HomeUiState(
            groups = listOf(TagGroup(tagId = 2L, name = "Today", items = listOf(todo(10L), todo(11L)))),
            doneItems = listOf(todo(20L, done = true), todo(21L, done = true)),
            collapsedTagIds = emptySet(),
            doneExpanded = true
        )

        val items = buildHomeTodoListItems(state)

        assertEquals(
            listOf("header-tag-2", "todo-10", "todo-11", "done-header", "done-20", "done-21"),
            items.map { it.key }
        )
        val firstActive = items[1] as HomeTodoListItem.TodoRow
        val lastActive = items[2] as HomeTodoListItem.TodoRow
        val firstDone = items[4] as HomeTodoListItem.TodoRow
        val lastDone = items[5] as HomeTodoListItem.TodoRow

        assertTrue(firstActive.showDivider)
        assertTrue(firstActive.visible)
        assertFalse(lastActive.showDivider)
        assertFalse(firstActive.strikeThrough)
        assertTrue(firstDone.showDivider)
        assertTrue(firstDone.visible)
        assertFalse(lastDone.showDivider)
        assertTrue(firstDone.strikeThrough)
    }

    private fun todo(id: Long, done: Boolean = false): TodoEntity =
        TodoEntity(
            id = id,
            title = "Todo $id",
            done = done,
            doneAtMillis = if (done) id else null,
            createdAtMillis = id
        )
}

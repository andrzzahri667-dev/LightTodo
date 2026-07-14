package com.zahri.lighttodo.feature.home

import com.zahri.lighttodo.usecase.todo.HomeTodo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTodoListItemsTest {

    @Test
    fun buildHomeTodoListItems_omitsRowsFromCollapsedSections() {
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
            listOf("header-tag-1", "header-uncat", "todo-3", "done-header"),
            items.map { it.key }
        )
        assertFalse((items[0] as HomeTodoListItem.Header).expanded)
        assertTrue((items[1] as HomeTodoListItem.Header).expanded)
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
            listOf("header-tag-2", "todo-10", "todo-11", "done-header", "todo-20", "todo-21"),
            items.map { it.key }
        )
        val firstActive = items[1] as HomeTodoListItem.TodoRow
        val lastActive = items[2] as HomeTodoListItem.TodoRow
        val firstDone = items[4] as HomeTodoListItem.TodoRow
        val lastDone = items[5] as HomeTodoListItem.TodoRow

        assertTrue(firstActive.showDivider)
        assertFalse(lastActive.showDivider)
        assertFalse(firstActive.strikeThrough)
        assertTrue(firstDone.showDivider)
        assertFalse(lastDone.showDivider)
        assertTrue(firstDone.strikeThrough)
    }

    @Test
    fun homeTodoListItemsExposeCompatibleLazyContentTypes() {
        val items = buildHomeTodoListItems(
            HomeUiState(
                groups = listOf(TagGroup(tagId = 2L, name = "Today", items = listOf(todo(10L)))),
                doneItems = emptyList(),
                collapsedTagIds = emptySet(),
                doneExpanded = false
            )
        )
        val contentTypeGetter = HomeTodoListItem::class.java.methods
            .singleOrNull { it.name == "getContentType" }

        assertNotNull(contentTypeGetter)
        assertEquals("HEADER", contentTypeGetter?.invoke(items[0]).toString())
        assertEquals("TODO_ROW", contentTypeGetter?.invoke(items[1]).toString())
    }

    private fun todo(id: Long, done: Boolean = false): HomeTodo =
        HomeTodo(
            id = id,
            title = "Todo $id",
            note = null,
            date = null,
            dateMillis = null,
            startHour = null,
            startMinute = null,
            deadlineHour = null,
            deadlineMinute = null,
            tagId = null,
            done = done,
            doneAtMillis = if (done) id else null,
            createdAtMillis = id,
            calendarEventId = null
        )
}

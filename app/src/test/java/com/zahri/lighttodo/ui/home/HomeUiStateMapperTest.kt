package com.zahri.lighttodo.ui.home

import com.zahri.lighttodo.data.HomeData
import com.zahri.lighttodo.data.TagEntity
import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.data.UserPrefs
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeUiStateMapperTest {

    @Test
    fun buildHomeUiState_groupsUndoneByTagAndSortsUndatedLast() {
        val state = buildHomeUiState(
            data = HomeData(
                todos = listOf(
                    todo(id = 1L, tagId = 2L, dateMillis = null, createdAtMillis = 20L),
                    todo(id = 2L, tagId = 2L, dateMillis = 1_000L, createdAtMillis = 30L),
                    todo(id = 3L, tagId = null, dateMillis = 2_000L, createdAtMillis = 10L),
                    todo(id = 4L, tagId = 99L, dateMillis = 3_000L, createdAtMillis = 40L)
                ),
                tags = listOf(TagEntity(id = 2L, name = "Work")),
                prefs = UserPrefs.Snapshot(collapsedTagIds = setOf("tag-2"), doneSectionExpanded = true)
            ),
            uncategorizedTitle = "Inbox"
        )

        assertEquals(listOf("Work", "Inbox"), state.groups.map { it.name })
        assertEquals(listOf(2L, 1L), state.groups[0].items.map { it.id })
        assertEquals(listOf(3L), state.groups[1].items.map { it.id })
        assertEquals(setOf("tag-2"), state.collapsedTagIds)
        assertEquals(true, state.doneExpanded)
    }

    @Test
    fun buildHomeUiState_sortsDoneItemsByCompletionTimeDescending() {
        val state = buildHomeUiState(
            data = HomeData(
                todos = listOf(
                    todo(id = 1L, done = true, doneAtMillis = 10L),
                    todo(id = 2L, done = true, doneAtMillis = 30L),
                    todo(id = 3L, done = true, doneAtMillis = null)
                ),
                tags = emptyList(),
                prefs = UserPrefs.Snapshot()
            ),
            uncategorizedTitle = "Inbox"
        )

        assertEquals(listOf(2L, 1L, 3L), state.doneItems.map { it.id })
        assertEquals(emptyList<TagGroup>(), state.groups)
    }

    private fun todo(
        id: Long,
        tagId: Long? = null,
        dateMillis: Long? = null,
        createdAtMillis: Long = id,
        done: Boolean = false,
        doneAtMillis: Long? = null
    ): TodoEntity =
        TodoEntity(
            id = id,
            title = "Todo $id",
            date = dateMillis?.let { 20260523 },
            dateMillis = dateMillis,
            tagId = tagId,
            done = done,
            doneAtMillis = doneAtMillis,
            createdAtMillis = createdAtMillis
        )
}

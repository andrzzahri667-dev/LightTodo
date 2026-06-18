package com.zahri.lighttodo.feature.home

import com.zahri.lighttodo.data.HomeData
import com.zahri.lighttodo.data.TodoEntity

fun buildHomeUiState(
    data: HomeData,
    uncategorizedTitle: String
): HomeUiState {
    val (done, undone) = data.todos.partition { it.done }
    val doneItems = done.sortedByDescending { it.doneAtMillis ?: 0L }
    val byTag: Map<Long?, List<TodoEntity>> = undone.groupBy { it.tagId }
    val itemOrder = compareBy<TodoEntity>({ it.dateMillis == null }, { it.createdAtMillis })
    val groups = mutableListOf<TagGroup>()

    data.tags.forEach { tag ->
        val items = byTag[tag.id].orEmpty().sortedWith(itemOrder)
        if (items.isNotEmpty()) groups += TagGroup(tag.id, tag.name, items)
    }

    val uncatItems = byTag[null].orEmpty().sortedWith(itemOrder)
    if (uncatItems.isNotEmpty()) {
        groups += TagGroup(null, uncategorizedTitle, uncatItems)
    }

    return HomeUiState(
        groups = groups,
        doneItems = doneItems,
        collapsedTagIds = data.prefs.collapsedTagIds,
        doneExpanded = data.prefs.doneSectionExpanded
    )
}

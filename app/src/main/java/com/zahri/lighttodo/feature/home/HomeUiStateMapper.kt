package com.zahri.lighttodo.feature.home

import com.zahri.lighttodo.usecase.todo.HomeTodo
import com.zahri.lighttodo.usecase.todo.HomeTodoSnapshot

fun buildHomeUiState(
    data: HomeTodoSnapshot,
    uncategorizedTitle: String
): HomeUiState {
    val (done, undone) = data.todos.partition { it.done }
    val doneItems = done.sortedByDescending { it.doneAtMillis ?: 0L }
    val byTag: Map<Long?, List<HomeTodo>> = undone.groupBy { it.tagId }
    val itemOrder = compareBy<HomeTodo>({ it.dateMillis == null }, { it.createdAtMillis })
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
        collapsedTagIds = data.collapsedTagIds,
        doneExpanded = data.doneSectionExpanded
    )
}

package com.zahri.lighttodo.feature.home

import com.zahri.lighttodo.data.TodoEntity

sealed interface HomeTodoListItem {
    val key: String

    data class Header(
        val sectionKey: String,
        val title: String,
        val count: Int,
        val expanded: Boolean,
        val doneSection: Boolean = false
    ) : HomeTodoListItem {
        override val key: String = if (doneSection) "done-header" else "header-$sectionKey"
    }

    data class TodoRow(
        val todo: TodoEntity,
        val showDivider: Boolean,
        val strikeThrough: Boolean,
        val visible: Boolean
    ) : HomeTodoListItem {
        override val key: String = if (strikeThrough) "done-${todo.id}" else "todo-${todo.id}"
    }
}

fun buildHomeTodoListItems(state: HomeUiState): List<HomeTodoListItem> {
    val items = mutableListOf<HomeTodoListItem>()

    state.groups.forEach { group ->
        val sectionKey = HomeViewModel.groupKey(group.tagId)
        val expanded = sectionKey !in state.collapsedTagIds
        items += HomeTodoListItem.Header(
            sectionKey = sectionKey,
            title = group.name,
            count = group.items.size,
            expanded = expanded
        )
        group.items.forEachIndexed { index, todo ->
            items += HomeTodoListItem.TodoRow(
                todo = todo,
                showDivider = index < group.items.lastIndex,
                strikeThrough = false,
                visible = expanded
            )
        }
    }

    if (state.doneItems.isNotEmpty()) {
        items += HomeTodoListItem.Header(
            sectionKey = "done",
            title = "",
            count = state.doneItems.size,
            expanded = state.doneExpanded,
            doneSection = true
        )
        state.doneItems.forEachIndexed { index, todo ->
            items += HomeTodoListItem.TodoRow(
                todo = todo,
                showDivider = index < state.doneItems.lastIndex,
                strikeThrough = true,
                visible = state.doneExpanded
            )
        }
    }

    return items
}

package com.zahri.lighttodo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahri.lighttodo.App
import com.zahri.lighttodo.R
import com.zahri.lighttodo.data.HomeData
import com.zahri.lighttodo.data.TagEntity
import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TagGroup(
    val tagId: Long?,
    val name: String,
    val items: List<TodoEntity>
)

data class HomeUiState(
    val groups: List<TagGroup>,
    val doneItems: List<TodoEntity>,
    val collapsedTagIds: Set<String>,
    val doneExpanded: Boolean
)

class HomeViewModel : ViewModel() {

    private val app = App.instance
    private val repo = app.repository
    private val prefs = app.prefs

    val state: StateFlow<HomeUiState> =
        repo.homeFlow().map { data -> data.toUiState() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = HomeUiState(emptyList(), emptyList(), emptySet(), false)
            )

    private fun HomeData.toUiState(): HomeUiState {
        val undone = todos.filter { !it.done }
        val done = todos.filter { it.done }.sortedByDescending { it.doneAtMillis ?: 0L }

        // Group by tag
        val byTag: Map<Long?, List<TodoEntity>> = undone.groupBy { it.tagId }
        val tagOrder: List<TagEntity> = tags
        val groups = mutableListOf<TagGroup>()
        for (t in tagOrder) {
            val items = byTag[t.id].orEmpty().sortedBy { it.createdAtMillis }
            if (items.isNotEmpty()) groups += TagGroup(t.id, t.name, items)
        }
        val uncatItems = byTag[null].orEmpty().sortedBy { it.createdAtMillis }
        if (uncatItems.isNotEmpty()) groups += TagGroup(null, app.getString(R.string.home_uncategorized), uncatItems)

        return HomeUiState(
            groups = groups,
            doneItems = done,
            collapsedTagIds = prefs.collapsedTagIds,
            doneExpanded = prefs.doneSectionExpanded
        )
    }

    fun toggleDone(id: Long, done: Boolean) {
        viewModelScope.launch { repo.setDone(id, done) }
    }

    fun setGroupExpanded(key: String, expanded: Boolean) {
        viewModelScope.launch {
            val cur = state.value.collapsedTagIds.toMutableSet()
            if (expanded) cur -= key else cur += key
            prefs.setCollapsedTagIds(cur)
        }
    }

    fun setDoneExpanded(expanded: Boolean) {
        viewModelScope.launch { prefs.setDoneSectionExpanded(expanded) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    // ── Batch selection ──────────────────────────────────────
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    val inSelectionMode: Boolean get() = _selectedIds.value.isNotEmpty()

    fun toggleSelection(id: Long) {
        _selectedIds.value = _selectedIds.value.let {
            if (id in it) it - id else it + id
        }
    }

    fun clearSelection() { _selectedIds.value = emptySet() }

    fun deleteSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repo.delete(it) }
            _selectedIds.value = emptySet()
        }
    }

    companion object {
        fun groupKey(tagId: Long?): String = if (tagId == null) "uncat" else "tag-$tagId"
    }
}

// helper available outside
fun TodoEntity.displayTitle(): String =
    title?.takeIf { it.isNotBlank() }
        ?: note?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: "无标题"

fun TodoEntity.displayTitle(context: android.content.Context): String =
    title?.takeIf { it.isNotBlank() }
        ?: note?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: context.getString(R.string.home_no_title)

fun TodoEntity.dateLabel(): String = DateUtils.displayDate(date)
fun TodoEntity.isOverdueDate(): Boolean = !done && DateUtils.isOverdue(date)

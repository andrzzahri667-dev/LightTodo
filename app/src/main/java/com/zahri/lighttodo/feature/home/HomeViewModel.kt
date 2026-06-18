package com.zahri.lighttodo.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahri.lighttodo.R
import com.zahri.lighttodo.usecase.note.DeleteNoteUseCase
import com.zahri.lighttodo.usecase.note.NoteListItem
import com.zahri.lighttodo.usecase.note.ObserveNotesUseCase
import com.zahri.lighttodo.usecase.todo.CompleteTodoUseCase
import com.zahri.lighttodo.usecase.todo.DeleteTodoUseCase
import com.zahri.lighttodo.usecase.todo.HomeTodo
import com.zahri.lighttodo.usecase.todo.ObserveHomeUseCase
import com.zahri.lighttodo.usecase.todo.UpdateHomePreferencesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TagGroup(
    val tagId: Long?,
    val name: String,
    val items: List<HomeTodo>
)

data class HomeUiState(
    val groups: List<TagGroup>,
    val doneItems: List<HomeTodo>,
    val collapsedTagIds: Set<String>,
    val doneExpanded: Boolean
)

class HomeViewModel(
    context: Context,
    observeHome: ObserveHomeUseCase,
    private val completeTodo: CompleteTodoUseCase,
    private val deleteTodo: DeleteTodoUseCase,
    private val updateHomePreferences: UpdateHomePreferencesUseCase,
    observeNotes: ObserveNotesUseCase,
    private val deleteNote: DeleteNoteUseCase
) : ViewModel() {
    private val appContext: Context = runCatching { context.applicationContext }.getOrNull() ?: context

    val state: StateFlow<HomeUiState> =
        observeHome().distinctUntilChanged().map { data ->
            buildHomeUiState(
                data = data,
                uncategorizedTitle = appContext.getString(R.string.home_uncategorized)
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = HomeUiState(emptyList(), emptyList(), emptySet(), false)
            )

    // ── Notes ────────────────────────────────────────────────
    val notes: StateFlow<List<NoteListItem>> =
        observeNotes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private val _noteSelectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val noteSelectedIds: StateFlow<Set<Long>> = _noteSelectedIds.asStateFlow()

    fun toggleNoteSelection(id: Long) {
        _noteSelectedIds.value = _noteSelectedIds.value.let {
            if (id in it) it - id else it + id
        }
    }

    fun clearNoteSelection() { _noteSelectedIds.value = emptySet() }

    fun deleteSelectedNotes() {
        val ids = _noteSelectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            deleteNote.deleteMany(ids)
            _noteSelectedIds.value = emptySet()
        }
    }

    /**
     * 勾选 → 完成:先把 id 加入 pendingCompleteIds 让 UI 播动画,
     * 200ms 后再真正写库。从已完成区取消勾选(done=false)立刻执行,无动画。
     */
    fun toggleDone(id: Long, done: Boolean) {
        if (!done) {
            viewModelScope.launch { completeTodo(id, false) }
            return
        }
        // 已经在动画中,忽略重复点击
        if (id in _pendingCompleteIds.value) return
        _pendingCompleteIds.value = _pendingCompleteIds.value + id
        viewModelScope.launch {
            kotlinx.coroutines.delay(320)
            completeTodo(id, true)
            _pendingCompleteIds.value = _pendingCompleteIds.value - id
        }
    }

    fun setGroupExpanded(key: String, expanded: Boolean) {
        viewModelScope.launch {
            updateHomePreferences.setGroupExpanded(
                currentCollapsedTagIds = state.value.collapsedTagIds,
                key = key,
                expanded = expanded
            )
        }
    }

    fun setDoneExpanded(expanded: Boolean) {
        viewModelScope.launch { updateHomePreferences.setDoneExpanded(expanded) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { deleteTodo.delete(id) }
    }

    // ── Batch selection ──────────────────────────────────────
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    // ── Complete-animation pending set ───────────────────────
    private val _pendingCompleteIds = MutableStateFlow<Set<Long>>(emptySet())
    val pendingCompleteIds: StateFlow<Set<Long>> = _pendingCompleteIds.asStateFlow()

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
            deleteTodo.deleteMany(ids)
            _selectedIds.value = emptySet()
        }
    }

    companion object {
        fun groupKey(tagId: Long?): String = if (tagId == null) "uncat" else "tag-$tagId"
    }
}

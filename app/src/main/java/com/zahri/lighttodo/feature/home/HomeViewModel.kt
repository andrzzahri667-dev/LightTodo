package com.zahri.lighttodo.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahri.lighttodo.App
import com.zahri.lighttodo.R
import com.zahri.lighttodo.data.NoteEntity
import com.zahri.lighttodo.data.NoteDao
import com.zahri.lighttodo.data.Repository
import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.data.UserPrefs
import com.zahri.lighttodo.ui.note.NoteAttachmentMarkdown
import com.zahri.lighttodo.ui.note.NoteAttachmentStore
import com.zahri.lighttodo.util.DateUtils
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
    val items: List<TodoEntity>
)

data class HomeUiState(
    val groups: List<TagGroup>,
    val doneItems: List<TodoEntity>,
    val collapsedTagIds: Set<String>,
    val doneExpanded: Boolean
)

class HomeViewModel(
    private val appContext: Context = App.instance,
    private val repo: Repository = App.instance.repository,
    private val prefs: UserPrefs = App.instance.prefs,
    private val noteDao: NoteDao = App.instance.db.noteDao()
) : ViewModel() {

    val state: StateFlow<HomeUiState> =
        repo.homeFlow().distinctUntilChanged().map { data ->
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
    val notes: StateFlow<List<NoteEntity>> =
        noteDao.observeAll()
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
            noteDao.findByIds(ids).forEach { note ->
                NoteAttachmentStore.deleteRefs(appContext, NoteAttachmentMarkdown.refsIn(note.content))
            }
            noteDao.deleteByIds(ids)
            val refs = noteDao.listAll()
                .flatMap { NoteAttachmentMarkdown.refsIn(it.content) }
                .toSet()
            NoteAttachmentStore.deleteUnreferenced(appContext, refs)
            _noteSelectedIds.value = emptySet()
        }
    }

    /**
     * 勾选 → 完成:先把 id 加入 pendingCompleteIds 让 UI 播动画,
     * 200ms 后再真正写库。从已完成区取消勾选(done=false)立刻执行,无动画。
     */
    fun toggleDone(id: Long, done: Boolean) {
        if (!done) {
            viewModelScope.launch { repo.setDone(id, false) }
            return
        }
        // 已经在动画中,忽略重复点击
        if (id in _pendingCompleteIds.value) return
        _pendingCompleteIds.value = _pendingCompleteIds.value + id
        viewModelScope.launch {
            kotlinx.coroutines.delay(320)
            repo.setDone(id, true)
            _pendingCompleteIds.value = _pendingCompleteIds.value - id
        }
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
            repo.deleteMany(ids)
            _selectedIds.value = emptySet()
        }
    }

    companion object {
        fun groupKey(tagId: Long?): String = if (tagId == null) "uncat" else "tag-$tagId"
    }
}

fun TodoEntity.displayTitle(fallback: String): String =
    title?.takeIf { it.isNotBlank() }
        ?: note?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: fallback

fun TodoEntity.displayTitle(context: android.content.Context): String =
    displayTitle(fallback = context.getString(R.string.home_no_title))

fun TodoEntity.dateLabel(): String = DateUtils.displayDateOrEmpty(date)
fun TodoEntity.isOverdueDate(): Boolean = !done && DateUtils.isOverdueOrFalse(date)

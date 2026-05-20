package com.zahri.lighttodo.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahri.lighttodo.App
import com.zahri.lighttodo.data.NoteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NoteEditViewModel : ViewModel() {

    private val app = App.instance
    private val noteDao = app.db.noteDao()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _createdAt = MutableStateFlow(0L)
    val createdAt: StateFlow<Long> = _createdAt.asStateFlow()

    private val _updatedAt = MutableStateFlow(0L)
    val updatedAt: StateFlow<Long> = _updatedAt.asStateFlow()

    private var noteId: Long? = null
    private var loaded = false
    private var saveJob: Job? = null
    private var saveAgainAfterCurrentJob = false
    private var lastSavedTitle = ""
    private var lastSavedContent = ""

    fun load(id: Long?) {
        if (loaded) return
        loaded = true
        if (id == null) {
            val now = System.currentTimeMillis()
            _createdAt.value = now
            _updatedAt.value = now
            return
        }
        noteId = id
        viewModelScope.launch {
            val note = noteDao.findById(id) ?: return@launch
            _title.value = note.title.orEmpty()
            _content.value = note.content
            _createdAt.value = note.createdAtMillis
            _updatedAt.value = note.updatedAtMillis
            lastSavedTitle = _title.value.trim()
            lastSavedContent = _content.value
        }
    }

    fun updateTitle(value: String) {
        if (_title.value == value) return
        _title.value = value
        _updatedAt.value = System.currentTimeMillis()
    }

    fun updateContent(value: String) {
        if (_content.value == value) return
        _content.value = value
        _updatedAt.value = System.currentTimeMillis()
    }

    /** 自动保存：有内容时写库 */
    fun save() {
        if (saveJob?.isActive == true) {
            saveAgainAfterCurrentJob = true
            return
        }
        saveJob = viewModelScope.launch {
            do {
                saveAgainAfterCurrentJob = false
                val t = _title.value.trim()
                val c = _content.value
                if (t.isEmpty() && c.isBlank()) return@launch
                if (noteId != null && t == lastSavedTitle && c == lastSavedContent) return@launch

                val now = System.currentTimeMillis()
                val entity = NoteEntity(
                    id = noteId ?: 0L,
                    title = t.ifEmpty { null },
                    content = c,
                    createdAtMillis = _createdAt.value,
                    updatedAtMillis = now
                )
                val newId = noteDao.upsert(entity)
                if (noteId == null) noteId = newId
                lastSavedTitle = t
                lastSavedContent = c
                _updatedAt.value = now
            } while (saveAgainAfterCurrentJob)
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = noteId ?: run { onDone(); return }
        viewModelScope.launch {
            noteDao.delete(id)
            onDone()
        }
    }
}

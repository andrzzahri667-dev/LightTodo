package com.zahri.lighttodo.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahri.lighttodo.App
import com.zahri.lighttodo.data.NoteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        }
    }

    fun updateTitle(value: String) { _title.value = value }
    fun updateContent(value: String) { _content.value = value }

    /** 自动保存：有内容时写库 */
    fun save() {
        val t = _title.value.trim()
        val c = _content.value
        if (t.isEmpty() && c.isBlank()) return

        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val entity = NoteEntity(
                id = noteId ?: 0L,
                title = t.ifEmpty { null },
                content = c,
                createdAtMillis = _createdAt.value,
                updatedAtMillis = now
            )
            val newId = noteDao.upsert(entity)
            if (noteId == null) noteId = newId
            _updatedAt.value = now
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

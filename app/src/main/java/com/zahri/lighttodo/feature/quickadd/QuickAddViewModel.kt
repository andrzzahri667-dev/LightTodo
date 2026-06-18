package com.zahri.lighttodo.feature.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahri.lighttodo.domain.todo.TodoInput
import com.zahri.lighttodo.usecase.todo.SaveTodoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickAddViewModel(
    private val saveTodo: SaveTodoUseCase
) : ViewModel() {
    fun save(text: String, onDone: () -> Unit) {
        if (text.isBlank()) {
            onDone()
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                saveTodo(
                    TodoInput(
                        title = text,
                        note = null
                    )
                )
            }
            onDone()
        }
    }
}

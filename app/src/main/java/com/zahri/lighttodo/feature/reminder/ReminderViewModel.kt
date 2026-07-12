package com.zahri.lighttodo.feature.reminder

import androidx.lifecycle.ViewModel
import com.zahri.lighttodo.usecase.todo.CompleteTodoUseCase
import com.zahri.lighttodo.usecase.todo.LoadReminderDialogUseCase
import com.zahri.lighttodo.usecase.todo.TodoReminderDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class ReminderViewModel(
    private val loadReminderDialog: LoadReminderDialogUseCase,
    private val completeTodo: CompleteTodoUseCase
) : ViewModel() {
    suspend fun load(todoId: Long, fallbackTitle: String): TodoReminderDialog? =
        withContext(Dispatchers.IO) {
            loadReminderDialog(todoId, fallbackTitle)
        }

    suspend fun complete(todoId: Long) {
        withContext(NonCancellable + Dispatchers.IO) {
            completeTodo(todoId, true)
        }
    }
}

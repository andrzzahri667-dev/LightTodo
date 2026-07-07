package com.zahri.lighttodo.feature.todoedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahri.lighttodo.domain.todo.TodoInput
import com.zahri.lighttodo.domain.todo.TodoReminderDefaults
import com.zahri.lighttodo.usecase.todo.DeleteTodoUseCase
import com.zahri.lighttodo.usecase.todo.LoadTodoEditUseCase
import com.zahri.lighttodo.usecase.todo.SaveTodoUseCase
import com.zahri.lighttodo.usecase.todo.TodoEditSnapshot
import com.zahri.lighttodo.usecase.todo.TodoEditTagOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class EditUiState(
    val id: Long? = null,
    val title: String = "",
    val note: String = "",
    /** null 表示"无日期任务"——既无开始也无截止 */
    val date: LocalDate? = null,
    /** 开始时间 Pair(hour, minute) or null */
    val startTime: Pair<Int, Int>? = null,
    /** 截止时间 Pair(hour, minute) or null */
    val endTime: Pair<Int, Int>? = null,
    val customHoursBefore: Int? = null,
    val defaultHoursBefore: Int = 2,
    val defaultRemindLabel: String = "09:00",
    val tagName: String = "",
    val allTags: List<TodoEditTagOption> = emptyList(),
    val readOnly: Boolean = false
) {
    val hasReminder: Boolean
        get() = date != null
}

class EditViewModel(
    private val loadTodoEdit: LoadTodoEditUseCase,
    private val saveTodo: SaveTodoUseCase,
    private val deleteTodo: DeleteTodoUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EditUiState())
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    fun load(id: Long?, initialTitle: String?) {
        viewModelScope.launch {
            _state.value = loadTodoEdit(id, initialTitle)?.toUiState() ?: return@launch
        }
    }

    fun setTitle(v: String) = _state.update { it.copy(title = v) }
    fun setNote(v: String) = _state.update { it.copy(note = v) }
    fun setTagName(v: String) = _state.update { it.copy(tagName = v) }

    /**
     * 启用/关闭日期。
     *  - 启用：若当前没日期，默认填今天；保留现有时间字段（一般也无）
     *  - 关闭：清空日期、时间和自定义提醒。变成"无日期任务"
     */
    fun setDateEnabled(enabled: Boolean) = _state.update { st ->
        if (enabled) {
            if (st.date != null) st
            else st.copy(date = LocalDate.now())
        } else {
            st.copy(date = null, startTime = null, endTime = null, customHoursBefore = null)
        }
    }

    fun setDate(year: Int, month: Int, day: Int) = _state.update {
        it.copy(date = LocalDate.of(year, month, day))
    }

    fun setStartTime(hour: Int, minute: Int) = _state.update { st ->
        val newStart = hour to minute
        val startTotal = hour * 60 + minute
        // 若没有截止时间，或截止时间早于/等于新开始时间，自动设为开始时间 +15 分钟
        val end = st.endTime
        val needAdjustEnd = end == null || (end.first * 60 + end.second) <= startTotal
        val newEnd = if (needAdjustEnd) {
            val total = (startTotal + 15).coerceAtMost(23 * 60 + 59)
            (total / 60) to (total % 60)
        } else {
            end
        }
        st.copy(startTime = newStart, endTime = newEnd)
    }

    fun setEndTime(hour: Int, minute: Int) = _state.update { st ->
        val endTotal = hour * 60 + minute
        // 若已有开始时间，截止不允许早于或等于开始；自动夹到 开始 +15 分钟
        val start = st.startTime
        val adjusted = if (start != null) {
            val startTotal = start.first * 60 + start.second
            if (endTotal <= startTotal) {
                val total = (startTotal + 15).coerceAtMost(23 * 60 + 59)
                (total / 60) to (total % 60)
            } else {
                hour to minute
            }
        } else {
            hour to minute
        }
        st.copy(endTime = adjusted)
    }

    fun clearTimes() = _state.update {
        it.copy(startTime = null, endTime = null, customHoursBefore = null)
    }

    fun adjustHoursBefore(delta: Int) = _state.update {
        val next = TodoReminderDefaults.adjustHoursBefore(
            customHoursBefore = it.customHoursBefore,
            defaultHoursBefore = it.defaultHoursBefore,
            delta = delta
        )
        it.copy(customHoursBefore = next)
    }

    suspend fun save() {
        val s = _state.value
        if (s.readOnly) return
        withContext(NonCancellable + Dispatchers.IO) {
            saveTodo(
                TodoInput(
                    id = s.id,
                    title = s.title,
                    note = s.note,
                    year = s.date?.year,
                    month = s.date?.monthValue,
                    day = s.date?.dayOfMonth,
                    startHour = s.startTime?.first,
                    startMinute = s.startTime?.second,
                    deadlineHour = s.endTime?.first,
                    deadlineMinute = s.endTime?.second,
                    customHoursBefore = s.customHoursBefore,
                    tagName = s.tagName
                )
            )
        }
    }

    suspend fun delete() {
        val id = _state.value.id ?: return
        withContext(NonCancellable + Dispatchers.IO) { deleteTodo.delete(id) }
    }
}

private fun TodoEditSnapshot.toUiState(): EditUiState =
    EditUiState(
        id = id,
        title = title,
        note = note,
        date = date,
        startTime = startTime,
        endTime = endTime,
        customHoursBefore = customHoursBefore,
        defaultHoursBefore = defaultHoursBefore,
        defaultRemindLabel = defaultRemindLabel,
        tagName = tagName,
        allTags = allTags,
        readOnly = readOnly
    )

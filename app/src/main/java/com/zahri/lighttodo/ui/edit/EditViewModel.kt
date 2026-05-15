package com.zahri.lighttodo.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahri.lighttodo.App
import com.zahri.lighttodo.data.TagEntity
import com.zahri.lighttodo.data.TodoInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditUiState(
    val id: Long? = null,
    val title: String = "",
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    /** 开始时间 Pair(hour, minute) or null */
    val startTime: Pair<Int, Int>? = null,
    /** 截止时间 Pair(hour, minute) or null */
    val endTime: Pair<Int, Int>? = null,
    val customHoursBefore: Int? = null,
    val defaultHoursBefore: Int = 2,
    val defaultRemindLabel: String = "09:00",
    val tagName: String = "",
    val allTags: List<TagEntity> = emptyList(),
    val readOnly: Boolean = false
)

class EditViewModel : ViewModel() {
    private val app = App.instance
    private val repo = app.repository
    private val prefs = app.prefs
    private val tagDao = app.db.tagDao()
    private val todoDao = app.db.todoDao()

    private val _state = MutableStateFlow(EditUiState())
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    fun load(id: Long?, initialTitle: String?) {
        viewModelScope.launch {
            val p = prefs.flow.first()
            val tags = tagDao.listAll()
            val label = "%02d:%02d".format(p.defaultRemindHour, p.defaultRemindMinute)
            if (id != null) {
                val t = todoDao.findById(id) ?: return@launch
                val tag = t.tagId?.let { tags.firstOrNull { tg -> tg.id == it } }
                _state.value = EditUiState(
                    id = t.id,
                    title = t.title.orEmpty(),
                    note = t.note.orEmpty(),
                    date = LocalDate.of(t.date / 10000, (t.date / 100) % 100, t.date % 100),
                    startTime = if (t.startHour != null && t.startMinute != null)
                        t.startHour to t.startMinute else null,
                    endTime = if (t.deadlineHour != null && t.deadlineMinute != null)
                        t.deadlineHour to t.deadlineMinute else null,
                    customHoursBefore = t.customRemindHoursBefore,
                    defaultHoursBefore = p.defaultHoursBefore,
                    defaultRemindLabel = label,
                    tagName = tag?.name.orEmpty(),
                    allTags = tags,
                    readOnly = t.calendarEventId != null
                )
            } else {
                _state.value = EditUiState(
                    id = null,
                    title = initialTitle.orEmpty(),
                    defaultHoursBefore = p.defaultHoursBefore,
                    defaultRemindLabel = label,
                    allTags = tags
                )
            }
        }
    }

    fun setTitle(v: String) = _state.update { it.copy(title = v) }
    fun setNote(v: String) = _state.update { it.copy(note = v) }
    fun setTagName(v: String) = _state.update { it.copy(tagName = v) }

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

    fun setEndTime(hour: Int, minute: Int) = _state.update {
        it.copy(endTime = hour to minute)
    }

    fun clearTimes() = _state.update {
        it.copy(startTime = null, endTime = null, customHoursBefore = null)
    }

    fun adjustHoursBefore(delta: Int) = _state.update {
        val cur = it.customHoursBefore ?: it.defaultHoursBefore
        val next = (cur + delta).coerceIn(0, 72)
        it.copy(customHoursBefore = next)
    }

    fun save() {
        val s = _state.value
        if (s.readOnly) return
        viewModelScope.launch {
            repo.saveTodo(
                TodoInput(
                    id = s.id,
                    title = s.title,
                    note = s.note,
                    year = s.date.year,
                    month = s.date.monthValue,
                    day = s.date.dayOfMonth,
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

    fun delete() {
        val id = _state.value.id ?: return
        viewModelScope.launch { repo.delete(id) }
    }
}

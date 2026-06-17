package com.zahri.lighttodo.domain.todo

import com.zahri.lighttodo.util.DateUtils

object TodoDisplayText {
    fun title(title: String?, note: String?, fallback: String): String =
        title?.takeIf { it.isNotBlank() }
            ?: note?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: fallback

    fun dateLabel(date: Int?): String = DateUtils.displayDateOrEmpty(date)

    fun isOverdueDate(done: Boolean, date: Int?): Boolean =
        !done && DateUtils.isOverdueOrFalse(date)
}

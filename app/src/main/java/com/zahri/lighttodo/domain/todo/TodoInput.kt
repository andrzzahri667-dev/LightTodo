package com.zahri.lighttodo.domain.todo

data class TodoInput(
    val id: Long? = null,
    val title: String?,
    val note: String?,
    /** year/month/day 三者要么同时非空（有日期任务），要么同时为 null（无日期任务） */
    val year: Int? = null,
    val month: Int? = null, // 1-12
    val day: Int? = null,   // 1-31
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val deadlineHour: Int? = null,
    val deadlineMinute: Int? = null,
    val customHoursBefore: Int? = null,
    val tagName: String? = null
)

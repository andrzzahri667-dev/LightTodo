package com.zahri.lighttodo.domain.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupBundle(
    val version: Int = 2,
    val tags: List<BackupTag>,
    val todos: List<BackupTodo>,
    val notes: List<BackupNote> = emptyList()
)

@Serializable
data class BackupTag(val id: Long, val name: String, val sortOrder: Int)

@Serializable
data class BackupTodo(
    val id: Long,
    val title: String?,
    val note: String?,
    val date: Int? = null,
    val dateMillis: Long? = null,
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val deadlineHour: Int?,
    val deadlineMinute: Int?,
    val remindStartAtMillis: Long? = null,
    val remindAtMillis: Long?,
    val customRemindHoursBefore: Int?,
    val tagId: Long?,
    val done: Boolean,
    val doneAtMillis: Long?,
    val createdAtMillis: Long,
    val calendarCreatedByApp: Boolean = false
)

@Serializable
data class BackupNote(
    val id: Long,
    val title: String?,
    val content: String,
    val tagId: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

package com.zahri.lighttodo.feature.noteeditor

data class NoteEditLaunchSeed(
    val id: Long,
    val title: String?,
    val content: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

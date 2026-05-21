package com.zahri.lighttodo.data

internal data class BackupEntities(
    val tags: List<TagEntity>,
    val todos: List<TodoEntity>,
    val notes: List<NoteEntity>
)

internal object BackupDtoMapper {
    fun buildBundle(
        tags: List<TagEntity>,
        todos: List<TodoEntity>,
        notes: List<NoteEntity>
    ) = BackupBundle(
        version = 2,
        tags = tags.map { it.toBackupTag() },
        todos = todos.map { it.toBackupTodo() },
        notes = notes.map { it.toBackupNote() }
    )

    fun toEntities(bundle: BackupBundle) = BackupEntities(
        tags = bundle.tags.map { it.toEntity() },
        todos = bundle.todos.map { it.toEntity() },
        notes = bundle.notes.map { it.toEntity() }
    )
}

private fun TagEntity.toBackupTag() = BackupTag(
    id = id,
    name = name,
    sortOrder = sortOrder
)

private fun BackupTag.toEntity() = TagEntity(
    id = id,
    name = name,
    sortOrder = sortOrder
)

private fun TodoEntity.toBackupTodo() = BackupTodo(
    id = id,
    title = title,
    note = note,
    date = date,
    dateMillis = dateMillis,
    startHour = startHour,
    startMinute = startMinute,
    deadlineHour = deadlineHour,
    deadlineMinute = deadlineMinute,
    remindStartAtMillis = remindStartAtMillis,
    remindAtMillis = remindAtMillis,
    customRemindHoursBefore = customRemindHoursBefore,
    tagId = tagId,
    done = done,
    doneAtMillis = doneAtMillis,
    createdAtMillis = createdAtMillis
)

private fun BackupTodo.toEntity() = TodoEntity(
    id = id,
    title = title,
    note = note,
    date = date,
    dateMillis = dateMillis,
    startHour = startHour,
    startMinute = startMinute,
    deadlineHour = deadlineHour,
    deadlineMinute = deadlineMinute,
    remindStartAtMillis = remindStartAtMillis,
    remindAtMillis = remindAtMillis,
    customRemindHoursBefore = customRemindHoursBefore,
    tagId = tagId,
    done = done,
    doneAtMillis = doneAtMillis,
    createdAtMillis = createdAtMillis,
    calendarEventId = null
)

private fun NoteEntity.toBackupNote() = BackupNote(
    id = id,
    title = title,
    content = content,
    tagId = tagId,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

private fun BackupNote.toEntity() = NoteEntity(
    id = id,
    title = title,
    content = content,
    tagId = tagId,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

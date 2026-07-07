package com.zahri.lighttodo.data.backup

import com.zahri.lighttodo.data.local.NoteEntity
import com.zahri.lighttodo.data.local.TagEntity
import com.zahri.lighttodo.data.local.TodoEntity
import com.zahri.lighttodo.domain.backup.BackupBundle
import com.zahri.lighttodo.domain.backup.BackupNote
import com.zahri.lighttodo.domain.backup.BackupTag
import com.zahri.lighttodo.domain.backup.BackupTodo

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

    fun toEntities(bundle: BackupBundle): BackupEntities {
        val validTagIds = bundle.validTagIds()
        return BackupEntities(
            tags = bundle.tags.map { it.toEntity() },
            todos = bundle.todos.map { it.toEntity(validTagIds) },
            notes = bundle.notes.map { it.toEntity(validTagIds) }
        )
    }
}

private fun BackupBundle.validTagIds(): Set<Long> = tags.map { it.id }.toSet()

private fun Long?.takeIfValidTag(validTagIds: Set<Long>): Long? =
    takeIf { it in validTagIds }

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
    createdAtMillis = createdAtMillis,
    calendarCreatedByApp = calendarCreatedByApp
)

private fun BackupTodo.toEntity(validTagIds: Set<Long>) = TodoEntity(
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
    tagId = tagId.takeIfValidTag(validTagIds),
    done = done,
    doneAtMillis = doneAtMillis,
    createdAtMillis = createdAtMillis,
    calendarEventId = null,
    calendarCreatedByApp = calendarCreatedByApp
)

private fun NoteEntity.toBackupNote() = BackupNote(
    id = id,
    title = title,
    content = content,
    tagId = tagId,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

private fun BackupNote.toEntity(validTagIds: Set<Long>) = NoteEntity(
    id = id,
    title = title,
    content = content,
    tagId = tagId.takeIfValidTag(validTagIds),
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

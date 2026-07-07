package com.zahri.lighttodo.data.backup

import com.zahri.lighttodo.data.local.NoteEntity
import com.zahri.lighttodo.data.local.TagEntity
import com.zahri.lighttodo.domain.backup.BackupBundle
import com.zahri.lighttodo.domain.backup.BackupNote
import com.zahri.lighttodo.domain.backup.BackupTag
import com.zahri.lighttodo.domain.backup.BackupTodo
import com.zahri.lighttodo.test.sourceFile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDtoMapperTest {

    @Test
    fun buildBundle_includesNotesWithVersion2() {
        val note = NoteEntity(
            id = 7L,
            title = "Meeting",
            content = "![image](lighttodo://attachment/image/a.jpg)\nnotes",
            tagId = 3L,
            createdAtMillis = 100L,
            updatedAtMillis = 200L
        )

        val bundle = BackupDtoMapper.buildBundle(
            tags = listOf(TagEntity(id = 3L, name = "Work", sortOrder = 1)),
            todos = emptyList(),
            notes = listOf(note)
        )

        assertEquals(2, bundle.version)
        assertEquals(1, bundle.notes.size)
        assertEquals("Meeting", bundle.notes.single().title)
        assertEquals(note.content, bundle.notes.single().content)
    }

    @Test
    fun toEntities_restoresNotesFromBundle() {
        val bundle = BackupBundle(
            version = 2,
            tags = emptyList(),
            todos = emptyList(),
            notes = listOf(
                BackupNote(
                    id = 9L,
                    title = "Restored",
                    content = "[audio 00:03](lighttodo://attachment/audio/a.m4a)",
                    tagId = null,
                    createdAtMillis = 300L,
                    updatedAtMillis = 400L
                )
            )
        )

        val entities = BackupDtoMapper.toEntities(bundle)

        assertEquals(1, entities.notes.size)
        assertEquals(9L, entities.notes.single().id)
        assertEquals("Restored", entities.notes.single().title)
        assertEquals(bundle.notes.single().content, entities.notes.single().content)
    }

    @Test
    fun toEntities_clearsTagReferencesMissingFromImportedTags() {
        val bundle = BackupBundle(
            version = 2,
            tags = listOf(BackupTag(id = 1L, name = "Work", sortOrder = 0)),
            todos = listOf(
                BackupTodo(
                    id = 10L,
                    title = "Foreign todo",
                    note = null,
                    deadlineHour = null,
                    deadlineMinute = null,
                    remindAtMillis = null,
                    customRemindHoursBefore = null,
                    tagId = 99L,
                    done = false,
                    doneAtMillis = null,
                    createdAtMillis = 100L
                )
            ),
            notes = listOf(
                BackupNote(
                    id = 11L,
                    title = "Foreign note",
                    content = "body",
                    tagId = 100L,
                    createdAtMillis = 200L,
                    updatedAtMillis = 300L
                )
            )
        )

        val entities = BackupDtoMapper.toEntities(bundle)

        assertEquals(null, entities.todos.single().tagId)
        assertEquals(null, entities.notes.single().tagId)
    }

    @Test
    fun backupTodoDtoIncludesCalendarCreatedByAppFlag() {
        val entitySource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/local/Entities.kt")
            .readText()
        val mapperSource = sourceFile("app/src/main/java/com/zahri/lighttodo/data/backup/BackupDtoMapper.kt")
            .readText()

        assertTrue(entitySource.contains("val calendarCreatedByApp: Boolean = false"))
        assertTrue(mapperSource.contains("calendarCreatedByApp = calendarCreatedByApp"))
    }
}

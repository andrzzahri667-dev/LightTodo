package com.zahri.lighttodo.data.note

import com.zahri.lighttodo.data.local.NoteDao
import com.zahri.lighttodo.data.local.NoteEntity
import com.zahri.lighttodo.domain.note.NoteAttachmentMarkdown
import com.zahri.lighttodo.usecase.note.NoteAttachmentGateway
import com.zahri.lighttodo.usecase.note.NoteEditorSnapshot
import com.zahri.lighttodo.usecase.note.NoteListItem
import com.zahri.lighttodo.usecase.note.NoteRepository
import com.zahri.lighttodo.usecase.note.SaveNoteInput
import com.zahri.lighttodo.usecase.note.SavedNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val attachmentGateway: NoteAttachmentGateway
) : NoteRepository {
    override fun observeNotes(): Flow<List<NoteListItem>> =
        noteDao.observeAll().map { notes -> notes.map { it.toListItem() } }

    override suspend fun loadEditorSnapshot(id: Long): NoteEditorSnapshot? =
        noteDao.findById(id)?.toEditorSnapshot()

    override suspend fun save(input: SaveNoteInput): SavedNote {
        val now = System.currentTimeMillis()
        val entity = NoteEntity(
            id = input.id ?: 0L,
            title = input.title?.takeIf { it.isNotBlank() },
            content = input.content,
            createdAtMillis = input.createdAtMillis,
            updatedAtMillis = now
        )
        val generatedId = noteDao.upsert(entity)
        cleanupUnreferencedAttachments()
        return SavedNote(
            id = input.id ?: generatedId,
            title = entity.title,
            content = entity.content,
            updatedAtMillis = now
        )
    }

    override suspend fun delete(id: Long, fallbackContent: String) {
        noteDao.delete(id)
        cleanupUnreferencedAttachments()
    }

    override suspend fun deleteMany(ids: List<Long>) {
        if (ids.isEmpty()) return
        noteDao.deleteByIds(ids)
        cleanupUnreferencedAttachments()
    }

    private suspend fun cleanupUnreferencedAttachments() {
        val refs = noteDao.listAll()
            .flatMap { NoteAttachmentMarkdown.refsIn(it.content) }
            .toSet()
        attachmentGateway.deleteUnreferenced(refs)
    }

    private fun NoteEntity.toEditorSnapshot(): NoteEditorSnapshot =
        NoteEditorSnapshot(
            id = id,
            title = title,
            content = content,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis
        )

    private fun NoteEntity.toListItem(): NoteListItem =
        NoteListItem(
            id = id,
            title = title,
            content = content,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis
        )
}

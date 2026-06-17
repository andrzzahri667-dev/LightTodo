package com.zahri.lighttodo.data.note

import android.content.Context
import android.net.Uri
import com.zahri.lighttodo.data.local.NoteDao
import com.zahri.lighttodo.data.local.NoteEntity
import com.zahri.lighttodo.domain.note.NoteAttachmentMarkdown
import com.zahri.lighttodo.usecase.note.NoteEditorSnapshot
import com.zahri.lighttodo.usecase.note.NoteListItem
import com.zahri.lighttodo.usecase.note.NoteRepository
import com.zahri.lighttodo.usecase.note.SaveNoteInput
import com.zahri.lighttodo.usecase.note.SavedNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class NoteRepositoryImpl(
    context: Context,
    private val noteDao: NoteDao
) : NoteRepository {
    private val appContext: Context = context.applicationContext ?: context

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
        NoteAttachmentStore.deleteRemovedRefs(appContext, input.previousContent, input.content)
        return SavedNote(
            id = input.id ?: generatedId,
            title = entity.title,
            content = entity.content,
            updatedAtMillis = now
        )
    }

    override suspend fun delete(id: Long, fallbackContent: String) {
        val content = noteDao.findById(id)?.content ?: fallbackContent
        NoteAttachmentStore.deleteRefs(appContext, NoteAttachmentMarkdown.refsIn(content))
        noteDao.delete(id)
        cleanupUnreferencedAttachments()
    }

    override suspend fun deleteMany(ids: List<Long>) {
        if (ids.isEmpty()) return
        noteDao.findByIds(ids).forEach { note ->
            NoteAttachmentStore.deleteRefs(appContext, NoteAttachmentMarkdown.refsIn(note.content))
        }
        noteDao.deleteByIds(ids)
        cleanupUnreferencedAttachments()
    }

    override fun createAudioFile(): File =
        NoteAttachmentStore.createAudioFile(appContext)

    override fun createImageFile(): File =
        NoteAttachmentStore.createImageFile(appContext)

    override fun fileProviderUri(file: File): Uri =
        NoteAttachmentStore.fileProviderUri(appContext, file)

    override fun copyImageFromUri(uri: Uri): File =
        NoteAttachmentStore.copyImageFromUri(appContext, uri)

    override fun imageRef(file: File): String =
        NoteAttachmentStore.imageRef(file)

    override fun audioRef(file: File): String =
        NoteAttachmentStore.audioRef(file)

    override fun resolveAttachment(ref: String): File? =
        NoteAttachmentStore.resolve(appContext, ref)

    private suspend fun cleanupUnreferencedAttachments() {
        val refs = noteDao.listAll()
            .flatMap { NoteAttachmentMarkdown.refsIn(it.content) }
            .toSet()
        NoteAttachmentStore.deleteUnreferenced(appContext, refs)
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

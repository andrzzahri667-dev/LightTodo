package com.zahri.lighttodo.data.note

import android.net.Uri
import com.zahri.lighttodo.data.local.NoteDao
import com.zahri.lighttodo.data.local.NoteEntity
import com.zahri.lighttodo.domain.note.NoteAttachmentMarkdown
import com.zahri.lighttodo.usecase.note.NoteAttachmentGateway
import com.zahri.lighttodo.usecase.note.SaveNoteInput
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteRepositoryAttachmentSafetyTest {
    @Test
    fun saveDoesNotDeleteAttachmentStillReferencedByAnotherNote() = runBlocking {
        val dao = FakeNoteDao(note(1L, SharedImage), note(2L, SharedImage))
        val attachments = RecordingAttachmentGateway()
        val repository = NoteRepositoryImpl(dao, attachments)

        repository.save(
            SaveNoteInput(
                id = 1L,
                title = null,
                content = "",
                createdAtMillis = 1L,
                previousContent = SharedImage
            )
        )

        assertEquals(emptyList<String>(), attachments.directlyDeletedRefs)
        assertEquals(setOf(SharedRef), attachments.lastReferencedRefs)
    }

    @Test
    fun deleteDoesNotDeleteAttachmentStillReferencedByAnotherNote() = runBlocking {
        val dao = FakeNoteDao(note(1L, SharedImage), note(2L, SharedImage))
        val attachments = RecordingAttachmentGateway()
        val repository = NoteRepositoryImpl(dao, attachments)

        repository.delete(1L, fallbackContent = SharedImage)

        assertEquals(emptyList<String>(), attachments.directlyDeletedRefs)
        assertEquals(setOf(SharedRef), attachments.lastReferencedRefs)
    }

    @Test
    fun deleteManyDoesNotDeleteAttachmentStillReferencedByKeptNote() = runBlocking {
        val dao = FakeNoteDao(note(1L, SharedImage), note(2L, SharedImage))
        val attachments = RecordingAttachmentGateway()
        val repository = NoteRepositoryImpl(dao, attachments)

        repository.deleteMany(listOf(1L))

        assertEquals(emptyList<String>(), attachments.directlyDeletedRefs)
        assertEquals(setOf(SharedRef), attachments.lastReferencedRefs)
    }

    @Test
    fun deleteRemovesUniqueAttachmentThroughFullReferenceSweep() = runBlocking {
        val dao = FakeNoteDao(note(1L, SharedImage))
        val attachments = RecordingAttachmentGateway()
        val repository = NoteRepositoryImpl(dao, attachments)

        repository.delete(1L, fallbackContent = SharedImage)

        assertEquals(emptySet<String>(), attachments.lastReferencedRefs)
    }

    private fun note(id: Long, content: String): NoteEntity =
        NoteEntity(id = id, content = content, createdAtMillis = 1L, updatedAtMillis = 1L)

    private companion object {
        const val SharedRef = "lighttodo://attachment/image/shared.jpg"
        val SharedImage = NoteAttachmentMarkdown.image(SharedRef)
    }
}

private class FakeNoteDao(vararg initial: NoteEntity) : NoteDao {
    private val notes = initial.associateByTo(linkedMapOf()) { it.id }
    private var nextId = (notes.keys.maxOrNull() ?: 0L) + 1L

    override fun observeAll(): Flow<List<NoteEntity>> = flowOf(notes.values.toList())
    override suspend fun listAll(): List<NoteEntity> = notes.values.toList()
    override suspend fun findById(id: Long): NoteEntity? = notes[id]
    override suspend fun findByIds(ids: List<Long>): List<NoteEntity> = ids.mapNotNull(notes::get)

    override suspend fun upsert(note: NoteEntity): Long {
        val id = note.id.takeIf { it != 0L } ?: nextId++
        notes[id] = note.copy(id = id)
        return id
    }

    override suspend fun upsertAll(notes: List<NoteEntity>) {
        notes.forEach { upsert(it) }
    }

    override suspend fun delete(id: Long) {
        notes.remove(id)
    }

    override suspend fun deleteByIds(ids: List<Long>) {
        ids.forEach(notes::remove)
    }

    override suspend fun deleteAll() {
        notes.clear()
    }
}

private class RecordingAttachmentGateway : NoteAttachmentGateway {
    val directlyDeletedRefs = mutableListOf<String>()
    var lastReferencedRefs: Set<String>? = null

    override fun deleteRefs(refs: Iterable<String>) {
        directlyDeletedRefs += refs
    }

    override fun deleteRemovedRefs(previousContent: String, currentContent: String) {
        directlyDeletedRefs += NoteAttachmentMarkdown.removedRefs(previousContent, currentContent)
    }

    override fun deleteUnreferenced(referencedRefs: Set<String>) {
        lastReferencedRefs = referencedRefs
    }

    override fun createImageFile(): File = error("unused")
    override fun createAudioFile(): File = error("unused")
    override fun fileProviderUri(file: File): Uri = error("unused")
    override fun copyImageFromUri(uri: Uri): File = error("unused")
    override fun imageRef(file: File): String = error("unused")
    override fun audioRef(file: File): String = error("unused")
    override fun resolveAttachment(ref: String): File? = error("unused")
    override fun importAttachment(
        kind: NoteAttachmentMarkdown.Kind,
        fileName: String,
        input: InputStream
    ): String = error("unused")
}

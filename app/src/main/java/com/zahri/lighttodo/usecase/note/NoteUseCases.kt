package com.zahri.lighttodo.usecase.note

import android.net.Uri
import com.zahri.lighttodo.domain.note.NoteAttachmentMarkdown
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.InputStream

interface NoteRepository {
    fun observeNotes(): Flow<List<NoteListItem>>
    suspend fun loadEditorSnapshot(id: Long): NoteEditorSnapshot?
    suspend fun save(input: SaveNoteInput): SavedNote
    suspend fun delete(id: Long, fallbackContent: String)
    suspend fun deleteMany(ids: List<Long>)
}

interface NoteAttachmentGateway {
    fun createImageFile(): File
    fun createAudioFile(): File
    fun fileProviderUri(file: File): Uri
    fun copyImageFromUri(uri: Uri): File
    fun imageRef(file: File): String
    fun audioRef(file: File): String
    fun resolveAttachment(ref: String): File?
    fun importAttachment(kind: NoteAttachmentMarkdown.Kind, fileName: String, input: InputStream): String
    fun deleteRefs(refs: Iterable<String>)
    fun deleteRemovedRefs(previousContent: String, currentContent: String)
    fun deleteUnreferenced(referencedRefs: Set<String>)
}

data class NoteListItem(
    val id: Long,
    val title: String?,
    val content: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class NoteEditorSnapshot(
    val id: Long,
    val title: String?,
    val content: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class SaveNoteInput(
    val id: Long?,
    val title: String?,
    val content: String,
    val createdAtMillis: Long,
    val previousContent: String
)

data class SavedNote(
    val id: Long,
    val title: String?,
    val content: String,
    val updatedAtMillis: Long
)

class ObserveNotesUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<List<NoteListItem>> = repository.observeNotes()
}

class LoadNoteUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(id: Long): NoteEditorSnapshot? =
        repository.loadEditorSnapshot(id)
}

class SaveNoteUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(input: SaveNoteInput): SavedNote =
        repository.save(input)
}

class DeleteNoteUseCase(
    private val repository: NoteRepository
) {
    suspend fun delete(id: Long, fallbackContent: String) {
        repository.delete(id, fallbackContent)
    }

    suspend fun deleteMany(ids: List<Long>) {
        repository.deleteMany(ids)
    }
}

class CreateNoteAudioFileUseCase(
    private val attachmentGateway: NoteAttachmentGateway
) {
    operator fun invoke(): File = attachmentGateway.createAudioFile()
}

class CreateNoteImageFileUseCase(
    private val attachmentGateway: NoteAttachmentGateway
) {
    operator fun invoke(): File = attachmentGateway.createImageFile()
}

class NoteFileProviderUriUseCase(
    private val attachmentGateway: NoteAttachmentGateway
) {
    operator fun invoke(file: File): Uri = attachmentGateway.fileProviderUri(file)
}

class CopyNoteImageFromUriUseCase(
    private val attachmentGateway: NoteAttachmentGateway
) {
    operator fun invoke(uri: Uri): File = attachmentGateway.copyImageFromUri(uri)
}

class NoteImageRefUseCase(
    private val attachmentGateway: NoteAttachmentGateway
) {
    operator fun invoke(file: File): String = attachmentGateway.imageRef(file)
}

class NoteAudioRefUseCase(
    private val attachmentGateway: NoteAttachmentGateway
) {
    operator fun invoke(file: File): String = attachmentGateway.audioRef(file)
}

class ResolveNoteAttachmentUseCase(
    private val attachmentGateway: NoteAttachmentGateway
) {
    operator fun invoke(ref: String): File? = attachmentGateway.resolveAttachment(ref)
}

data class NoteUseCases(
    val observeNotes: ObserveNotesUseCase,
    val loadNote: LoadNoteUseCase,
    val saveNote: SaveNoteUseCase,
    val deleteNote: DeleteNoteUseCase,
    val createImageFile: CreateNoteImageFileUseCase,
    val createAudioFile: CreateNoteAudioFileUseCase,
    val fileProviderUri: NoteFileProviderUriUseCase,
    val copyImageFromUri: CopyNoteImageFromUriUseCase,
    val imageRef: NoteImageRefUseCase,
    val audioRef: NoteAudioRefUseCase,
    val resolveAttachment: ResolveNoteAttachmentUseCase
)

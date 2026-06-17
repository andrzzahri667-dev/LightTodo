package com.zahri.lighttodo.usecase.note

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import java.io.File

interface NoteRepository {
    fun observeNotes(): Flow<List<NoteListItem>>
    suspend fun loadEditorSnapshot(id: Long): NoteEditorSnapshot?
    suspend fun save(input: SaveNoteInput): SavedNote
    suspend fun delete(id: Long, fallbackContent: String)
    suspend fun deleteMany(ids: List<Long>)
    fun createImageFile(): File
    fun createAudioFile(): File
    fun fileProviderUri(file: File): Uri
    fun copyImageFromUri(uri: Uri): File
    fun imageRef(file: File): String
    fun audioRef(file: File): String
    fun resolveAttachment(ref: String): File?
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
    private val repository: NoteRepository
) {
    operator fun invoke(): File = repository.createAudioFile()
}

class CreateNoteImageFileUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(): File = repository.createImageFile()
}

class NoteFileProviderUriUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(file: File): Uri = repository.fileProviderUri(file)
}

class CopyNoteImageFromUriUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(uri: Uri): File = repository.copyImageFromUri(uri)
}

class NoteImageRefUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(file: File): String = repository.imageRef(file)
}

class NoteAudioRefUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(file: File): String = repository.audioRef(file)
}

class ResolveNoteAttachmentUseCase(
    private val repository: NoteRepository
) {
    operator fun invoke(ref: String): File? = repository.resolveAttachment(ref)
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

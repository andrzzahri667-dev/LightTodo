package com.zahri.lighttodo.feature.noteeditor

import android.net.Uri
import com.zahri.lighttodo.usecase.note.CopyNoteImageFromUriUseCase
import com.zahri.lighttodo.usecase.note.CreateNoteAudioFileUseCase
import com.zahri.lighttodo.usecase.note.CreateNoteImageFileUseCase
import com.zahri.lighttodo.usecase.note.DeleteNoteUseCase
import com.zahri.lighttodo.usecase.note.LoadNoteUseCase
import com.zahri.lighttodo.usecase.note.NoteAudioRefUseCase
import com.zahri.lighttodo.usecase.note.NoteEditorSnapshot
import com.zahri.lighttodo.usecase.note.NoteFileProviderUriUseCase
import com.zahri.lighttodo.usecase.note.NoteImageRefUseCase
import com.zahri.lighttodo.usecase.note.NoteListItem
import com.zahri.lighttodo.usecase.note.NoteRepository
import com.zahri.lighttodo.usecase.note.NoteUseCases
import com.zahri.lighttodo.usecase.note.ObserveNotesUseCase
import com.zahri.lighttodo.usecase.note.ResolveNoteAttachmentUseCase
import com.zahri.lighttodo.usecase.note.SaveNoteInput
import com.zahri.lighttodo.usecase.note.SaveNoteUseCase
import com.zahri.lighttodo.usecase.note.SavedNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteEditViewModelLaunchSeedTest {
    @Test
    fun load_matchingLaunchSeedPublishesFirstFrameStateSynchronouslyWithoutRoomRead() {
        val noteRepository = CountingNoteRepository()
        val vm = NoteEditViewModel(noteUseCases = noteUseCases(noteRepository))
        val seed = NoteEditLaunchSeed(
            id = 7L,
            title = "Seed title",
            content = "Seed content",
            createdAtMillis = 1_000L,
            updatedAtMillis = 2_000L
        )

        vm.load(id = 7L, launchSeed = seed)

        assertEquals("Seed title", vm.title.value)
        assertEquals("Seed content", vm.content.value)
        assertEquals(1_000L, vm.createdAt.value)
        assertEquals(2_000L, vm.updatedAt.value)
        assertEquals(0, noteRepository.loadCalls)
    }

    @Test
    fun editingLoadedNoteDoesNotMutateUpdatedAtUntilSave() {
        val vm = NoteEditViewModel(noteUseCases = noteUseCases(CountingNoteRepository()))
        val seed = NoteEditLaunchSeed(
            id = 7L,
            title = "Seed title",
            content = "Seed content",
            createdAtMillis = 1_000L,
            updatedAtMillis = 2_000L
        )

        vm.load(id = 7L, launchSeed = seed)
        vm.updateTitle("Changed title")
        vm.updateContent("Changed content")

        assertEquals(2_000L, vm.updatedAt.value)
    }
}

private fun noteUseCases(repository: NoteRepository): NoteUseCases =
    NoteUseCases(
        observeNotes = ObserveNotesUseCase(repository),
        loadNote = LoadNoteUseCase(repository),
        saveNote = SaveNoteUseCase(repository),
        deleteNote = DeleteNoteUseCase(repository),
        createImageFile = CreateNoteImageFileUseCase(repository),
        createAudioFile = CreateNoteAudioFileUseCase(repository),
        fileProviderUri = NoteFileProviderUriUseCase(repository),
        copyImageFromUri = CopyNoteImageFromUriUseCase(repository),
        imageRef = NoteImageRefUseCase(repository),
        audioRef = NoteAudioRefUseCase(repository),
        resolveAttachment = ResolveNoteAttachmentUseCase(repository)
    )

private class CountingNoteRepository : NoteRepository {
    var loadCalls = 0

    override fun observeNotes(): Flow<List<NoteListItem>> = emptyFlow()
    override suspend fun loadEditorSnapshot(id: Long): NoteEditorSnapshot? {
        loadCalls++
        return null
    }
    override suspend fun save(input: SaveNoteInput): SavedNote =
        SavedNote(
            id = input.id ?: 1L,
            title = input.title,
            content = input.content,
            updatedAtMillis = 1L
        )
    override suspend fun delete(id: Long, fallbackContent: String) = Unit
    override suspend fun deleteMany(ids: List<Long>) = Unit
    override fun createImageFile(): File = File("unused-image")
    override fun createAudioFile(): File = File("unused")
    override fun fileProviderUri(file: File): Uri = Uri.EMPTY
    override fun copyImageFromUri(uri: Uri): File = File("unused-copy")
    override fun imageRef(file: File): String = "lighttodo://attachment/image/${file.name}"
    override fun audioRef(file: File): String = "lighttodo://attachment/audio/${file.name}"
    override fun resolveAttachment(ref: String): File? = null
}

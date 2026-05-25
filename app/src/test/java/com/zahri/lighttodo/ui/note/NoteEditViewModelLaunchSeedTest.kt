package com.zahri.lighttodo.ui.note

import com.zahri.lighttodo.App
import com.zahri.lighttodo.data.NoteDao
import com.zahri.lighttodo.data.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteEditViewModelLaunchSeedTest {
    @Test
    fun load_matchingLaunchSeedPublishesFirstFrameStateSynchronouslyWithoutRoomRead() {
        val noteDao = CountingNoteDao()
        val vm = NoteEditViewModel(app = App(), noteDao = noteDao)
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
        assertEquals(0, noteDao.findByIdCalls)
    }
}

private class CountingNoteDao : NoteDao {
    var findByIdCalls = 0

    override fun observeAll(): Flow<List<NoteEntity>> = emptyFlow()
    override suspend fun listAll(): List<NoteEntity> = emptyList()
    override suspend fun findById(id: Long): NoteEntity? {
        findByIdCalls++
        return null
    }
    override suspend fun findByIds(ids: List<Long>): List<NoteEntity> = emptyList()
    override suspend fun upsert(note: NoteEntity): Long = note.id
    override suspend fun upsertAll(notes: List<NoteEntity>) = Unit
    override suspend fun delete(id: Long) = Unit
    override suspend fun deleteByIds(ids: List<Long>) = Unit
    override suspend fun deleteAll() = Unit
}

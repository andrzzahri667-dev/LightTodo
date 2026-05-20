package com.zahri.lighttodo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todo ORDER BY (dateMillis IS NULL), dateMillis ASC, createdAtMillis ASC")
    fun observeAll(): Flow<List<TodoEntity>>

    @Query("""
        SELECT * FROM todo
        WHERE done = 0 AND (dateMillis IS NULL OR dateMillis <= :endOfDayMillis)
        ORDER BY (dateMillis IS NULL), dateMillis ASC, createdAtMillis ASC
    """)
    fun observeDueByDay(endOfDayMillis: Long): Flow<List<TodoEntity>>

    /** Used by widget (synchronous). */
    @Query("""
        SELECT * FROM todo
        WHERE done = 0 AND (dateMillis IS NULL OR dateMillis <= :endOfDayMillis)
        ORDER BY (dateMillis IS NULL), dateMillis ASC, createdAtMillis ASC
        LIMIT :limit
    """)
    fun listDueByDaySync(endOfDayMillis: Long, limit: Int): List<TodoEntity>

    /**
     * Widget：所有未完成任务，过期优先（越过期越前），未来任务按时间近→远；
     * 无日期任务排在所有有日期任务之后，再按 createdAtMillis 升序。
     */
    @Query("""
        SELECT * FROM todo WHERE done = 0
        ORDER BY
            (dateMillis IS NULL),
            (CASE WHEN dateMillis IS NULL THEN 0
                  WHEN startHour IS NOT NULL AND startMinute IS NOT NULL
                    THEN dateMillis + startHour * 3600000 + startMinute * 60000 - :nowMillis
                  ELSE dateMillis - :nowMillis
            END) ASC,
            createdAtMillis ASC
        LIMIT :limit
    """)
    fun listAllUndoneSync(nowMillis: Long, limit: Int): List<TodoEntity>

    @Query("SELECT * FROM todo WHERE id = :id")
    suspend fun findById(id: Long): TodoEntity?

    @Query("SELECT * FROM todo WHERE id = :id")
    fun findByIdSync(id: Long): TodoEntity?

    @Query("SELECT * FROM todo")
    suspend fun listAll(): List<TodoEntity>

    @Query("SELECT * FROM todo WHERE done = 0 AND remindAtMillis IS NOT NULL")
    suspend fun listWithReminders(): List<TodoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(todo: TodoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(todos: List<TodoEntity>)

    @Update
    suspend fun update(todo: TodoEntity)

    @Query("UPDATE todo SET done = :done, doneAtMillis = :doneAtMillis WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, doneAtMillis: Long?)

    @Query("DELETE FROM todo WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM todo WHERE done = 1")
    suspend fun deleteAllDone()

    @Query("DELETE FROM todo")
    suspend fun deleteAll()

    /** For calendar sync: existing event ids we already imported. */
    @Query("SELECT calendarEventId FROM todo WHERE calendarEventId IS NOT NULL")
    suspend fun listCalendarEventIds(): List<Long>

    @Query("SELECT * FROM todo WHERE calendarEventId IN (:eventIds)")
    suspend fun findByCalendarEventIds(eventIds: List<Long>): List<TodoEntity>

    @Query("DELETE FROM todo WHERE calendarEventId IS NOT NULL AND calendarEventId NOT IN (:keepIds)")
    suspend fun deleteCalendarOrphans(keepIds: List<Long>)

    @Transaction
    suspend fun replaceAll(tags: List<TagEntity>, todos: List<TodoEntity>) {
        deleteAll()
        upsertAll(todos)
    }
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tag ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tag ORDER BY sortOrder ASC, id ASC")
    suspend fun listAll(): List<TagEntity>

    @Query("SELECT * FROM tag WHERE id = :id")
    suspend fun findById(id: Long): TagEntity?

    @Query("SELECT * FROM tag WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tags: List<TagEntity>)

    @Query("DELETE FROM tag WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM tag")
    suspend fun deleteAll()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM note ORDER BY updatedAtMillis DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note ORDER BY updatedAtMillis DESC")
    suspend fun listAll(): List<NoteEntity>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun findById(id: Long): NoteEntity?

    @Query("SELECT * FROM note WHERE id IN (:ids)")
    suspend fun findByIds(ids: List<Long>): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM note WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM note")
    suspend fun deleteAll()
}

package com.zahri.lighttodo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zahri.lighttodo.BuildConfig

@Database(
    entities = [TodoEntity::class, TagEntity::class, NoteEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun tagDao(): TagDao
    abstract fun noteDao(): NoteDao

    companion object {
        val databaseName: String = BuildConfig.DB_NAME

        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todo ADD COLUMN startHour INTEGER")
                db.execSQL("ALTER TABLE todo ADD COLUMN startMinute INTEGER")
                db.execSQL("ALTER TABLE todo ADD COLUMN remindStartAtMillis INTEGER")
            }
        }

        /**
         * v3：放开 todo.date / todo.dateMillis 的 NOT NULL 约束，
         * 以支持"无日期任务"。
         *
         * SQLite 不支持 ALTER COLUMN 修改约束，必须走"建新表 → 拷数据 → 删旧表 → 重命名"流程，
         * 同时重建索引，保持与 Room schema 一致。
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS todo_new (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        title TEXT,
                        note TEXT,
                        date INTEGER,
                        dateMillis INTEGER,
                        startHour INTEGER,
                        startMinute INTEGER,
                        deadlineHour INTEGER,
                        deadlineMinute INTEGER,
                        remindStartAtMillis INTEGER,
                        remindAtMillis INTEGER,
                        customRemindHoursBefore INTEGER,
                        tagId INTEGER,
                        done INTEGER NOT NULL,
                        doneAtMillis INTEGER,
                        createdAtMillis INTEGER NOT NULL,
                        calendarEventId INTEGER,
                        FOREIGN KEY(tagId) REFERENCES tag(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO todo_new (
                        id, title, note, date, dateMillis,
                        startHour, startMinute, deadlineHour, deadlineMinute,
                        remindStartAtMillis, remindAtMillis, customRemindHoursBefore,
                        tagId, done, doneAtMillis, createdAtMillis, calendarEventId
                    )
                    SELECT
                        id, title, note, date, dateMillis,
                        startHour, startMinute, deadlineHour, deadlineMinute,
                        remindStartAtMillis, remindAtMillis, customRemindHoursBefore,
                        tagId, done, doneAtMillis, createdAtMillis, calendarEventId
                    FROM todo
                """.trimIndent())
                db.execSQL("DROP TABLE todo")
                db.execSQL("ALTER TABLE todo_new RENAME TO todo")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_todo_tagId ON todo(tagId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_todo_dateMillis ON todo(dateMillis)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_todo_done ON todo(done)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_todo_calendarEventId ON todo(calendarEventId)")
            }
        }

        /** v4：新增 note 表，支持笔记功能。 */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS note (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        title TEXT,
                        content TEXT NOT NULL DEFAULT '',
                        tagId INTEGER,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        FOREIGN KEY(tagId) REFERENCES tag(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_tagId ON note(tagId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_updatedAtMillis ON note(updatedAtMillis)")
            }
        }

        /** v5：给主页和提醒查询补复合索引，避免 todo 表增长后全表扫描。 */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_todo_done_dateMillis_createdAtMillis " +
                        "ON todo(done, dateMillis, createdAtMillis)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_todo_done_remindStartAtMillis_remindAtMillis " +
                        "ON todo(done, remindStartAtMillis, remindAtMillis)"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    databaseName
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { INSTANCE = it }
            }
    }
}

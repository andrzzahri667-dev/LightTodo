package com.zahri.lighttodo.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 一条任务。
 *
 * 设计要点：
 *  - 标题可空。空时列表上回退使用 note 首行 / "无标题"
 *  - 日期使用 yyyyMMdd 整数 (date) + dateMillis（当天 0 点的本地时间戳）双存：
 *    date 用于按日分组，dateMillis 用于排序、过期判断
 *  - date / dateMillis 同时可空（且必须同时为 null 或同时非 null）。
 *    null 表示"无日期任务"——既无开始也无截止，不参与按日期排序与过期判断。
 *  - 截止时间分为两个字段：dateMillis (天) + deadlineHour/deadlineMinute (可空，全天则为 null)
 *  - remindAtMillis 提前算出，AlarmManager 直接用
 *  - tagId 单标签，可空（=未分类）
 *  - sourceCalendarId 非空表示来自系统日历，主页只读显示
 */
@Entity(
    tableName = "todo",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("tagId"), Index("dateMillis"), Index("done"), Index("calendarEventId", unique = true)]
)
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String? = null,
    val note: String? = null,
    /** yyyyMMdd e.g. 20260427；null 表示无日期任务 */
    val date: Int? = null,
    /** 当天本地 0:00 的毫秒时间戳，用于排序和比较；null 表示无日期任务 */
    val dateMillis: Long? = null,
    /** 开始时刻；null 表示全天或未设 */
    val startHour: Int? = null,
    val startMinute: Int? = null,
    /** 截止时刻；null 表示全天或未设 */
    val deadlineHour: Int? = null,
    val deadlineMinute: Int? = null,
    /** 开始时间提醒时间戳 */
    val remindStartAtMillis: Long? = null,
    /** 截止时间提醒时间戳 */
    val remindAtMillis: Long? = null,
    /** 是否使用了"提前 N 小时"自定义提醒。null = 使用全局默认 */
    val customRemindHoursBefore: Int? = null,
    val tagId: Long? = null,
    val done: Boolean = false,
    val doneAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    /** 来自小米日历的事件 ID；非空表示只读 */
    val calendarEventId: Long? = null
)

fun TodoEntity.hasAnyReminder(): Boolean =
    remindStartAtMillis != null || remindAtMillis != null

@Entity(
    tableName = "tag",
    indices = [Index("name", unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(defaultValue = "0") val sortOrder: Int = 0
)

/**
 * 一条笔记。
 *
 * 设计要点：
 *  - content 存 Markdown 原文
 *  - tagId 预留标签接口，当前 UI 不暴露分类功能
 *  - 排序按 updatedAtMillis DESC（最近编辑在最前）
 */
@Entity(
    tableName = "note",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("tagId"), Index("updatedAtMillis")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String? = null,
    val content: String = "",
    val tagId: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

/* ---------- DTO for export/import (kotlinx.serialization) ---------- */

@Serializable
data class BackupBundle(
    val version: Int = 2,
    val tags: List<BackupTag>,
    val todos: List<BackupTodo>,
    val notes: List<BackupNote> = emptyList()
)

@Serializable
data class BackupTag(val id: Long, val name: String, val sortOrder: Int)

@Serializable
data class BackupTodo(
    val id: Long,
    val title: String?,
    val note: String?,
    val date: Int? = null,
    val dateMillis: Long? = null,
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val deadlineHour: Int?,
    val deadlineMinute: Int?,
    val remindStartAtMillis: Long? = null,
    val remindAtMillis: Long?,
    val customRemindHoursBefore: Int?,
    val tagId: Long?,
    val done: Boolean,
    val doneAtMillis: Long?,
    val createdAtMillis: Long
)

@Serializable
data class BackupNote(
    val id: Long,
    val title: String?,
    val content: String,
    val tagId: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

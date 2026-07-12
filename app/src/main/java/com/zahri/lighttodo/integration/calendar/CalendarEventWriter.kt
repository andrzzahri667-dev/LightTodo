package com.zahri.lighttodo.integration.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.zahri.lighttodo.R
import com.zahri.lighttodo.domain.calendar.CalendarEventDraft
import com.zahri.lighttodo.domain.calendar.CalendarEventTodo
import com.zahri.lighttodo.domain.calendar.CalendarEventWritePolicy
import com.zahri.lighttodo.domain.calendar.CalendarSyncPolicy
import com.zahri.lighttodo.usecase.todo.TodoRecord
import java.time.ZoneId

internal object CalendarEventWriter {
    fun upsertFromTodo(
        context: Context,
        todo: TodoRecord,
        userFilter: String
    ): Long? {
        val draft = CalendarEventWritePolicy.draftFor(
            todo.toCalendarEventTodo(),
            ZoneId.systemDefault()
        ) ?: return null
        if (!hasReadPermission(context) || !hasWritePermission(context)) return todo.calendarEventId

        val cr = context.contentResolver
        val values = valuesFor(context, draft)
        val existingEventId = todo.calendarEventId
        if (existingEventId != null) {
            val updated = runCatching {
                cr.update(
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId),
                    values,
                    null,
                    null
                )
            }.getOrDefault(0)
            if (updated > 0) return existingEventId
        }

        val calendarId = pickWritableCalendarId(context, userFilter) ?: return existingEventId
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId)
        return runCatching {
            cr.insert(CalendarContract.Events.CONTENT_URI, values)?.let(ContentUris::parseId)
        }.getOrNull() ?: existingEventId
    }

    fun setCompleted(context: Context, eventId: Long, done: Boolean): Boolean {
        if (!hasWritePermission(context)) return false
        val values = ContentValues().apply {
            put(
                CalendarContract.Events.STATUS,
                if (done) CalendarContract.Events.STATUS_CANCELED
                else CalendarContract.Events.STATUS_CONFIRMED
            )
        }
        return runCatching {
            context.contentResolver.update(
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
                values,
                null,
                null
            ) > 0
        }.getOrDefault(false)
    }

    fun deleteEvent(context: Context, eventId: Long): Boolean {
        if (!hasWritePermission(context)) return false
        return runCatching {
            context.contentResolver.delete(
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
                null,
                null
            ) > 0
        }.getOrDefault(false)
    }

    private fun valuesFor(context: Context, draft: CalendarEventDraft): ContentValues =
        ContentValues().apply {
            val title = draft.title.ifBlank { context.getString(R.string.calendar_no_title) }
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, draft.description)
            put(CalendarContract.Events.DTSTART, draft.startMillis)
            put(CalendarContract.Events.DTEND, draft.endMillis)
            put(CalendarContract.Events.ALL_DAY, if (draft.allDay) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, draft.timezoneId)
            put(CalendarContract.Events.EVENT_END_TIMEZONE, draft.timezoneId)
            put(
                CalendarContract.Events.STATUS,
                if (draft.completed) CalendarContract.Events.STATUS_CANCELED
                else CalendarContract.Events.STATUS_CONFIRMED
            )
        }

    private fun pickWritableCalendarId(context: Context, userFilter: String): Long? {
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
            ),
            null,
            null,
            null
        ) ?: return null

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val accountName = it.getString(1).orEmpty()
                val accountType = it.getString(2).orEmpty()
                val displayName = it.getString(3).orEmpty()
                val accessLevel = if (it.isNull(4)) 0 else it.getInt(4)
                if (CalendarSyncPolicy.shouldExcludeCalendarName(displayName)) continue
                if (!CalendarSyncPolicy.matchesAccount(accountName, accountType, userFilter)) continue
                if (accessLevel < CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) continue
                return id
            }
        }
        return null
    }

    private fun hasReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasWritePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    private fun TodoRecord.toCalendarEventTodo(): CalendarEventTodo =
        CalendarEventTodo(
            title = title,
            note = note,
            date = date,
            startHour = startHour,
            startMinute = startMinute,
            deadlineHour = deadlineHour,
            deadlineMinute = deadlineMinute,
            done = done
        )
}

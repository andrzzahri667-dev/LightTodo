package com.zahri.lighttodo.integration.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.zahri.lighttodo.domain.calendar.CalendarSyncPolicy
import com.zahri.lighttodo.usecase.calendar.CalendarProviderEvent
import com.zahri.lighttodo.usecase.calendar.CalendarProviderEvents
import com.zahri.lighttodo.usecase.calendar.CalendarSyncGateway

class AndroidCalendarSyncGateway(
    private val context: Context
) : CalendarSyncGateway {
    override fun hasReadCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    override fun queryEvents(userFilter: String, fromMillis: Long, toMillis: Long): CalendarProviderEvents {
        val calendarIds = pickCalendarIds(userFilter)
        if (calendarIds.isEmpty()) {
            return CalendarProviderEvents(hasCalendars = false, events = emptyList())
        }
        return CalendarProviderEvents(
            hasCalendars = true,
            events = queryEvents(calendarIds, fromMillis, toMillis)
        )
    }

    private fun pickCalendarIds(userFilter: String): List<Long> {
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
            ),
            null,
            null,
            null
        ) ?: return emptyList()

        val ids = mutableListOf<Long>()
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val accountName = it.getString(1).orEmpty()
                val accountType = it.getString(2).orEmpty()
                val displayName = it.getString(3).orEmpty()
                if (CalendarSyncPolicy.shouldExcludeCalendarName(displayName)) continue
                if (CalendarSyncPolicy.matchesAccount(accountName, accountType, userFilter)) {
                    ids += id
                }
            }
        }
        return ids
    }

    private fun queryEvents(
        calendarIds: List<Long>,
        fromMillis: Long,
        toMillis: Long
    ): List<CalendarProviderEvent> {
        val placeholders = calendarIds.joinToString(",") { "?" }
        val selection = "${CalendarContract.Events.CALENDAR_ID} IN ($placeholders) AND " +
            "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val args = (calendarIds.map { it.toString() } + listOf(
            fromMillis.toString(),
            toMillis.toString()
        )).toTypedArray()

        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.STATUS
            ),
            selection,
            args,
            "${CalendarContract.Events.DTSTART} ASC"
        ) ?: return emptyList()

        val events = mutableListOf<CalendarProviderEvent>()
        cursor.use {
            while (it.moveToNext()) {
                val status = if (it.isNull(6)) null else it.getInt(6)
                events += CalendarProviderEvent(
                    id = it.getLong(0),
                    title = it.getString(1).orEmpty(),
                    description = it.getString(2),
                    startMillis = it.getLong(3),
                    allDay = it.getInt(4) == 1,
                    endMillis = if (it.isNull(5)) null else it.getLong(5),
                    canceled = status == CalendarContract.Events.STATUS_CANCELED
                )
            }
        }
        return events
    }
}

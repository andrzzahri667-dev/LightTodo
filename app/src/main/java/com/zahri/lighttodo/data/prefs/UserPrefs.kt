package com.zahri.lighttodo.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zahri.lighttodo.usecase.boot.BootPreferences
import com.zahri.lighttodo.usecase.boot.BootPreferencesRepository
import com.zahri.lighttodo.usecase.calendar.CalendarSyncPreferences
import com.zahri.lighttodo.usecase.calendar.CalendarSyncPreferencesRepository
import com.zahri.lighttodo.usecase.todo.HomePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

/**
 * 用户偏好。设计原则：所有项都有合理默认，启动即可用，无需必填。
 */
class UserPrefs(
    private val context: Context
) : HomePreferencesRepository, CalendarSyncPreferencesRepository, BootPreferencesRepository {

    private object Keys {
        val DEFAULT_REMIND_HOUR = intPreferencesKey("default_remind_hour")
        val DEFAULT_REMIND_MINUTE = intPreferencesKey("default_remind_minute")
        val DEFAULT_HOURS_BEFORE = intPreferencesKey("default_hours_before")
        val CALENDAR_SYNC_ENABLED = booleanPreferencesKey("calendar_sync_enabled")
        val CALENDAR_ACCOUNT_NAME = stringPreferencesKey("calendar_account_name")
        val QUICK_ADD_NOTIF_ENABLED = booleanPreferencesKey("quick_add_notif_enabled")
        val EXPANDED_TAG_IDS = stringPreferencesKey("collapsed_tag_ids")
        val DONE_SECTION_EXPANDED = booleanPreferencesKey("done_section_expanded")
        val PORTABLE_BACKUP_TREE_URI = stringPreferencesKey("portable_backup_tree_uri")
    }

    @Serializable
    data class Snapshot(
        val defaultRemindHour: Int = 9,
        val defaultRemindMinute: Int = 0,
        val defaultHoursBefore: Int = 2,
        val calendarSyncEnabled: Boolean = false,
        val calendarAccountName: String = "", // empty = match every non-holiday calendar by default
        val quickAddNotifEnabled: Boolean = false,
        /** Collapsed group keys; empty = all expanded (the default). */
        val collapsedTagIds: Set<String> = emptySet(),
        val doneSectionExpanded: Boolean = false
    )

    val flow: Flow<Snapshot> = context.userPrefsDataStore.data.map { p ->
        Snapshot(
            defaultRemindHour = p[Keys.DEFAULT_REMIND_HOUR] ?: 9,
            defaultRemindMinute = p[Keys.DEFAULT_REMIND_MINUTE] ?: 0,
            defaultHoursBefore = p[Keys.DEFAULT_HOURS_BEFORE] ?: 2,
            calendarSyncEnabled = p[Keys.CALENDAR_SYNC_ENABLED] ?: false,
            calendarAccountName = p[Keys.CALENDAR_ACCOUNT_NAME].orEmpty(),
            quickAddNotifEnabled = p[Keys.QUICK_ADD_NOTIF_ENABLED] ?: false,
            collapsedTagIds = (p[Keys.EXPANDED_TAG_IDS]?.split('|')?.filter { it.isNotEmpty() }?.toSet()) ?: emptySet(),
            doneSectionExpanded = p[Keys.DONE_SECTION_EXPANDED] ?: false
        )
    }

    suspend fun snapshot(): Snapshot = flow.first()

    suspend fun portableBackupTreeUri(): String? =
        context.userPrefsDataStore.data.map { it[Keys.PORTABLE_BACKUP_TREE_URI] }.first()

    suspend fun setPortableBackupTreeUri(uri: String) {
        context.userPrefsDataStore.edit { it[Keys.PORTABLE_BACKUP_TREE_URI] = uri }
    }

    suspend fun clearPortableBackupTreeUri() {
        context.userPrefsDataStore.edit { it.remove(Keys.PORTABLE_BACKUP_TREE_URI) }
    }

    override suspend fun calendarSyncPreferences(): CalendarSyncPreferences =
        snapshot().let {
            CalendarSyncPreferences(
                calendarSyncEnabled = it.calendarSyncEnabled,
                calendarAccountName = it.calendarAccountName
            )
        }

    override suspend fun bootPreferences(): BootPreferences =
        snapshot().let {
            BootPreferences(
                quickAddNotifEnabled = it.quickAddNotifEnabled,
                calendarSyncEnabled = it.calendarSyncEnabled
            )
        }

    suspend fun setDefaultRemind(hour: Int, minute: Int) {
        context.userPrefsDataStore.edit {
            it[Keys.DEFAULT_REMIND_HOUR] = hour
            it[Keys.DEFAULT_REMIND_MINUTE] = minute
        }
    }

    suspend fun setDefaultHoursBefore(hours: Int) {
        context.userPrefsDataStore.edit { it[Keys.DEFAULT_HOURS_BEFORE] = hours.coerceIn(0, 72) }
    }

    suspend fun setCalendarSyncEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.CALENDAR_SYNC_ENABLED] = enabled }
    }

    suspend fun setCalendarAccountName(name: String) {
        context.userPrefsDataStore.edit { it[Keys.CALENDAR_ACCOUNT_NAME] = name }
    }

    suspend fun setQuickAddNotifEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.QUICK_ADD_NOTIF_ENABLED] = enabled }
    }

    override suspend fun setCollapsedTagIds(ids: Set<String>) {
        context.userPrefsDataStore.edit { it[Keys.EXPANDED_TAG_IDS] = ids.joinToString("|") }
    }

    override suspend fun setDoneSectionExpanded(expanded: Boolean) {
        context.userPrefsDataStore.edit { it[Keys.DONE_SECTION_EXPANDED] = expanded }
    }

    suspend fun restore(snapshot: Snapshot) {
        context.userPrefsDataStore.edit {
            it[Keys.DEFAULT_REMIND_HOUR] = snapshot.defaultRemindHour
            it[Keys.DEFAULT_REMIND_MINUTE] = snapshot.defaultRemindMinute
            it[Keys.DEFAULT_HOURS_BEFORE] = snapshot.defaultHoursBefore
            it[Keys.CALENDAR_SYNC_ENABLED] = snapshot.calendarSyncEnabled
            it[Keys.CALENDAR_ACCOUNT_NAME] = snapshot.calendarAccountName
            it[Keys.QUICK_ADD_NOTIF_ENABLED] = snapshot.quickAddNotifEnabled
            it[Keys.EXPANDED_TAG_IDS] = snapshot.collapsedTagIds.joinToString("|")
            it[Keys.DONE_SECTION_EXPANDED] = snapshot.doneSectionExpanded
        }
    }
}

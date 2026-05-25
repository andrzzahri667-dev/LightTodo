package com.zahri.lighttodo.calendar

import java.util.Locale

object CalendarSyncPolicy {
    const val SyncTimeoutMillis = 15_000L

    private val excludedNameKeywords = listOf("节日", "假期", "假日", "Holidays", "节假日")

    fun shouldExcludeCalendarName(displayName: String): Boolean =
        excludedNameKeywords.any { keyword -> displayName.contains(keyword, ignoreCase = true) }

    fun matchesAccount(acctName: String, userFilter: String): Boolean {
        val filter = userFilter.trim()
        if (filter.isNotEmpty()) {
            return acctName.equals(filter, ignoreCase = true) ||
                acctName.contains(filter, ignoreCase = true)
        }

        val normalized = acctName.trim().lowercase(Locale.ROOT)
        return normalized.contains("xiaomi") ||
            acctName.contains("小米") ||
            normalized == "mi" ||
            normalized.startsWith("mi ") ||
            normalized.endsWith(" mi") ||
            normalized.contains(" mi ")
    }

    fun orphanEventIds(
        importedEventIds: List<Long>,
        providerEventIds: List<Long>
    ): List<Long> {
        if (importedEventIds.isEmpty()) return emptyList()
        val liveIds = providerEventIds.toHashSet()
        return importedEventIds.filterNot { it in liveIds }
    }
}

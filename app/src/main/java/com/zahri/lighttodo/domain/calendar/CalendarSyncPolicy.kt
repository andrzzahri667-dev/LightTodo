package com.zahri.lighttodo.domain.calendar

object CalendarSyncPolicy {
    const val SyncTimeoutMillis = 15_000L

    private val excludedNameKeywords = listOf("节日", "假期", "假日", "Holidays", "节假日")

    fun shouldExcludeCalendarName(displayName: String): Boolean =
        excludedNameKeywords.any { keyword -> displayName.contains(keyword, ignoreCase = true) }

    @Suppress("UNUSED_PARAMETER")
    fun matchesAccount(acctName: String, acctType: String, userFilter: String): Boolean {
        val filter = userFilter.trim()
        if (filter.isNotEmpty()) {
            return acctName.equals(filter, ignoreCase = true) ||
                acctName.contains(filter, ignoreCase = true)
        }
        // 无过滤条件时匹配所有非节日日历
        return true
    }

    fun orphanEventIds(
        importedEventIds: List<Long>,
        providerEventIds: List<Long>
    ): List<Long> {
        if (importedEventIds.isEmpty()) return emptyList()
        val liveIds = providerEventIds.toHashSet()
        return importedEventIds.filterNot { it in liveIds }
    }

    fun mergeDoneState(
        providerCanceled: Boolean,
        existingDone: Boolean?,
        existingDoneAtMillis: Long?,
        nowMillis: Long
    ): DoneMerge {
        if (!providerCanceled) {
            return DoneMerge(
                done = existingDone ?: false,
                doneAtMillis = existingDoneAtMillis
            )
        }
        return DoneMerge(
            done = true,
            doneAtMillis = existingDoneAtMillis ?: nowMillis
        )
    }

    data class DoneMerge(
        val done: Boolean,
        val doneAtMillis: Long?
    )
}

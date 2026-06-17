package com.zahri.lighttodo.domain.todo

import com.zahri.lighttodo.util.DateUtils

sealed class TodoDateFields {
    abstract val date: Int?
    abstract val dateMillis: Long?

    data object None : TodoDateFields() {
        override val date: Int? = null
        override val dateMillis: Long? = null
    }

    data class Dated(
        override val date: Int,
        override val dateMillis: Long
    ) : TodoDateFields() {
        init {
            require(date > 0 && dateMillis >= 0L) { "Dated todo fields require both date and dateMillis." }
        }
    }

    companion object {
        fun fromParts(year: Int?, month: Int?, day: Int?): TodoDateFields {
            if (year == null || month == null || day == null) return None
            val (date, dateMillis) = DateUtils.dayKeyAndStart(year, month, day)
            return Dated(date, dateMillis)
        }

        fun fromEpochMillis(epochMillis: Long): Dated {
            val (date, dateMillis) = DateUtils.dayKeyAndStartFromMillis(epochMillis)
            return Dated(date, dateMillis)
        }

        fun fromStored(date: Int?, dateMillis: Long?): TodoDateFields {
            if (date == null && dateMillis == null) return None
            require(date != null && dateMillis != null) {
                "Stored todo date fields must be both null or both non-null."
            }
            return Dated(date, dateMillis)
        }
    }
}

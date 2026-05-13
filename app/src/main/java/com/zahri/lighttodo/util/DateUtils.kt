package com.zahri.lighttodo.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun todayDayKey(): Int = toDayKey(LocalDate.now(zone))

    fun toDayKey(d: LocalDate): Int = d.year * 10000 + d.monthValue * 100 + d.dayOfMonth

    fun startOfDayMillis(d: LocalDate): Long =
        d.atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDayMillis(d: LocalDate): Long =
        d.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()

    fun fromDayKey(key: Int): LocalDate {
        val y = key / 10000
        val m = (key / 100) % 100
        val d = key % 100
        return LocalDate.of(y, m, d)
    }

    fun dayKeyAndStart(year: Int, month: Int, day: Int): Pair<Int, Long> {
        val date = LocalDate.of(year, month, day)
        return toDayKey(date) to startOfDayMillis(date)
    }

    fun dayKeyAndStartFromMillis(epoch: Long): Pair<Int, Long> {
        val ld = Instant.ofEpochMilli(epoch).atZone(zone).toLocalDate()
        return toDayKey(ld) to startOfDayMillis(ld)
    }

    private val mdFmt = DateTimeFormatter.ofPattern("MM月dd日")
    private val ymdFmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日")

    /** 与今年同年时只显示 MM月dd日；否则显示完整日期 */
    fun displayDate(dayKey: Int): String {
        val d = fromDayKey(dayKey)
        return if (d.year == LocalDate.now(zone).year) d.format(mdFmt) else d.format(ymdFmt)
    }

    fun isOverdue(dayKey: Int): Boolean {
        return fromDayKey(dayKey).isBefore(LocalDate.now(zone))
    }

    fun isToday(dayKey: Int): Boolean = dayKey == todayDayKey()

    fun formatTime(hour: Int, minute: Int): String =
        "%02d:%02d".format(hour, minute)

    fun nowDateTime(): LocalDateTime = LocalDateTime.now(zone)
}

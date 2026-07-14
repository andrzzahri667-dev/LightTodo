package com.zahri.lighttodo.feature.todoedit

import com.zahri.lighttodo.test.sourceFile
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelDatePickerTest {
    @Test
    fun dayLabelsMatchSettledMonthLength() {
        assertEquals((1..28).map(Int::toString), generateAllDayPickerDayLabels(LocalDate.of(2026, 2, 1)))
        assertEquals((1..29).map(Int::toString), generateAllDayPickerDayLabels(LocalDate.of(2028, 2, 1)))
        assertEquals((1..30).map(Int::toString), generateAllDayPickerDayLabels(LocalDate.of(2026, 4, 1)))
        assertEquals((1..31).map(Int::toString), generateAllDayPickerDayLabels(LocalDate.of(2026, 7, 1)))
    }

    @Test
    fun dayIndexClampsOnlyToSettledMonthLength() {
        assertEquals(27, clampDayIndexToMonth(30, LocalDate.of(2026, 2, 1)))
        assertEquals(28, clampDayIndexToMonth(30, LocalDate.of(2028, 2, 1)))
        assertEquals(29, clampDayIndexToMonth(30, LocalDate.of(2026, 4, 1)))
        assertEquals(30, clampDayIndexToMonth(30, LocalDate.of(2026, 7, 1)))
    }

    @Test
    fun allDayPickerClampsAfterMonthScrollSettles() {
        val source = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/todoedit/EditDateTimePicker.kt"
        ).readText()
        val wheelSource = sourceFile(
            "app/src/main/java/com/zahri/lighttodo/feature/todoedit/WheelPicker.kt"
        ).readText()

        assertTrue(source.contains("var settledMonthIndex by remember"))
        assertTrue(source.contains("remember(settledMonthIndex)"))
        assertTrue(source.contains("generateAllDayPickerDayLabels(monthList[settledMonthIndex])"))
        assertTrue(source.contains("onScrollSettled = { monthIndex ->"))
        assertTrue(source.contains("settledMonthIndex = monthIndex"))
        assertFalse(source.contains("loopThreshold = 0"))
        assertFalse(source.contains("derivedStateOf { (1..daysInMonth).map"))
        assertTrue(wheelSource.contains("onScrollSettled: (Int) -> Unit = {}"))
        assertTrue(wheelSource.contains("snapshotFlow { listState.isScrollInProgress to logicalIndex }"))
        assertTrue(wheelSource.contains("var programmaticScrollTarget by remember"))
        assertTrue(wheelSource.contains("programmaticScrollTarget == null"))
        assertTrue(wheelSource.contains("val listState = key(items.size, looping)"))
    }
}

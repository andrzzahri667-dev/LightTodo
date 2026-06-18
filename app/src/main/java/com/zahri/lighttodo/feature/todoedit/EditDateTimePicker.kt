package com.zahri.lighttodo.feature.todoedit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zahri.lighttodo.R
import com.zahri.lighttodo.ui.motion.WheelPickerMotionPolicy
import com.zahri.lighttodo.ui.theme.AppColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

internal fun generateDateList(centerDate: LocalDate): List<LocalDate> =
    (-180..180).map { centerDate.plusDays(it.toLong()) }

internal fun formatDateForWheel(date: LocalDate): String {
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return "$dayOfWeek, ${date.monthValue}/${date.dayOfMonth}"
}

@Composable
internal fun WheelDateTimePickerDialog(
    title: String,
    initialDate: LocalDate,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (LocalDate, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val dateList = remember(initialDate) { generateDateList(initialDate) }
    val dateLabels = remember(dateList) { dateList.map { formatDateForWheel(it) } }
    val hourLabels = remember { (0..23).map { "%d".format(it) } }
    val minuteLabels = remember { (0..59).map { "%02d".format(it) } }

    val initialDateIndex = remember(initialDate, dateList) {
        dateList.indexOf(initialDate).coerceAtLeast(0)
    }

    var selectedDateIndex by remember { mutableIntStateOf(initialDateIndex) }
    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))

            var liveDate by remember { mutableIntStateOf(initialDateIndex) }
            var liveHour by remember { mutableIntStateOf(initialHour) }
            var liveMinute by remember { mutableIntStateOf(initialMinute) }

            val headerDate = dateList.getOrElse(liveDate) { initialDate }
            Text(
                text = "${formatDateForWheel(headerDate)}, %02d:%02d".format(liveHour, liveMinute),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dIdx = WheelPicker(
                    items = dateLabels,
                    selectedIndex = selectedDateIndex,
                    onSelectedChanged = { selectedDateIndex = it; liveDate = it },
                    modifier = Modifier.weight(1.6f),
                    textMotion = WheelPickerMotionPolicy.DateColumnTextMotion
                )
                val hIdx = WheelPicker(
                    items = hourLabels,
                    selectedIndex = selectedHour,
                    onSelectedChanged = { selectedHour = it; liveHour = it },
                    modifier = Modifier.weight(0.7f),
                    textMotion = WheelPickerMotionPolicy.TimeColumnTextMotion,
                    superscript = "H"
                )
                val mIdx = WheelPicker(
                    items = minuteLabels,
                    selectedIndex = selectedMinute,
                    onSelectedChanged = { selectedMinute = it; liveMinute = it },
                    modifier = Modifier.weight(0.7f),
                    textMotion = WheelPickerMotionPolicy.TimeColumnTextMotion,
                    superscript = "M"
                )
                LaunchedEffect(dIdx) { if (dIdx != liveDate) liveDate = dIdx }
                LaunchedEffect(hIdx) { if (hIdx != liveHour) liveHour = hIdx }
                LaunchedEffect(mIdx) { if (mIdx != liveMinute) liveMinute = mIdx }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        stringResource(R.string.edit_picker_cancel),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                Button(
                    onClick = {
                        val confirmDate = dateList.getOrElse(liveDate) { initialDate }
                        onConfirm(confirmDate, liveHour, liveMinute)
                    },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Brand,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.edit_picker_confirm), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}


/**
 * 仅含时间(小时/分钟)的滚轮选择器,用于设置页"默认提醒时间"等场景。
 * 与 WheelDateTimePickerDialog 保持视觉一致,差别只在去掉了日期列。
 */
@Composable
fun WheelTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val hourLabels = remember { (0..23).map { "%d".format(it) } }
    val minuteLabels = remember { (0..59).map { "%02d".format(it) } }

    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }
    var liveHour by remember { mutableIntStateOf(initialHour) }
    var liveMinute by remember { mutableIntStateOf(initialMinute) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))

            Text(
                text = "%02d:%02d".format(liveHour, liveMinute),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hIdx = WheelPicker(
                    items = hourLabels,
                    selectedIndex = selectedHour,
                    onSelectedChanged = { selectedHour = it; liveHour = it },
                    modifier = Modifier.weight(1f),
                    textMotion = WheelPickerMotionPolicy.TimeColumnTextMotion,
                    superscript = "H"
                )
                val mIdx = WheelPicker(
                    items = minuteLabels,
                    selectedIndex = selectedMinute,
                    onSelectedChanged = { selectedMinute = it; liveMinute = it },
                    modifier = Modifier.weight(1f),
                    textMotion = WheelPickerMotionPolicy.TimeColumnTextMotion,
                    superscript = "M"
                )
                LaunchedEffect(hIdx) { if (hIdx != liveHour) liveHour = hIdx }
                LaunchedEffect(mIdx) { if (mIdx != liveMinute) liveMinute = mIdx }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        stringResource(R.string.edit_picker_cancel),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                Button(
                    onClick = { onConfirm(liveHour, liveMinute) },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Brand,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.edit_picker_confirm), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}


/**
 * 仅含日期的滚轮选择器(两列:年月 + 日)。
 *
 * 用于"全天任务"切换日期 —— 全天没有时分概念,把时分列去掉避免误导。
 * 切换月份时,日期列动态根据当月天数(28/29/30/31)重建,初始
 * 选中的日如果超过新月最大天数,自动夹到月底。
 */
@Composable
fun WheelDatePickerDialog(
    title: String,
    initialDate: java.time.LocalDate,
    onConfirm: (java.time.LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    // 年-月列表:以 initialDate 为中心前后各 24 个月,共 49 个
    val monthList = remember(initialDate) {
        (-24..24).map { initialDate.withDayOfMonth(1).plusMonths(it.toLong()) }
    }
    // 字符串模板从资源加载,避免硬编码本地化文本
    val monthFmt = stringResource(R.string.date_picker_year_month)
    val ymdFmt = stringResource(R.string.date_picker_year_month_day)
    val monthLabels = remember(monthList, monthFmt) {
        monthList.map { monthFmt.format(it.year, it.monthValue) }
    }
    val initialMonthIndex = remember(initialDate, monthList) {
        monthList.indexOfFirst {
            it.year == initialDate.year && it.monthValue == initialDate.monthValue
        }.coerceAtLeast(0)
    }

    var selectedMonthIndex by remember { mutableIntStateOf(initialMonthIndex) }
    var liveMonthIndex by remember { mutableIntStateOf(initialMonthIndex) }

    val daysInMonth by remember(liveMonthIndex) {
        derivedStateOf { monthList[liveMonthIndex].lengthOfMonth() }
    }
    val dayLabels by remember(daysInMonth) {
        derivedStateOf { (1..daysInMonth).map { it.toString() } }
    }

    // 选中的日(0-based 索引);切月后若超过当月最大天数,夹到月底
    var selectedDayIndex by remember { mutableIntStateOf(initialDate.dayOfMonth - 1) }
    var liveDayIndex by remember { mutableIntStateOf(initialDate.dayOfMonth - 1) }

    LaunchedEffect(daysInMonth) {
        if (selectedDayIndex >= daysInMonth) {
            selectedDayIndex = daysInMonth - 1
            liveDayIndex = daysInMonth - 1
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))

            val headerMonth = monthList[liveMonthIndex]
            Text(
                text = ymdFmt.format(headerMonth.year, headerMonth.monthValue, liveDayIndex + 1),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val mIdx = WheelPicker(
                    items = monthLabels,
                    selectedIndex = selectedMonthIndex,
                    onSelectedChanged = { selectedMonthIndex = it; liveMonthIndex = it },
                    modifier = Modifier.weight(1.4f),
                    textMotion = WheelPickerMotionPolicy.DateColumnTextMotion
                )
                val dIdx = WheelPicker(
                    items = dayLabels,
                    selectedIndex = selectedDayIndex.coerceAtMost(daysInMonth - 1),
                    onSelectedChanged = { selectedDayIndex = it; liveDayIndex = it },
                    modifier = Modifier.weight(0.8f),
                    textMotion = WheelPickerMotionPolicy.DayColumnTextMotion
                )
                LaunchedEffect(mIdx) { if (mIdx != liveMonthIndex) liveMonthIndex = mIdx }
                LaunchedEffect(dIdx) { if (dIdx != liveDayIndex) liveDayIndex = dIdx }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        stringResource(R.string.edit_picker_cancel),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                Button(
                    onClick = {
                        val month = monthList[liveMonthIndex]
                        val day = (liveDayIndex + 1).coerceAtMost(month.lengthOfMonth())
                        onConfirm(month.withDayOfMonth(day))
                    },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Brand,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.edit_picker_confirm), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

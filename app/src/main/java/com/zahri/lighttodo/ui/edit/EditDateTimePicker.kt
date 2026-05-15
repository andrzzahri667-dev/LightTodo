package com.zahri.lighttodo.ui.edit

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
                    selectedFontSize = 18.sp,
                    unselectedFontSize = 14.sp
                )
                val hIdx = WheelPicker(
                    items = hourLabels,
                    selectedIndex = selectedHour,
                    onSelectedChanged = { selectedHour = it; liveHour = it },
                    modifier = Modifier.weight(0.7f),
                    selectedFontSize = 24.sp,
                    unselectedFontSize = 16.sp,
                    superscript = "H"
                )
                val mIdx = WheelPicker(
                    items = minuteLabels,
                    selectedIndex = selectedMinute,
                    onSelectedChanged = { selectedMinute = it; liveMinute = it },
                    modifier = Modifier.weight(0.7f),
                    selectedFontSize = 24.sp,
                    unselectedFontSize = 16.sp,
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

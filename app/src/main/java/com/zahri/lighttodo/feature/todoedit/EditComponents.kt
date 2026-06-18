package com.zahri.lighttodo.feature.todoedit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zahri.lighttodo.R
import com.zahri.lighttodo.domain.todo.TodoReminderDefaults
import com.zahri.lighttodo.ui.theme.AppColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun EditCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}

@Composable
internal fun EditRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
}

@Composable
internal fun EditSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    val labelColor =
        if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = labelColor)
        Spacer(Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.Brand,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
internal fun EditDateTimeRow(
    label: String,
    date: LocalDate?,
    time: Pair<Int, Int>?,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val fmt = stringResource(R.string.edit_date_format)
    val display = remember(date, time, fmt) {
        if (date == null) {
            "—"
        } else {
            val datePart = fmt.format(
                date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                date.monthValue,
                date.dayOfMonth
            )
            val timePart = time?.let { " %02d:%02d".format(it.first, it.second) } ?: ""
            "$datePart$timePart"
        }
    }
    val labelColor =
        if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val valueColor =
        if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    val chevronColor =
        if (enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = labelColor)
        Spacer(Modifier.weight(1f))
        Text(display, fontSize = 15.sp, color = valueColor)
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = chevronColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
internal fun EditReminderRow(
    hasTime: Boolean,
    customHoursBefore: Int?,
    defaultHoursBefore: Int,
    defaultRemindLabel: String,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val labelColor =
        if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val valueColor =
        if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.edit_reminder), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = labelColor)
        Spacer(Modifier.weight(1f))
        if (!hasTime) {
            Text(
                stringResource(R.string.edit_remind_default, defaultRemindLabel),
                fontSize = 15.sp,
                color = valueColor
            )
        } else {
            val hours = TodoReminderDefaults.effectiveHoursBefore(
                customHoursBefore = customHoursBefore,
                defaultHoursBefore = defaultHoursBefore
            )
            StepperButton(text = "−", enabled = enabled, onClick = onDecrease)
            Spacer(Modifier.width(10.dp))
            Text(
                if (hours == 0) stringResource(R.string.edit_remind_on_time) else stringResource(R.string.edit_remind_hours_before, hours),
                fontSize = 15.sp,
                color = valueColor
            )
            Spacer(Modifier.width(10.dp))
            StepperButton(text = "+", enabled = enabled, onClick = onIncrease)
        }
    }
}

@Composable
internal fun StepperButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

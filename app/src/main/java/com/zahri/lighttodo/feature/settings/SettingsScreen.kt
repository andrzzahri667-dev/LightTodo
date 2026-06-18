package com.zahri.lighttodo.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zahri.lighttodo.BuildConfig
import com.zahri.lighttodo.PermissionRequestPolicy
import com.zahri.lighttodo.R
import com.zahri.lighttodo.lightTodoViewModelFactory
import com.zahri.lighttodo.ui.theme.AppColors
import com.zahri.lighttodo.ui.theme.AppType

@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel(factory = lightTodoViewModelFactory())) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showRemindTimePicker by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { vm.exportTo(context, it) { msg -> feedbackMessage = msg } } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { vm.importFrom(context, it) { msg -> feedbackMessage = msg } } }

    val importPortableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            vm.importPortableFrom(context, it) { msg -> feedbackMessage = msg }
        }
    }

    val readCalendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (PermissionRequestPolicy.calendarPermissionsGranted(results)) vm.setCalendarSyncEnabled(true)
        else feedbackMessage = context.getString(R.string.settings_no_calendar_permission)
    }

    LaunchedEffect(feedbackMessage) {
        val message = feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        feedbackMessage = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ── Navigation bar ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Large title ──────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_title),
                style = AppType.largeTitle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            // ── Card 1: Reminders ────────────────────────────────────
            SectionCard {
                SettingRow(
                    title = stringResource(R.string.settings_default_remind_time),
                    value = "%02d:%02d".format(state.defaultRemindHour, state.defaultRemindMinute),
                    onClick = { showRemindTimePicker = true }
                )
                InsetDivider()
                SettingRow(
                    title = stringResource(R.string.settings_default_hours_before),
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { vm.setDefaultHoursBefore(state.defaultHoursBefore - 1) }) {
                                Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.settings_hours_unit, state.defaultHoursBefore),
                                style = AppType.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { vm.setDefaultHoursBefore(state.defaultHoursBefore + 1) }) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Card 2: Calendar Sync ────────────────────────────────
            SectionCard {
                SwitchRow(
                    title = stringResource(R.string.settings_calendar_sync),
                    subtitle = stringResource(R.string.settings_calendar_sync_desc),
                    checked = state.calendarSyncEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) readCalendarLauncher.launch(PermissionRequestPolicy.calendarPermissions())
                        else vm.setCalendarSyncEnabled(false)
                    }
                )
                InsetDivider()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    OutlinedTextField(
                        value = state.calendarAccountName,
                        onValueChange = vm::setCalendarAccount,
                        label = { Text(stringResource(R.string.settings_calendar_account), style = AppType.footnote) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { vm.syncCalendarNow(context) { feedbackMessage = it } }) {
                        Text(stringResource(R.string.settings_sync_now), color = AppColors.Brand)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Card 3: Quick Add ────────────────────────────────────
            SectionCard {
                SwitchRow(
                    title = stringResource(R.string.settings_quick_add_notif),
                    subtitle = stringResource(R.string.settings_quick_add_desc),
                    checked = state.quickAddNotifEnabled,
                    onCheckedChange = vm::setQuickAddNotif
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Card 4: Data ─────────────────────────────────────────
            SectionCard {
                ActionRow(
                    title = stringResource(R.string.settings_export),
                    onClick = { exportLauncher.launch("lighttodo-backup.json") }
                )
                InsetDivider()
                ActionRow(
                    title = stringResource(R.string.settings_import),
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                )
                InsetDivider()
                ActionRow(
                    title = stringResource(R.string.settings_import_portable),
                    onClick = { importPortableLauncher.launch(null) }
                )
                if (BuildConfig.DEBUG) {
                    InsetDivider()
                    ActionRow(
                        title = stringResource(R.string.settings_save_db_snapshot),
                        onClick = { vm.exportDatabaseSnapshot(context) { feedbackMessage = it } }
                    )
                }
                InsetDivider()
                ActionRow(
                    title = stringResource(R.string.settings_clear_done),
                    onClick = { showClearConfirm = true },
                    destructive = true
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // ── Default reminder time picker ─────────────────────────────
    if (showRemindTimePicker) {
        com.zahri.lighttodo.feature.todoedit.WheelTimePickerDialog(
            title = stringResource(R.string.settings_default_remind_time_short),
            initialHour = state.defaultRemindHour,
            initialMinute = state.defaultRemindMinute,
            onConfirm = { h, m ->
                vm.setDefaultRemind(h, m)
                showRemindTimePicker = false
            },
            onDismiss = { showRemindTimePicker = false }
        )
    }

    // ── Clear confirmation dialog ────────────────────────────────
    if (showClearConfirm) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showClearConfirm = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp)
            ) {
                Text(
                    stringResource(R.string.settings_clear_done_confirm_title),
                    style = AppType.title3,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_clear_done_confirm_msg),
                    style = AppType.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = { showClearConfirm = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            stringResource(R.string.settings_clear_done_cancel),
                            style = AppType.headline,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(
                        onClick = {
                            showClearConfirm = false
                            vm.clearDone { feedbackMessage = it }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.Overdue)
                    ) {
                        Text(
                            stringResource(R.string.settings_clear_done_confirm),
                            style = AppType.headline,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ─── Reusable components ─────────────────────────────────────────

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column { content() }
    }
}

@Composable
private fun InsetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )
}

@Composable
private fun SettingRow(
    title: String,
    value: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = AppType.body,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            if (value != null) {
                Text(value, style = AppType.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
            }
            trailing()
        } else if (value != null) {
            Text(value, style = AppType.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.height(18.dp)
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AppType.body, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = AppType.footnote, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.DoneGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun ActionRow(title: String, onClick: () -> Unit, destructive: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = AppType.body,
            color = if (destructive) AppColors.Overdue else AppColors.Brand
        )
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.height(18.dp)
        )
    }
}

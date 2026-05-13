package com.zahri.lighttodo.ui.settings

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val toast = remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { vm.exportTo(context, it) { msg -> toast.value = msg } } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { vm.importFrom(context, it) { msg -> toast.value = msg } } }

    val readCalendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.setCalendarSyncEnabled(true)
        else toast.value = "未授予日历权限，无法开启同步"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Default remind time
            SettingRow(
                title = "默认提醒时间（无截止时间的任务）",
                subtitle = "%02d:%02d".format(state.defaultRemindHour, state.defaultRemindMinute),
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m -> vm.setDefaultRemind(h, m) },
                        state.defaultRemindHour, state.defaultRemindMinute, true
                    ).show()
                }
            )
            HorizontalDivider()

            SettingRow(
                title = "默认提前小时数（有截止时间的任务）",
                subtitle = "${state.defaultHoursBefore} 小时",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { vm.setDefaultHoursBefore(state.defaultHoursBefore - 1) }) { Text("-") }
                        TextButton(onClick = { vm.setDefaultHoursBefore(state.defaultHoursBefore + 1) }) { Text("+") }
                    }
                }
            )
            HorizontalDivider()

            // Calendar sync
            SwitchRow(
                title = "同步小米日历（只读）",
                subtitle = "拉取本机系统日历，过滤节日和假期",
                checked = state.calendarSyncEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        readCalendarLauncher.launch(android.Manifest.permission.READ_CALENDAR)
                    } else {
                        vm.setCalendarSyncEnabled(false)
                    }
                }
            )
            OutlinedTextField(
                value = state.calendarAccountName,
                onValueChange = vm::setCalendarAccount,
                label = { Text("账户筛选（留空则匹配 xiaomi 关键字）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { vm.syncCalendarNow(context) { toast.value = it } }) {
                Text("立即同步一次")
            }
            HorizontalDivider()

            SwitchRow(
                title = "通知栏常驻快速添加",
                subtitle = "下拉通知栏即可一键写入待办",
                checked = state.quickAddNotifEnabled,
                onCheckedChange = vm::setQuickAddNotif
            )
            HorizontalDivider()

            Button(onClick = { exportLauncher.launch("lighttodo-backup.json") }, modifier = Modifier.fillMaxWidth()) {
                Text("导出 JSON")
            }
            Button(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("导入 JSON")
            }
            Button(onClick = { vm.clearDone() { toast.value = it } }, modifier = Modifier.fillMaxWidth()) {
                Text("清空已完成任务")
            }

            Spacer(Modifier.height(24.dp))
            toast.value?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

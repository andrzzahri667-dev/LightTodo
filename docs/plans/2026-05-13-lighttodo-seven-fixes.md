# LightTodo 七项修复 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复 LightTodo 原型的 7 个问题 + MIUI Widget 适配：widget 尺寸、widget 数据加载、日历同步、键盘唤起、widget 箭头、日期选择器、全屏提醒。

**Architecture:** 在现有 Kotlin + Compose + Room 架构上做 surgical fixes。widget 适配 MIUI 规范（独立进程、曝光刷新、统一圆角）。日历同步修改过滤逻辑。提醒系统加入 fullScreenIntent。

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Room, AlarmManager, RemoteViews

**MIUI 关键规范参考：**
- Widget 必须 `minWidth/minHeight = 110dp` (2x2)
- Widget receiver 和 service 运行在 `:widgetProvider` 独立进程
- 必须声明 `miuiWidget` meta-data
- 必须声明 `miuiWidgetVersion` 在 `<application>` 下
- 根布局必须 `android:id="@android:id/background"` + 非透明背景色
- 使用曝光刷新替代系统定时刷新
- 根布局宽高必须 `match_parent`

---

## Task 1: MIUI Widget 适配（Manifest + 布局 + 尺寸）

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/xml/widget_info.xml`
- Modify: `app/src/main/res/layout/widget_2x2.xml`

**Step 1: 修改 AndroidManifest.xml — Widget 部分适配 MIUI**

1. 在 `<application>` 标签内添加 widget 版本号（在 `</activity>` 之后，widget receiver 之前）：

```xml
<!-- MIUI Widget 版本号 -->
<meta-data
    android:name="miuiWidgetVersion"
    android:value="1" />
```

2. 修改 widget receiver，添加 MIUI 标识、独立进程、label、曝光刷新 meta-data 和 intent-filter：

```xml
<!-- Widget provider (MIUI adapted) -->
<receiver
    android:name=".widget.TodoWidgetProvider"
    android:exported="true"
    android:label="@string/widget_title"
    android:process=":widgetProvider">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
        <action android:name="miui.appwidget.action.APPWIDGET_UPDATE" />
        <action android:name="com.zahri.lighttodo.WIDGET_TOGGLE_DONE" />
        <action android:name="com.zahri.lighttodo.WIDGET_ITEM_CLICK" />
        <action android:name="com.zahri.lighttodo.WIDGET_REFRESH" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/widget_info" />
    <meta-data
        android:name="miuiWidget"
        android:value="true" />
    <meta-data
        android:name="miuiWidgetRefresh"
        android:value="exposure" />
    <meta-data
        android:name="miuiWidgetRefreshMinInterval"
        android:value="30000" />
</receiver>
```

3. 修改 widget service，添加独立进程：

```xml
<service
    android:name=".widget.TodoWidgetService"
    android:exported="false"
    android:process=":widgetProvider"
    android:permission="android.permission.BIND_REMOTEVIEWS" />
```

**Step 2: 修改 widget_info.xml — 尺寸修正**

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="110dp"
    android:minHeight="110dp"
    android:targetCellWidth="2"
    android:targetCellHeight="2"
    android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/widget_2x2"
    android:resizeMode="none"
    android:widgetCategory="home_screen"
    android:previewLayout="@layout/widget_2x2"
    android:description="@string/app_name" />
```

**Step 3: 修改 widget_2x2.xml — MIUI 根布局要求**

根布局必须 `android:id="@android:id/background"` + 非透明背景色 + `match_parent`。

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@android:id/background"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/widget_bg"
    android:padding="6dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@drawable/widget_background"
        android:orientation="vertical"
        android:padding="14dp">

        <TextView
            android:id="@+id/widget_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginBottom="6dp"
            android:text="@string/widget_title"
            android:textColor="@color/widget_text_primary"
            android:textSize="17sp"
            android:textStyle="bold" />

        <ListView
            android:id="@+id/widget_list"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:divider="@null"
            android:dividerHeight="0dp"
            android:scrollbars="none" />

        <TextView
            android:id="@+id/widget_empty"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:gravity="center"
            android:text="@string/widget_empty"
            android:textColor="@color/widget_text_secondary"
            android:textSize="13sp"
            android:visibility="gone" />
    </LinearLayout>
</FrameLayout>
```

**Step 4: 在 TodoWidgetProvider.onReceive 中处理 MIUI 曝光刷新广播**

在 `onReceive` 中添加 MIUI 曝光刷新的处理：

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
        "miui.appwidget.action.APPWIDGET_UPDATE" -> {
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (ids != null) {
                onUpdate(context, AppWidgetManager.getInstance(context), ids)
            }
        }
        ACTION_ITEM_CLICK -> {
            // ... existing code unchanged
        }
        ACTION_REFRESH -> notifyAllWidgetsDataChanged(context)
        else -> super.onReceive(context, intent)
    }
}
```

**Step 5: Build 验证**

Run: `cd /home/pipo/first && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/res/xml/widget_info.xml app/src/main/res/layout/widget_2x2.xml app/src/main/java/com/zahri/lighttodo/widget/TodoWidgetProvider.kt
git commit -m "feat: MIUI widget adaptation - independent process, exposure refresh, correct sizing"
```

---

## Task 2: 修复 Widget 无法加载待办

**Files:**
- Modify: `app/src/main/java/com/zahri/lighttodo/data/Daos.kt`
- Modify: `app/src/main/java/com/zahri/lighttodo/widget/TodoWidgetService.kt`

**Step 1: 在 Daos.kt 添加新查询**

在 `TodoDao` 接口中，在 `listDueByDaySync` 方法后面添加：

```kotlin
/** Widget：所有未完成任务，按时间排序（过期排前面） */
@Query("SELECT * FROM todo WHERE done = 0 ORDER BY dateMillis ASC, createdAtMillis ASC LIMIT :limit")
fun listAllUndoneSync(limit: Int): List<TodoEntity>
```

**Step 2: 修改 TodoWidgetService.onDataSetChanged**

```kotlin
override fun onDataSetChanged() {
    val app = context.applicationContext as App
    items = app.db.todoDao().listAllUndoneSync(limit = 6)
}
```

删除不再使用的 `DateUtils` import（`endOfDayMillis` 和 `LocalDate` 如果只有此处使用）。

**Step 3: Build 验证**

Run: `cd /home/pipo/first && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/com/zahri/lighttodo/data/Daos.kt app/src/main/java/com/zahri/lighttodo/widget/TodoWidgetService.kt
git commit -m "fix: widget loads all undone tasks instead of only today+overdue"
```

---

## Task 3: 修复日历同步无法拉取小米日历

**Files:**
- Modify: `app/src/main/java/com/zahri/lighttodo/calendar/CalendarSync.kt`

**Step 1: 删除 LOCAL 类型过滤，放宽账户匹配**

修改 `pickCalendarIds`：删除 `acctType == LOCAL` 过滤（小米日历就是 LOCAL 类型）。

修改后的 `pickCalendarIds`：

```kotlin
private fun pickCalendarIds(context: Context, userFilter: String): List<Long> {
    val cr = context.contentResolver
    val cursor = cr.query(
        CalendarContract.Calendars.CONTENT_URI,
        arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        ), null, null, null
    ) ?: return emptyList()

    val ids = mutableListOf<Long>()
    cursor.use {
        while (it.moveToNext()) {
            val id = it.getLong(0)
            val acctName = it.getString(1).orEmpty()
            val displayName = it.getString(3).orEmpty()

            // Skip Holidays / festivals
            if (EXCLUDED_NAME_KEYWORDS.any { kw -> displayName.contains(kw, ignoreCase = true) }) continue

            if (matchesAccount(acctName, userFilter)) {
                ids += id
            }
        }
    }
    return ids
}
```

修改后的 `matchesAccount`（用户未指定过滤时匹配所有）：

```kotlin
private fun matchesAccount(acctName: String, userFilter: String): Boolean {
    val f = userFilter.trim()
    return if (f.isNotEmpty()) {
        acctName.equals(f, ignoreCase = true) || acctName.contains(f, ignoreCase = true)
    } else {
        // 无过滤条件时匹配所有非节日日历
        true
    }
}
```

**Step 2: Build 验证**

Run: `cd /home/pipo/first && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/zahri/lighttodo/calendar/CalendarSync.kt
git commit -m "fix: calendar sync now matches local accounts and all calendars by default"
```

---

## Task 4: 修复快速添加无法唤起键盘

**Files:**
- Modify: `app/src/main/java/com/zahri/lighttodo/notify/QuickAddActivity.kt`

**Step 1: 在 onCreate 中设置 softInputMode**

在 `QuickAddActivity.onCreate` 的 `super.onCreate()` 之后、`setContent` 之前加一行：

```kotlin
window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
```

这是最可靠的方案。保留 `focus.requestFocus()` 让 TextField 获得焦点（已有代码不动）。

**Step 2: Build 验证**

Run: `cd /home/pipo/first && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add app/src/main/java/com/zahri/lighttodo/notify/QuickAddActivity.kt
git commit -m "fix: show soft keyboard when quick-add dialog opens"
```

---

## Task 5: 移除 Widget 右上角箭头，改为点击空白区域打开 App

**Files:**
- Modify: `app/src/main/java/com/zahri/lighttodo/widget/TodoWidgetProvider.kt`
- Delete: `app/src/main/res/drawable/widget_arrow.xml`
- Delete: `app/src/main/res/drawable/widget_arrow_bg.xml`

注意：`widget_2x2.xml` 已在 Task 1 中移除了箭头 ImageView，无需再改。

**Step 1: 修改 TodoWidgetProvider.kt**

将原来绑定在 `widget_open_app` 和 `widget_title` 上的 `openAppPi`，改为绑定在根布局 `android.R.id.background` 上（MIUI 根布局的 ID）：

```kotlin
// open app on background tap (any area not covered by list items)
val openAppPi = PendingIntent.getActivity(
    context, 0,
    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
views.setOnClickPendingIntent(android.R.id.background, openAppPi)
```

删除原来的 `views.setOnClickPendingIntent(R.id.widget_open_app, ...)` 和 `views.setOnClickPendingIntent(R.id.widget_title, ...)`。

注意：ListView 的 item 点击（通过 `setPendingIntentTemplate`）优先级高于 root 的 `setOnClickPendingIntent`，所以点击列表项和 checkbox 不会触发打开 app。

**Step 2: 删除不再使用的 drawable 文件**

```bash
rm app/src/main/res/drawable/widget_arrow.xml
rm app/src/main/res/drawable/widget_arrow_bg.xml
```

**Step 3: Build 验证**

Run: `cd /home/pipo/first && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add -A
git commit -m "fix: remove widget arrow, tap empty area to open app"
```

---

## Task 6: 替换日期时间选择器为 Material3 组件

**Files:**
- Modify: `app/src/main/java/com/zahri/lighttodo/ui/edit/EditScreen.kt`

**Step 1: 替换 imports**

删除：
```kotlin
import android.app.DatePickerDialog
import android.app.TimePickerDialog
```

添加：
```kotlin
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
```

**Step 2: 添加 state 变量**

在 EditScreen composable 中，现有 state 声明附近添加：

```kotlin
var showDatePicker by remember { mutableStateOf(false) }
var showTimePicker by remember { mutableStateOf(false) }
```

**Step 3: 替换日期 AssistChip 的 onClick**

```kotlin
AssistChip(
    enabled = !state.readOnly,
    onClick = { showDatePicker = true },
    label = { Text("%d 年 %02d 月 %02d 日".format(state.date.year, state.date.monthValue, state.date.dayOfMonth)) }
)
```

**Step 4: 替换截止时间 AssistChip 的 onClick**

```kotlin
AssistChip(
    enabled = !state.readOnly,
    onClick = { showTimePicker = true },
    label = { Text(state.deadline?.let { "%02d:%02d".format(it.first, it.second) } ?: "全天") }
)
```

**Step 5: 在 Scaffold content 末尾添加 DatePicker 和 TimePicker dialogs**

在 Column 的闭合括号之后、padding lambda 闭合之前添加：

```kotlin
if (showDatePicker) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = { showDatePicker = false },
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val localDate = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    vm.setDate(localDate.year, localDate.monthValue, localDate.dayOfMonth)
                }
                showDatePicker = false
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = { showDatePicker = false }) { Text("取消") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

if (showTimePicker) {
    val (initH, initM) = state.deadline ?: (9 to 0)
    val timePickerState = rememberTimePickerState(initialHour = initH, initialMinute = initM, is24Hour = true)
    DatePickerDialog(
        onDismissRequest = { showTimePicker = false },
        confirmButton = {
            TextButton(onClick = {
                vm.setDeadline(timePickerState.hour, timePickerState.minute)
                showTimePicker = false
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = { showTimePicker = false }) { Text("取消") }
        }
    ) {
        TimePicker(state = timePickerState)
    }
}
```

**Step 6: Build 验证**

Run: `cd /home/pipo/first && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add app/src/main/java/com/zahri/lighttodo/ui/edit/EditScreen.kt
git commit -m "feat: replace native date/time pickers with Material3 components"
```

---

## Task 7: 提醒全屏弹出

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/zahri/lighttodo/notify/ReminderActivity.kt`
- Modify: `app/src/main/java/com/zahri/lighttodo/notify/ReminderReceiver.kt`
- Modify: `app/src/main/res/values/themes.xml`

**Step 1: 添加权限和 Activity 声明到 AndroidManifest.xml**

在 `<manifest>` 的权限区域添加：

```xml
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
```

在 `<application>` 中，`QuickAddActivity` 声明之后添加：

```xml
<!-- Full-screen reminder activity -->
<activity
    android:name=".notify.ReminderActivity"
    android:exported="false"
    android:excludeFromRecents="true"
    android:launchMode="singleInstance"
    android:taskAffinity=""
    android:showOnLockScreen="true"
    android:turnScreenOn="true"
    android:theme="@style/Theme.LightTodo.Reminder" />
```

**Step 2: 创建 ReminderActivity 主题**

在 `app/src/main/res/values/themes.xml` 中添加：

```xml
<style name="Theme.LightTodo.Reminder" parent="android:Theme.Material.Light.NoActionBar">
    <item name="android:windowIsTranslucent">true</item>
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:colorBackgroundCacheHint">@null</item>
    <item name="android:windowIsFloating">false</item>
    <item name="android:backgroundDimEnabled">true</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
</style>
```

**Step 3: 创建 ReminderActivity.kt**

```kotlin
package com.zahri.lighttodo.notify

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.zahri.lighttodo.App
import com.zahri.lighttodo.MainActivity
import com.zahri.lighttodo.ui.home.displayTitle
import com.zahri.lighttodo.ui.theme.AppColors
import com.zahri.lighttodo.ui.theme.LightTodoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        if (todoId <= 0) { finish(); return }

        val app = applicationContext as App
        var todoTitle = "待办提醒"
        var todoNote = ""
        lifecycleScope.launch(Dispatchers.IO) {
            val todo = app.db.todoDao().findById(todoId) ?: return@launch
            todoTitle = todo.displayTitle()
            todoNote = todo.note?.lineSequence()?.firstOrNull().orEmpty()
        }.invokeOnCompletion {
            setContent {
                LightTodoTheme {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0x80000000))
                            .clickable { /* dismiss */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "任务提醒",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    todoTitle,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (todoNote.isNotBlank()) {
                                    Text(
                                        todoNote,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                app.repository.setDone(todoId, true)
                                            }
                                            finish()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AppColors.Brand,
                                            contentColor = Color.Black
                                        )
                                    ) { Text("完成") }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            startActivity(
                                                android.content.Intent(
                                                    this@ReminderActivity,
                                                    MainActivity::class.java
                                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                            finish()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) { Text("查看") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_TODO_ID = "todo_id"
    }
}
```

**Step 4: 修改 ReminderReceiver 使用 fullScreenIntent**

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    val id = ReminderScheduler.extractTodoId(intent) ?: return
    val pending = goAsync()
    runBlocking(Dispatchers.IO) {
        try {
            val app = context.applicationContext as App
            val todo = app.db.todoDao().findById(id) ?: return@runBlocking
            if (todo.done) return@runBlocking

            val fullScreenIntent = Intent(context, ReminderActivity::class.java).apply {
                putExtra(ReminderActivity.EXTRA_TODO_ID, id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val fullScreenPi = PendingIntent.getActivity(
                context, id.toInt(), fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val contentPi = PendingIntent.getActivity(
                context, id.toInt(),
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notif = NotificationCompat.Builder(context, NotificationChannels.REMINDER_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(todo.displayTitle())
                .setContentText(buildSubtitle(todo))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(contentPi)
                .setFullScreenIntent(fullScreenPi, true)
                .build()
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.notify(id.toInt(), notif)
        } finally {
            pending.finish()
        }
    }
}
```

需要添加 import：`import android.app.PendingIntent`（如果不存在）。

**Step 5: NotificationChannel 确认**

当前 `NotificationChannels.kt` 的 reminder channel 已是 `IMPORTANCE_HIGH`，无需修改。

**Step 6: Build 验证**

Run: `cd /home/pipo/first && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/zahri/lighttodo/notify/ReminderActivity.kt app/src/main/java/com/zahri/lighttodo/notify/ReminderReceiver.kt app/src/main/res/values/themes.xml
git commit -m "feat: full-screen reminder popup for task alerts"
```

---

## 执行状态

### ✅ Task 1: MIUI Widget 适配 — 已完成

**提交:** `43f9d7b`

**实际改动（与 plan 有偏差）：**
- AndroidManifest.xml: 加了 `:widgetProvider` 独立进程、`miuiWidgetRefresh=exposure`、`miuiWidgetRefreshMinInterval=30000`
- **去掉了 `miuiWidget=true` meta-data** — 加了之后 MIUI 小组件列表里找不到这个 widget（未提交审核的 widget 不能声明此标识）
- **去掉了 `miuiWidgetVersion`** — 同理，配合 `miuiWidget=true` 用的
- widget_info.xml: `minWidth/minHeight=110dp`, `targetCellWidth/Height=2`
- widget_2x2.xml: 根布局 `@android:id/background`，不透明背景
- TodoWidgetProvider.kt: 处理 `miui.appwidget.action.APPWIDGET_UPDATE` 曝光刷新广播

### ✅ Task 2: 修复 Widget 无法加载待办 — 已完成

**提交:** `a01b005`

**额外修复:**
- **`widget_item.xml` 第 10 行 `<View>` 改为 `<ImageView>`** — RemoteViews 不允许 `android.view.View`，这是 widget 加载失败的直接原因（报错 `Class not allowed to be inflated android.view.View`）
- TodoWidgetService.kt: 用 `listAllUndoneSync(limit=6)` 替代 `listDueByDaySync`
- Daos.kt: 新增 `listAllUndoneSync` 查询

### ✅ Task 3: 修复日历同步无法拉取小米日历 — 已完成

**提交:** `3b36515`

与 plan 一致，无偏差。

### ✅ Task 5: 移除 Widget 右上角箭头 — 已完成

**提交:** `43f9d7b`（与 Task 1 合并）+ `b4f1f3d`（删除 drawable）

**实际改动：**
- widget_2x2.xml 已在 Task 1 中移除箭头 ImageView
- `setOnClickPendingIntent` 改为绑定在 `android.R.id.background`（根布局）上
- 删除 `widget_arrow.xml` 和 `widget_arrow_bg.xml`

### ✅ Widget 正方形修复 — 计划外

**提交:** `8a27997` + `294f153`

**问题：** MIUI 2×2 网格格子不是正方形（实测 475×626px），widget 跟着变成竖矩形。

**方案尝试：**
- `minWidth/minHeight=110dp` + `targetCellWidth/Height=2` — 不是正方形
- `minWidth/minHeight=180dp` — 跳到 3×3
- ConstraintLayout `dimensionRatio="1:1"` — ❌ MIUI RemoteViews 不允许 ConstraintLayout（`Class not allowed to be inflated`）
- **最终方案：代码动态 `setViewLayoutWidth/Height`（API 31+）取短边强制正方形**

**改动文件：**
- `widget_2x2.xml`: FrameLayout 套 LinearLayout（id=square_container），`layout_gravity="center_horizontal|top"`
- `TodoWidgetProvider.kt`: `onAppWidgetOptionsChanged` 获取实际 dp 尺寸，用 `setViewLayoutWidth/Height` 把 square_container 限制为短边长度
- `widget_background.xml`: 改为不透明 `#FF1E2226`
- `build.gradle.kts`: 添加 `androidx.constraintlayout:constraintlayout:2.1.4`（虽然最终没用上，依赖保留）

---

### ❌ Task 4: 修复快速添加无法唤起键盘 — 未开始

按 plan 执行即可，无已知问题。

### ❌ Task 6: 替换日期时间选择器为 Material3 组件 — 未开始

按 plan 执行即可，无已知问题。注意需确认 Material3 DatePicker/TimePicker 在当前 compose-bom 版本（2024.06.00）中可用。

### ❌ Task 7: 提醒全屏弹出 — 未开始

按 plan 执行即可，无已知问题。`ReminderActivity.kt` 需新建。


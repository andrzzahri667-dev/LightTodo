# 轻待办 (LightTodo)

纯本地的 Android 待办应用，自用。

## 功能

- 待办管理：标签分组、日期、开始/截止时间、备注
- 提醒：AlarmManager 精确闹钟，支持提前 N 小时
- 桌面小组件：2×2 暗色卡片，直接勾选完成
- 日历同步：单向只读拉取系统日历事件
- 通知栏快速添加
- 自动备份/恢复（本地 JSON）
- 批量选择删除
- 暗色主题跟随系统

## 技术栈

- Kotlin + Jetpack Compose
- Room + DataStore
- minSdk 26 / targetSdk 34

## 构建

```bash
./gradlew :app:assembleDebug
```

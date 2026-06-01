🔴 HIGH — 必须修复

  ~~HIGH-1: 双 LaunchedEffect 竞态条件（快速返回时动画跳变）~~

  文件: ui/motion/components/MotionNoteEditorTransformHost.kt:62-81

  两个 LaunchedEffect 共享相同 key (canRender, exitRequested)，各自驱动同一个 progress Animatable。快速在入场动画期间按返回键时，两个 effect 同时重启，动画从中间值跳变。

  // 当前：两个独立的 effect
  LaunchedEffect(canRender, exitRequested) { /* entry */ }
  LaunchedEffect(exitRequested, canRender) { /* exit */ }

  建议: 合并为单个 effect，明确状态机：

  LaunchedEffect(canRender, exitRequested) {
      if (!canRender) return@LaunchedEffect
      if (exitRequested) {
          progress.animateTo(0f, NoteEditorContainerTransformPolicy.exitTween())
          onExitFinished()
      } else if (!entryPlayed) {
          progress.animateTo(1f, NoteEditorContainerTransformPolicy.entryTween())
          entryPlayed = true
      }
  }

 ~~HIGH-3: mirrorTodoToCalendar 在调用者线程执行 ContentProvider I/O~~

  文件: data/Repository.kt:258-273

  mirrorTodoToCalendar 是普通函数（非 suspend），直接调用 ContentResolver.insert/update。当前所有调用方碰巧都在 IO dispatcher 上，但没有显式保证。未来如果调用路径改变，可能阻塞主线程。

  建议: 改为 suspend 或在入口处加 withContext(Dispatchers.IO)。

  ~~HIGH-4: Source tests 中 sourceFile() 重复 15+ 次~~

  文件: 每个 *SourceTest.kt

  完全相同的 7 行 sourceFile() 函数被复制到 15+ 个测试文件。任何修改都需要改 15 处。

  建议: 提取到共享测试工具类：

  // app/src/test/java/com/zahri/lighttodo/test/SourceFiles.kt
  package com.zahri.lighttodo.test

  fun sourceFile(relativePath: String): File {
      val userDir = requireNotNull(System.getProperty("user.dir"))
      var dir = File(userDir).absoluteFile
      while (true) {
          val candidate = File(dir, relativePath)
          if (candidate.exists()) return candidate
          dir = dir.parentFile ?: break
      }
      error("Could not find $relativePath from $userDir")
  }

  ---
  🟡 MEDIUM — 建议修复

  ┌─────┬───────────────────────────────────────────────────────────────────┬──────────────────────────────────┬───────────────────────────────────────────────────────┐
  │  #  │                               问题                                │               文件               │                         建议                          │
  ├─────┼───────────────────────────────────────────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ ~~M1~~  │ ~~孤儿清理删除 app 创建的 todo（第三方删了日历事件→本地 todo 被删）~~ │ ~~CalendarSync.kt:58-64~~            │ ~~对 calendarCreatedByApp=true 的孤儿，清除链接而非删除~~ │
  ├─────┼───────────────────────────────────────────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ ~~M2~~  │ ~~setDoneLocked 多做一次 DB 读取~~                                    │ ~~Repository.kt:143-153~~            │ ~~复用已有的 updated 实体，省一次查询~~                   │
  ├─────┼───────────────────────────────────────────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ ~~M3~~  │ ~~AppMotionEngine / AppMotionEnginePolicy 是死代码~~                  │ ~~AppMotionEngine.kt~~               │ ~~从本分支删除，用到时再加~~                              │
  ├─────┼───────────────────────────────────────────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ ~~M4~~  │ ~~Lottie 未加载时显示文字 "✓" 导致闪烁~~                              │ ~~TodoCompletionIndicator.kt:59-68~~ │ ~~未加载时返回空 Box，或预加载 composition~~              │
  ├─────┼───────────────────────────────────────────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ ~~M5~~  │ ~~MotionSectionVisibility + animateItem 双动画系统冲突~~              │ ~~HomeTodoPage.kt:122-141~~          │ ~~列表 item 仅保留 placement，section 负责 enter/exit~~                         │
  ├─────┼───────────────────────────────────────────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ ~~M6~~  │ ~~MotionScroll.kt 的单行包装函数无价值~~                              │ ~~MotionScroll.kt~~                  │ ~~删除，调用方直接使用 Compose scroll API~~                                  │
  ├─────┼───────────────────────────────────────────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ ~~M7~~  │ ~~DatabaseSnapshotExporter 硬编码数据库名~~                           │ ~~DatabaseSnapshotExporter.kt:17~~   │ ~~恢复使用 AppDatabase.databaseName 常量~~                │
  ├─────┼───────────────────────────────────────────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ ~~M8~~  │ ~~WidgetUpdateDebouncer 进程重启后持有过期 scope~~                    │ ~~TodoWidgetProvider.kt~~            │ ~~每次 notifyAllWidgetsDataChanged 重建 debouncer~~       │
  ├─────┼───────────────────────────────────────────────────────────────────┼──────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ ~~M9~~  │ ~~BackupDtoMapper 不导出 calendarCreatedByApp~~                       │ ~~BackupDtoMapper.kt~~               │ ~~功能影响为零（恢复时链接已清除），但语义不准确~~        │
  └─────┴───────────────────────────────────────────────────────────────────┴──────────────────────────────────┴───────────────────────────────────────────────────────┘

  ---
  🟢 LOW — 可后续处理

- NoteEditorContainerTransformEasing 包装类多余，直接用 CubicBezierEasing
- NoteSourceVisibilityMotion 只是对 AppMotion 常量的别名，可直接引用
- WheelPickerMotionPolicy 中 0.28f、0.42f 等魔法数字应收为命名常量
- matchesAccount 忽略 acctType 参数，应加 TODO 或删除
- Source tests 基于字符串 contains() 断言，任何无关注释修改都可能导致误报
- 180ms widget debounce 偏短，如仍有冗余刷新可调至 300-500ms

---
后续执行计划

P1: 收口剩余 note 动画边界

- 目标: 把仍散落在 `ui.note` 的启动/退出动画策略统一迁入 `ui.motion`
- 涉及文件:
  - `ui/note/NoteLaunchAnimationPolicy.kt`
  - `ui/note/NoteEditorLaunchAnimationModePolicy.kt`
  - `ui/note/NoteScaleDownUpdatePolicy.kt`
  - `ui/note/NoteSourceVisibilityCallbackPolicy.kt`
  - `ui/note/MiuiScaleUpDownOptions.kt`
- 原则:
  - 视觉与编辑逻辑留在 `ui.note`
  - 动画策略、平台动画适配器、motion spec 全部归口到 `ui.motion`
  - `MiuiScaleUpDownOptions` 倾向迁到 `ui.motion.platform`

P2: 增加动画架构护栏测试

- 目标: 防止后续继续在业务页面手写动画 primitive
- 做法:
  - 新增 source test，限制 `ui.motion` 之外直接使用 `Animatable`、`graphicsLayer`、`AnimatedVisibility`、`tween`、`spring`、`MotionLayout`
  - 对确有必要的例外走白名单，不允许隐式扩散
- 结果:
  - 后续动画改动必须先进入统一层，再被页面调用

P3: 修列表展开/收起动画

- 目标: 解决已完成列表收起时“直接消失”、缺少过渡的问题
- 做法:
  - 统一收敛到 `MotionSectionVisibility` 或新的 section collapse motion
  - 同时处理高度、透明度、item placement，避免只做 alpha
  - 校验 `animateItem` 与 section visibility 叠加时是否出现抖动

P4: 优化时间/滚轮 proximity 动画

- 目标: 从二元 hover/选中，改成连续距离响应
- 做法:
  - 根据 item 与中心点距离连续计算 scale、alpha、颜色权重
  - 参数统一收进 `WheelPickerMotionPolicy` / `AppMotion`
  - 控制 recomposition 粒度，避免 proximity 计算拖慢滚动

P5: 继续统一 motion spec

- 目标: 把时长、曲线、scale、stagger 继续集中管理
- 做法:
  - 页面转场、列表折叠、滚轮接近度都引用 `AppMotion`
  - 删除已经失去价值的轻包装和重复常量

P6: 验证与提交策略

- 验证:
  - `./gradlew testDebugUnitTest`
  - `./gradlew :app:assembleDebug`
- 提交拆分:
  - `refactor(motion): move note launch policies`
  - `test(motion): guard animation ownership`
  - `fix(motion): animate collapsed list sections`
  - `refactor(motion): tune wheel proximity motion`

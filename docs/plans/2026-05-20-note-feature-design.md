# Note 功能设计文档

## 概述

为 LightTodo 增加笔记功能。首页通过 HorizontalPager 左右滑动切换 Note（Page 0）和 Todo（Page 1）两个页面。笔记支持 Markdown 所见即所得编辑，卡片网格展示。

## 数据层

### NoteEntity

```kotlin
@Entity(tableName = "note",
    foreignKeys = [ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("tagId"), Index("updatedAtMillis")])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String? = null,
    val content: String = "",
    val tagId: Long? = null,         // 预留，当前不暴露 UI
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
```

### Migration v3 → v4

新增 `note` 表，不影响现有数据。

### 备份恢复

`BackupBundle.version` 升为 2，增加 `notes: List<BackupNote>` 字段。读取时 `ignoreUnknownKeys = true` 保证向前兼容。

## 首页改造

- `HomeScreen` 内部使用 `HorizontalPager`（2 页）
- Page 0：Note 卡片网格
- Page 1：Todo 列表（现有内容）
- 页面索引通过常量定义，不硬编码
- 顶部加指示器（两个圆点）
- FAB 跟随当前页切换行为：Note 页新建笔记，Todo 页新建待办
- Settings 齿轮图标两页共享

## Note 卡片页

- `LazyVerticalStaggeredGrid`，2 列
- 卡片固定高度，内容超出截断
- 卡片显示：标题（如有）+ Markdown 渲染预览
- 不显示时间，排序按 `updatedAtMillis DESC`（最近编辑在最前）
- 点击 → 进入编辑页
- 长按 → 多选模式，支持批量删除

## Markdown 所见即所得

### 当前支持语法

- 标题：`#` `##` `###`
- 加粗：`**text**`
- 删除线：`~~text~~`
- 无序列表：`- item`
- 引用：`> text`
- 行内代码：`` `code` ``
- 链接：`[text](url)` 渲染为可点击蓝色文字
- 粘贴纯 URL 自动包装为 `[url](url)`

### 实现方案

纯 Compose 实现，`BasicTextField` + `AnnotatedString`，不引入外部库。逐行解析 Markdown token，映射为 `SpanStyle` / `ParagraphStyle`。

### TODO（后续迭代）

- 图片插入（拍照 + 相册，本地存储）
- 录音插入 + 播放

## 笔记编辑页

- 路由：`note_edit`（新建）、`note_edit?id={id}`（编辑）
- 顶部：返回 + 删除按钮
- 标题输入框（可选）
- Markdown 所见即所得内容编辑区
- 滚动到底部显示创建时间 + 更新时间
- 自动保存：失焦/返回时写库

## 国际化

所有用户可见文字通过 `strings.xml`（中文）和 `values-en/strings.xml`（英文）管理，不硬编码。

## 编码规范

- 保持与现有代码风格一致
- 常量提取，避免魔法数字和硬编码字符串
- tagId 字段预留但 UI 不暴露，遵循 YAGNI 但保持扩展性

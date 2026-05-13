package com.zahri.lighttodo.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StrikethroughSpan
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.zahri.lighttodo.R
import com.zahri.lighttodo.data.AppDatabase
import com.zahri.lighttodo.data.TodoEntity
import com.zahri.lighttodo.ui.home.dateLabel
import com.zahri.lighttodo.ui.home.displayTitle
import com.zahri.lighttodo.ui.home.isOverdueDate
import com.zahri.lighttodo.util.DateUtils
import java.time.LocalDate

/**
 * RemoteViewsService 在 widget host 进程上下文运行。
 * 这里直接通过 AppDatabase.get(context) 拿到 db，不依赖 App.instance（更稳）。
 */
class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TodoListFactory(applicationContext)
}

class TodoListFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    @Volatile private var items: List<TodoEntity> = emptyList()

    override fun onCreate() { reload() }
    override fun onDataSetChanged() { reload() }

    private fun reload() {
        items = try {
            val dao = AppDatabase.get(context).todoDao()
            val endOfTodayMillis = DateUtils.endOfDayMillis(LocalDate.now())
            dao.listDueByDaySync(endOfTodayMillis, limit = 8)
        } catch (t: Throwable) {
            android.util.Log.e("TodoWidget", "load failed", t)
            emptyList()
        }
    }

    override fun onDestroy() { items = emptyList() }
    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_item)
        val item = items.getOrNull(position) ?: return rv

        val titleText = SpannableString(item.displayTitle())
        if (item.done) {
            titleText.setSpan(StrikethroughSpan(), 0, titleText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        rv.setTextViewText(R.id.item_title, titleText)

        val deadlineSuffix = if (item.deadlineHour != null && item.deadlineMinute != null)
            "%02d:%02d".format(item.deadlineHour, item.deadlineMinute)
        else item.dateLabel()
        rv.setTextViewText(R.id.item_subtitle, deadlineSuffix)
        rv.setTextColor(
            R.id.item_subtitle,
            if (item.isOverdueDate()) Color.parseColor("#F26A6A") else Color.parseColor("#B6B6B6")
        )

        // 点 [ ] → 标记完成（带 todoId）
        val checkFill = Intent().apply {
            putExtra(TodoWidgetProvider.EXTRA_TODO_ID, item.id)
            putExtra(TodoWidgetProvider.EXTRA_OPEN_APP, false)
        }
        rv.setOnClickFillInIntent(R.id.item_check, checkFill)

        // 点行内文字 / 圆点 → 打开 App（不带 todoId，靠 OPEN_APP flag 判断）
        val rowFill = Intent().apply {
            putExtra(TodoWidgetProvider.EXTRA_OPEN_APP, true)
        }
        rv.setOnClickFillInIntent(R.id.item_root, rowFill)
        rv.setOnClickFillInIntent(R.id.item_title, rowFill)
        rv.setOnClickFillInIntent(R.id.item_subtitle, rowFill)
        return rv
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = items.getOrNull(position)?.id ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}

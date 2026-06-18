package com.zahri.lighttodo.integration.calendar

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import com.zahri.lighttodo.App
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 监听系统日历事件 (CalendarContract.Events) 的变化，触发 [CalendarSync.runOnce]。
 *
 * 设计：
 *  - onChange 由系统在主线程回调，这里做 500ms debounce，避免短时间内多次触发同步
 *  - 实际同步切到 IO 线程跑（数据库 + ContentResolver 查询，不能在主线程）
 *  - 调用方持有 observer 的引用，prefs 关闭日历同步时通过 [unregister] 注销
 */
class CalendarObserver(
    private val context: Context
) : ContentObserver(Handler(Looper.getMainLooper())) {

    internal val mainHandler = Handler(Looper.getMainLooper())
    internal val debouncedRun = Runnable { triggerSync() }

    override fun onChange(selfChange: Boolean) {
        onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        // 短时间内多次变更（例如批量插入）只跑一次
        mainHandler.removeCallbacks(debouncedRun)
        mainHandler.postDelayed(debouncedRun, DEBOUNCE_MILLIS)
    }

    private fun triggerSync() {
        val app = context.applicationContext as App
        app.appScope.launch(Dispatchers.IO) {
            try {
                CalendarSync.runOnce(app)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // observer 回调里不抛其他异常
            }
        }
    }

    companion object {
        const val DEBOUNCE_MILLIS = 500L

        /**
         * 注册一个新的 observer 并返回它，调用方负责保留引用，便于后续注销。
         * 已经注册过则会重复注册（系统允许，但调用方应避免）。
         */
        fun register(context: Context): CalendarObserver {
            val observer = CalendarObserver(context.applicationContext)
            context.applicationContext.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                /* notifyForDescendants = */ true,
                observer
            )
            return observer
        }

        fun unregister(context: Context, observer: CalendarObserver) {
            // Cancel any pending debounced callback to avoid a stale sync
            // firing after the user disabled calendar sync.
            observer.mainHandler.removeCallbacks(observer.debouncedRun)
            try {
                context.applicationContext.contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {
                // 没注册过 / 已注销，忽略
            }
        }
    }
}

package com.zahri.lighttodo.domain.reminder

object ReminderRequestCodePolicy {
    fun requestCodeFor(id: Long, isStart: Boolean): Int {
        val folded = (id xor (id ushr 32)).toInt() and 0x3FFFFFFF
        return (folded shl 1) or if (isStart) 0 else 1
    }

    fun notificationIdFor(id: Long, isStart: Boolean): Int =
        requestCodeFor(id, isStart)
}

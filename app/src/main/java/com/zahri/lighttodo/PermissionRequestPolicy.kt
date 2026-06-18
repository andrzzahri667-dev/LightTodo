package com.zahri.lighttodo

import android.Manifest
import android.os.Build

object PermissionRequestPolicy {
    fun startupPermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        buildList {
            if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (sdkInt < Build.VERSION_CODES.Q) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

    fun calendarPermissions(): Array<String> =
        arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )

    fun calendarPermissionsGranted(results: Map<String, Boolean>): Boolean =
        results[Manifest.permission.READ_CALENDAR] == true &&
            results[Manifest.permission.WRITE_CALENDAR] == true
}

package com.zahri.lighttodo

import android.Manifest
import android.os.Build

object PermissionRequestPolicy {
    fun startupPermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        buildList {
            if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            add(Manifest.permission.READ_CALENDAR)
            if (sdkInt < Build.VERSION_CODES.Q) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
}

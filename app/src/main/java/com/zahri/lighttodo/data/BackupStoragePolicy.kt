package com.zahri.lighttodo.data

import android.os.Build

object BackupStoragePolicy {
    enum class PublicDownloadsMode {
        MediaStore,
        LegacyDirectPath
    }

    fun publicDownloadsMode(sdkInt: Int = Build.VERSION.SDK_INT): PublicDownloadsMode =
        if (sdkInt >= Build.VERSION_CODES.Q) {
            PublicDownloadsMode.MediaStore
        } else {
            PublicDownloadsMode.LegacyDirectPath
        }

    fun needsAllFilesAccessForRestore(): Boolean = false
}

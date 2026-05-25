package com.zahri.lighttodo.data

object BackupReadPolicy {
    const val MaxBackupBytes = 50L * 1024L * 1024L

    fun canReadBackupSize(sizeBytes: Long): Boolean =
        sizeBytes in 0..MaxBackupBytes
}

package com.zahri.lighttodo

import android.Manifest
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionRequestPolicyTest {

    @Test
    fun startupPermissions_includeNotificationsButNotCalendarOnAndroid13Plus() {
        val permissions = PermissionRequestPolicy.startupPermissions(Build.VERSION_CODES.TIRAMISU)

        assertEquals(
            listOf(
                Manifest.permission.POST_NOTIFICATIONS
            ),
            permissions
        )
    }

    @Test
    fun startupPermissions_doNotRequestCalendarOnAndroid10To12() {
        val permissions = PermissionRequestPolicy.startupPermissions(Build.VERSION_CODES.Q)

        assertEquals(emptyList<String>(), permissions)
    }

    @Test
    fun startupPermissions_includeLegacyStorageButNotCalendarBeforeAndroid10() {
        val permissions = PermissionRequestPolicy.startupPermissions(Build.VERSION_CODES.P)

        assertEquals(
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            permissions
        )
    }
}

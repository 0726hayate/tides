package com.hayate0726.tides.security

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoInternetPermissionTest {

    @Test
    fun apk_must_not_declare_internet_permission() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val pkg = ctx.packageManager.getPackageInfo(
            ctx.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val perms = pkg.requestedPermissions.orEmpty().toList()
        assertFalse(
            "INTERNET permission must not appear in manifest. Found: $perms",
            perms.contains(android.Manifest.permission.INTERNET)
        )
    }

    @Test
    fun apk_must_not_declare_access_network_state_permission() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val pkg = ctx.packageManager.getPackageInfo(
            ctx.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val perms = pkg.requestedPermissions.orEmpty().toList()
        assertFalse(
            "ACCESS_NETWORK_STATE must not appear. Found: $perms",
            perms.contains(android.Manifest.permission.ACCESS_NETWORK_STATE)
        )
    }
}

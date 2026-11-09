// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from DevSwitch DevSettings.kt, Copyright (C) 2026 Lennox.
// Changes: wireless-only controls, native callbacks and explicit Wi-Fi / permission errors.
package studio.astratale.xingyuhelper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Process
import android.provider.Settings
import android.util.Log

class WirelessSettings(private val context: Context) {
    companion object { const val KEY = "adb_wifi_enabled" }
    val hasPermission: Boolean get() = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    val enabled: Boolean get() = Settings.Global.getInt(context.contentResolver, KEY, 0) != 0
    private val onWifi: Boolean
        // Check the Wi-Fi transport even when a VPN owns the default route.
        @Suppress("DEPRECATION")
        get() {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            return manager.allNetworks.any { manager.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
        }
    fun setEnabled(value: Boolean) {
        if (!hasPermission) throw SecurityException("请先在电脑端初始化手机助手。")
        if (value && !onWifi) throw IllegalStateException("请先连接与电脑相通的 Wi-Fi。")
        if (value && Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 0) {
            check(Settings.Global.putInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1)) { "系统未允许开启开发者选项。" }
        }
        check(Settings.Global.putInt(context.contentResolver, KEY, if (value) 1 else 0)) { "系统未接受无线调试开关。" }
        Log.i("XingyuHelper", "wireless requested=$value uid=${Process.myUid()}")
    }
}

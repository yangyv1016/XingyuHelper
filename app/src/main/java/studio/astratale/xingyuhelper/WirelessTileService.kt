// SPDX-License-Identifier: GPL-3.0-or-later
// Wireless-only adaptation of DevSwitch ToggleTileService.kt, Copyright (C) 2026 Lennox.
package studio.astratale.xingyuhelper

import android.app.PendingIntent
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class WirelessTileService : TileService() {
    private val settings by lazy { WirelessSettings(this) }
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) { override fun onChange(selfChange: Boolean) = render() }
    override fun onStartListening() { contentResolver.registerContentObserver(Settings.Global.getUriFor(WirelessSettings.KEY), false, observer); render() }
    override fun onStopListening() { contentResolver.unregisterContentObserver(observer) }
    override fun onClick() {
        if (!settings.hasPermission) { openApp(); return }
        if (isLocked) unlockAndRun { toggle() } else toggle()
    }
    private fun toggle() {
        try { settings.setEnabled(!settings.enabled) }
        catch (error: SecurityException) { Toast.makeText(this, error.message, Toast.LENGTH_LONG).show() }
        catch (error: IllegalStateException) { Toast.makeText(this, error.message, Toast.LENGTH_LONG).show() }
        render()
    }
    private fun render() {
        val tile = qsTile ?: return
        tile.state = if (!settings.hasPermission) Tile.STATE_UNAVAILABLE else if (settings.enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (!settings.hasPermission) "请先初始化" else if (settings.enabled) "已开启" else "已关闭"
        tile.updateTile()
    }
    @Suppress("DEPRECATION")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        else startActivityAndCollapse(intent)
    }
}

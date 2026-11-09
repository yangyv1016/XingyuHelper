// SPDX-License-Identifier: GPL-3.0-or-later
package studio.astratale.xingyuhelper

import android.app.Activity
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*

class MainActivity : Activity() {
    private val settings by lazy { WirelessSettings(this) }
    private lateinit var state: TextView
    private lateinit var detail: TextView
    private lateinit var action: Button
    private lateinit var permission: TextView
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = render()
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun background(color: String, radius: Int) = GradientDrawable().apply { setColor(Color.parseColor(color)); cornerRadius = dp(radius).toFloat() }
    private fun text(value: String, size: Float, color: String = "#25344D") = TextView(this).apply { text = value; textSize = size; setTextColor(Color.parseColor(color)); setLineSpacing(dp(3).toFloat(), 1f) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(Color.parseColor("#F4F7FC")) }
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(32), dp(24), dp(24)) }
        scroll.addView(column)
        column.addView(ImageView(this).apply { setImageResource(R.drawable.studio_logo); contentDescription = "AstraTaleStudio" }, LinearLayout.LayoutParams(dp(72), dp(72)))
        column.addView(text("星语互联手机助手", 26f).apply { setTypeface(typeface, Typeface.BOLD) }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20) })
        column.addView(text("一次初始化，之后轻点连接", 15f, "#77849B"), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(28) })
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(26), dp(24), dp(26)); background = background("#FFFFFF", 22) }
        state = text("", 22f).apply { setTypeface(typeface, Typeface.BOLD) }
        detail = text("", 15f, "#77849B")
        action = Button(this).apply { textSize = 17f; isAllCaps = false; elevation = 0f; setTextColor(Color.WHITE); background = background("#527EF4", 16); contentDescription = "无线调试开关"; minHeight = dp(56) }
        card.addView(state)
        card.addView(detail, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12); bottomMargin = dp(24) })
        card.addView(action, LinearLayout.LayoutParams(-1, dp(56)))
        column.addView(card)
        permission = text("", 14f, "#77849B")
        column.addView(permission, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) })
        column.addView(text("首次使用此 Wi-Fi 时，请确认系统的网络授权。\n开启无线调试不会自动信任其他电脑。", 14f, "#77849B"), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        column.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        column.addView(text("AstraTaleStudio 作品 · 0.1.0 测试版", 12f, "#9BA5B7").apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(32) })
        setContentView(scroll)
        action.setOnClickListener {
            try { settings.setEnabled(!settings.enabled); render() }
            catch (error: SecurityException) { render(); detail.text = error.message }
            catch (error: IllegalStateException) { render(); detail.text = error.message }
        }
    }
    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver(Settings.Global.getUriFor(WirelessSettings.KEY), false, observer)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        render()
    }
    override fun onResume() { super.onResume(); render() }
    override fun onStop() { contentResolver.unregisterContentObserver(observer); window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); super.onStop() }
    private fun render() {
        val ready = settings.hasPermission
        val enabled = settings.enabled
        action.isEnabled = ready
        state.text = if (!ready) "先完成一次初始化" else if (enabled) "无线调试已开启" else "准备连接电脑"
        detail.text = if (!ready) "首次用 USB 连接电脑，在星语互联的“局域网连接 → 手机助手”中点击初始化。" else if (enabled) "电脑端保持星语互联运行，发现本设备后会自动连接。" else "手机连接与电脑相通的 Wi-Fi，然后点击下方按钮。"
        action.text = if (enabled) "关闭无线调试" else "开启无线调试"
        action.background = background(if (!ready) "#B9C7EB" else if (enabled) "#405578" else "#527EF4", 16)
        permission.text = if (ready) "已完成电脑初始化\n首次无线连接成功后，日常使用无需插线。" else "请在电脑端完成授权，之后即可使用开关。"
    }
}

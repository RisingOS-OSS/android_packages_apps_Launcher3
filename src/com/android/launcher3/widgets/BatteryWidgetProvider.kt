/*
 * Copyright (C) 2023-2025 the risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.widgets

import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.widget.RemoteViews

import androidx.core.content.ContextCompat

import com.android.launcher3.R

class BatteryWidgetProvider : BaseWidgetProvider() {

    private var previousBatteryInfo: String? = null
    private var appWidgetIds: IntArray? = null
    private var remoteViews: RemoteViews? = null

    override fun getLayoutId(): Int = R.layout.widget_battery

    override fun onWidgetUpdate(context: Context) {
        if (appWidgetIds == null) {
            appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
                android.content.ComponentName(context, BatteryWidgetProvider::class.java)
            )
        }
        if (remoteViews == null) {
            remoteViews = RemoteViews(context.packageName, getLayoutId())
        }
        update(context)
    }

    override suspend fun performScheduledTask(context: Context) {
        update(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "ACTION_WIDGET_CLICK") {
            update(context)
            schedulePolling(context)
            val batterySaverIntent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(batterySaverIntent)
        }
    }

    private fun update(context: Context) {
        if (appWidgetIds == null || remoteViews == null) return
        val batteryInfo = getBatteryInfo(context)
        val deviceName = android.os.Build.MODEL
        if (batteryInfo == previousBatteryInfo) return
        previousBatteryInfo = batteryInfo
        appWidgetIds!!.forEach { appWidgetId ->
            remoteViews!!.apply {
                setTextViewText(R.id.batteryLevel, batteryInfo)
                setImageViewBitmap(R.id.phone_icon, getWidgetIcon(context, batteryInfo))
                setTextViewText(R.id.deviceName, deviceName)
                val intent = Intent(context, BatteryWidgetProvider::class.java).apply {
                    action = "ACTION_WIDGET_CLICK"
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent, PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.batteryRootLayout, pendingIntent)
            }
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, remoteViews)
        }
    }

    private fun getBatteryInfo(context: Context): String {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val currentCharging = intent?.getIntExtra("max_charging_current", -1) ?: -1
        val isCharging = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
        val totalScreenTime = getTotalScreenTime(context)
        return if (isCharging) {
            val currentText = if (currentCharging.toString().length >= 5) {
                (currentCharging / 1000).toString()
            } else {
                currentCharging.toString()
            }
            "$batteryLevel% - ${currentText}mA"
        } else {
            val screenTimeText = formatScreenTime(totalScreenTime)
            "$batteryLevel% - $screenTimeText"
        }
    }

    private fun getTotalScreenTime(context: Context): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24 * 60 * 60 * 1000

        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        
        if (stats.isNullOrEmpty()) {
            return 0
        }
        
        var totalScreenTime = 0L
        for (usageStat in stats) {
            totalScreenTime += usageStat.totalTimeInForeground
        }
        return totalScreenTime / (1000 * 60)
    }

    private fun formatScreenTime(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return "$hours H $mins"
    }

    private fun getWidgetIcon(context: Context, batteryInfo: String): Bitmap {
        val icon: Drawable? = ContextCompat.getDrawable(context, R.drawable.device_phone_default)
        return generateBitmap(context, batteryInfo.substringBefore('%').toInt(), icon, 24f)
    }

    companion object {
        fun generateBitmap(
            context: Context,
            percentage: Int,
            iconDrawable: Drawable?,
            iconSizePx: Float
        ): Bitmap {
            val width = 200
            val height = 200
            val stroke = 22
            val padding = 20
            val minAngle = 135f
            val maxAngle = 275f
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
            paint.strokeWidth = stroke.toFloat()
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            val arc = RectF().apply {
                set((stroke / 2f) + padding, (stroke / 2f) + padding, width - padding - (stroke / 2f), height - padding - (stroke / 2f))
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            paint.color = Color.argb(75, 255, 255, 255)
            canvas.drawArc(arc, minAngle, maxAngle, false, paint)
            paint.color = Color.WHITE
            canvas.drawArc(arc, minAngle, (maxAngle / 100) * percentage, false, paint)
            iconDrawable?.let {
                val arcCenterX = (arc.left + arc.right) / 2
                val arcCenterY = (arc.top + arc.bottom) / 2
                val size = dp2px(context, iconSizePx).toFloat()
                val left = (arcCenterX - size / 2).toInt()
                val top = (arcCenterY - size / 2).toInt()
                it.setBounds(left, top, left + size.toInt(), top + size.toInt())
                it.draw(canvas)
            }
            return bitmap
        }

        private fun dp2px(context: Context, dp: Float): Int {
            val displayMetrics = context.resources.displayMetrics
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, displayMetrics).toInt()
        }
    }
}

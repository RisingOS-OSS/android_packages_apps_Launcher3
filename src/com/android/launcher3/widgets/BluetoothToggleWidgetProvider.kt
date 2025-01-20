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
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.android.launcher3.R

class BluetoothToggleWidgetProvider : BaseToggleWidgetProvider() {

    private var appWidgetIds: IntArray? = null
    private var remoteViews: RemoteViews? = null

    override fun getLayoutId() = R.layout.widget_bluetooth_tile

    override fun onUpdate(context: Context) {
        initializeState(context)
        updateWidget(context)
    }

    override fun getToggleActionIntent(context: Context, appWidgetId: Int): Intent {
        return Intent(context, this::class.java).apply {
            action = "TOGGLE_BLUETOOTH"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
    }

    override fun isServiceActive(context: Context): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter?.isEnabled == true
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "TOGGLE_BLUETOOTH") {
            toggleBluetooth(context)
            initializeState(context)
            updateWidget(context)
            schedulePolling(context)
        }
    }

    override fun updateWidget(context: Context) {
        if (appWidgetIds == null || remoteViews == null) return
        val widgetViews = remoteViews
        val isActive = isServiceActive(context)
        appWidgetIds!!.forEach { appWidgetId ->
            widgetViews!!.apply {
                widgetViews.setInt(
                    R.id.widget_root, 
                    "setBackgroundColor", 
                    context.getColor(
                        if (isActive) R.color.themed_icon_color else R.color.themed_icon_background_color
                    )
                )
                widgetViews.setInt(
                    R.id.widget_icon, 
                    "setColorFilter", 
                    context.getColor(
                        if (isActive) R.color.themed_icon_background_color else R.color.themed_icon_color
                    )
                )
                val bluetoothIconRes = if (isActive) R.drawable.ic_bluetooth_icon_on else R.drawable.ic_bluetooth_icon_off
                widgetViews.setImageViewResource(R.id.widget_icon, bluetoothIconRes)
                val toggleIntent = getToggleActionIntent(context, appWidgetId)
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    appWidgetId, 
                    toggleIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                widgetViews.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, widgetViews)
        }
    }
    
    private fun toggleBluetooth(context: Context) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
            bluetoothAdapter.disable()
        } else {
            bluetoothAdapter?.enable()
        }
    }
    
    private fun initializeState(context: Context) {
        if (appWidgetIds == null || remoteViews == null) {
            appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
                android.content.ComponentName(context, this::class.java)
            )
            remoteViews = RemoteViews(context.packageName, getLayoutId())
        }
    }
}

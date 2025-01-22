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
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.widget.RemoteViews
import com.android.launcher3.R

class WifiWidgetProvider : BaseToggleTileWidgetProvider() {

    private var appWidgetIds: IntArray? = null

    override fun getLayoutId() = R.layout.widget_wifi_tile

    override fun getToggleActionIntent(context: Context, appWidgetId: Int): Intent {
        return Intent(context, this::class.java).apply {
            action = "TOGGLE_WIFI"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
    }

    override fun isServiceActive(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    override fun toggleService(context: Context) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
    }

    override fun getIconResource(isActive: Boolean): Int {
        return if (isActive) R.drawable.ic_wifi_24 else R.drawable.ic_wifi_off_24
    }

    override fun getActionString() = "TOGGLE_WIFI"

    override fun getAppWidgetIds(context: Context): IntArray {
        if (appWidgetIds == null) {
            appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
                android.content.ComponentName(context, this::class.java)
            )
        }
        return appWidgetIds ?: IntArray(0)
    }
}

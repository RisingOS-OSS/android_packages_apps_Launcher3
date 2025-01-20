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
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import android.widget.RemoteViews
import com.android.launcher3.R

class DataToggleWidgetProvider : BaseToggleWidgetProvider() {

    private var appWidgetIds: IntArray? = null
    private var remoteViews: RemoteViews? = null

    override fun getLayoutId() = R.layout.widget_data_tile

    override fun onUpdate(context: Context) {
        initializeState(context)
        updateWidget(context)
    }

    override fun getToggleActionIntent(context: Context, appWidgetId: Int): Intent {
        return Intent(context, this::class.java).apply {
            action = "TOGGLE_MOBILE_DATA"
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
    }

    override fun isServiceActive(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "TOGGLE_MOBILE_DATA") {
            toggleMobileData(context)
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
                val dataIconRes = if (isActive) R.drawable.ic_signal_cellular_alt_24 else R.drawable.ic_mobiledata_off_24
                widgetViews.setImageViewResource(R.id.widget_icon, dataIconRes)
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
    
    private fun toggleMobileData(context: Context) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            val method = telephonyManager::class.java.getDeclaredMethod("setDataEnabled", Boolean::class.java)
            method.isAccessible = true
            method.invoke(telephonyManager, !isServiceActive(context))
        } catch (e: Exception) {}
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

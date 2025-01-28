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
import android.widget.RemoteViews
import com.android.launcher3.R

abstract class BaseToggleTileWidgetProvider : BaseToggleWidgetProvider() {

    private var remoteViews: RemoteViews? = null
    private var previousState: Any? = null

    abstract fun toggleService(context: Context)

    protected abstract fun getIconResource(isActive: Boolean): Int
    protected abstract fun getActionString(): String

    override fun onUpdate(context: Context) {
        initializeState(context)
        updateWidget(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == getActionString()) {
            toggleService(context)
            initializeState(context)
            updateWidget(context)
            schedulePolling(context)
        }
    }

    override fun updateWidget(context: Context) {
        val appWidgetIds = getAppWidgetIds(context)
        if (appWidgetIds == null || appWidgetIds!!.isEmpty() || remoteViews == null) return
        val currentState = isServiceActive(context)
        if (previousState == currentState) return
        previousState = currentState
        val isActive = isServiceActive(context)
        appWidgetIds.forEach { appWidgetId ->
            remoteViews!!.apply {
                setInt(R.id.widget_root, "setBackgroundColor", context.getColor(
                    if (isActive) R.color.battery_device_primary_color else R.color.battery_device_secondary_color
                ))
                setInt(R.id.widget_icon, "setColorFilter", context.getColor(
                    if (isActive) R.color.battery_device_secondary_color else R.color.battery_device_primary_color
                ))
                setImageViewResource(R.id.widget_icon, getIconResource(isActive))
                val toggleIntent = getToggleActionIntent(context, appWidgetId)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, remoteViews)
        }
    }

    private fun initializeState(context: Context) {
        if (remoteViews == null) {
            remoteViews = RemoteViews(context.packageName, getLayoutId())
        }
    }

    protected abstract fun getAppWidgetIds(context: Context): IntArray
}

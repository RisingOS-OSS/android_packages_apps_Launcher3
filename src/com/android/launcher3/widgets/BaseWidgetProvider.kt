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

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

abstract class BaseWidgetProvider : AppWidgetProvider() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        onWidgetUpdate(context)
        schedulePolling(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedulePolling(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelPolling()
    }

    private fun schedulePolling(context: Context) {
        cancelPolling()
        job = coroutineScope.launch {
            while (isActive) {
                delay(2000)
                performScheduledTask(context)
            }
        }
    }

    private fun cancelPolling() {
        job?.cancel() 
        job = null
    }

    abstract fun getLayoutId(): Int

    abstract fun onWidgetUpdate(context: Context)

    open suspend fun performScheduledTask(context: Context) {}
}

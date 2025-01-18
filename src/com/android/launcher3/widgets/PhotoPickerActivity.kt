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

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.ContentResolver
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Button
import android.widget.RemoteViews
import android.widget.Switch
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatDelegate

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.UserHandle

import com.android.launcher3.R

class PhotoPickerActivity : AppCompatActivity() {

    private val PICK_PHOTO_REQUEST_CODE = 1
    private lateinit var switchGrayscale: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_picker)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        switchGrayscale = findViewById(R.id.switchGrayscale)
        val isGrayscale = Settings.System.getIntForUser(
            contentResolver, 
            "home_photo_widget_grayscale", 
            0, 
            UserHandle.USER_CURRENT
        ) == 1
        switchGrayscale.isChecked = isGrayscale
        val pickPhotoButton: Button = findViewById(R.id.btnPickPhoto)
        pickPhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_PHOTO_REQUEST_CODE)
        }
        switchGrayscale.setOnCheckedChangeListener { _, isChecked ->
            val resolver: ContentResolver = applicationContext.contentResolver
            Settings.System.putIntForUser(
                resolver, 
                "home_photo_widget_grayscale", 
                if (isChecked) 1 else 0, 
                UserHandle.USER_CURRENT
            )
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            val widgetIds = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, PhotoWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            sendBroadcast(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == PICK_PHOTO_REQUEST_CODE) {
            val photoUri: Uri? = data?.data
            photoUri?.let {
                val savedImagePath = saveImageToInternalStorage(applicationContext, it, "home_widget", "HOME_WIDGET_PHOTO")
                if (savedImagePath != null) {
                    val resolver: ContentResolver = applicationContext.contentResolver
                    Settings.System.putStringForUser(resolver, "home_photo_widget_uri", savedImagePath, UserHandle.USER_CURRENT)
                    val isGrayscale = switchGrayscale.isChecked
                    Settings.System.putIntForUser(
                        resolver,
                        "home_photo_widget_grayscale",
                        if (isGrayscale) 1 else 0,
                        UserHandle.USER_CURRENT
                    )
                    val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
                    val widgetIds = appWidgetManager.getAppWidgetIds(ComponentName(applicationContext, PhotoWidgetProvider::class.java))
                    widgetIds.forEach { widgetId ->
                        val remoteViews = RemoteViews(applicationContext.packageName, R.layout.widget_photo)
                        remoteViews.setImageViewUri(R.id.widget_image, Uri.parse(savedImagePath))
                        val intent = Intent(applicationContext, PhotoPickerActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                        remoteViews.setOnClickPendingIntent(R.id.widget_image, pendingIntent)
                        appWidgetManager.updateAppWidget(widgetId, remoteViews)
                    }
                    Toast.makeText(this, "Photo selected for widget", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to save the photo", Toast.LENGTH_SHORT).show()
                }
            }
            finish()
        }
    }

    private fun saveImageToInternalStorage(context: Context, imgUri: Uri, featurePath: String, filePrefix: String): String? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(imgUri)
            if (imgUri.toString().startsWith("content://com.google.android.apps.photos.contentprovider")) {
                val segments = imgUri.pathSegments
                if (segments.size > 2) {
                    val mediaUriString = URLDecoder.decode(segments[2], StandardCharsets.UTF_8.name())
                    val mediaUri = Uri.parse(mediaUriString)
                    inputStream = context.contentResolver.openInputStream(mediaUri)
                } else {
                    throw Exception("Failed to parse Google Photos content URI")
                }
            }
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val imageFileName = "$filePrefix" + "_$timeStamp.png"

            val directory = File(context.getExternalFilesDir(null), "risingOS/$featurePath")
            if (!directory.exists() && !directory.mkdirs()) {
                return null
            }
            val files = directory.listFiles { _, name -> name.startsWith(filePrefix) && name.endsWith(".png") }
            files?.forEach { it.delete() }
            val file = File(directory, imageFileName)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            return file.absolutePath
        } catch (e: Exception) {
        } finally {
            inputStream?.close()
        }
        return null
    }
}


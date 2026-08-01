package com.hiddenlayer.launcher.data

import android.content.ComponentName
import android.graphics.Bitmap

data class AppInfo(
    val packageName: String,
    val componentName: ComponentName,
    val label: String,
    // Pre-decoded once in AppRepository (off the main thread) so scrolling/paging
    // never has to convert a Drawable to a Bitmap on the UI thread.
    val icon: Bitmap
)

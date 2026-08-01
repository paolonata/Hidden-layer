package com.hiddenlayer.launcher.data

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val componentName: ComponentName,
    val label: String,
    val icon: Drawable
)

package com.hiddenlayer.launcher.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Marks the current screen as secure for as long as it is composed: no screenshots, and no
 * thumbnail of it in the system's recent-apps switcher. Without this, the contents of the
 * vault could show up in a screenshot or leak through the app-switcher preview — which
 * would rather defeat the point of hiding the apps in the first place.
 */
@Composable
fun SecureScreen() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val window = context.findActivity()?.window
        window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

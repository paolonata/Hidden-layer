package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.hiddenlayer.launcher.data.AppInfo

@Composable
fun AppIcon(app: AppInfo, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(app.componentName) {
        app.icon.toBitmap(width = 128, height = 128).asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = app.label,
        modifier = modifier.size(size)
    )
}

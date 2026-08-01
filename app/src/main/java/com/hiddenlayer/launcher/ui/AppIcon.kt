package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import com.hiddenlayer.launcher.data.AppInfo

@Composable
fun AppIcon(app: AppInfo, size: Dp, modifier: Modifier = Modifier) {
    // app.icon is already a decoded Bitmap (see AppRepository) — this just wraps it,
    // no decoding happens here, so paging/scrolling never pays that cost.
    val bitmap = remember(app.icon) { app.icon.asImageBitmap() }
    Image(
        bitmap = bitmap,
        contentDescription = app.label,
        modifier = modifier.size(size)
    )
}

package com.hiddenlayer.launcher.ui

import android.app.WallpaperManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/**
 * Frosted-glass panel background: the device's actual wallpaper, blurred, with a dark
 * scrim on top for text contrast — instead of a flat opaque Material surface color.
 *
 * Reading the live wallpaper via WallpaperManager only works without extra permissions
 * while this app is the active home/launcher (which is the whole point of this project);
 * if it's ever unavailable (e.g. no wallpaper drawable, or restricted by the OEM), this
 * silently falls back to just the scrim rather than crashing. Modifier.blur() only takes
 * effect on API 31+; on 26-30 the wallpaper shows through sharp instead of blurred.
 */
@Composable
fun BlurredWallpaperBackground(modifier: Modifier = Modifier, scrimAlpha: Float = 0.28f) {
    val context = LocalContext.current
    val wallpaperBitmap = remember {
        runCatching { WallpaperManager.getInstance(context).drawable?.toBitmap() }.getOrNull()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (wallpaperBitmap != null) {
            Image(
                bitmap = remember(wallpaperBitmap) { wallpaperBitmap.asImageBitmap() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )
    }
}

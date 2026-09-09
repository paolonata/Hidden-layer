package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.ui.theme.HlPaper
import com.hiddenlayer.launcher.ui.theme.HlTileShape

/** Shared tile for any full-screen app grid (regular drawer, hidden drawer): the icon on
 * its own, no caption — the icons are the labels.
 *
 * [tinted] posa dietro l'icona un fondo appena percepibile. Serve alle app bloccate durante
 * una sessione: è il **terzo** segnale dopo la desaturazione e l'alpha, e a differenza dei
 * primi due funziona anche su un'icona che era già bianca e nera. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridTile(
    app: AppInfo,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    grayscale: Boolean = false,
    tinted: Boolean = false
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(HlTileShape)
            .background(if (tinted) HlPaper.copy(alpha = 0.04f) else Color.Transparent)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .padding(6.dp)
    ) {
        AppIcon(app = app, size = 48.dp, grayscale = grayscale)
    }
}

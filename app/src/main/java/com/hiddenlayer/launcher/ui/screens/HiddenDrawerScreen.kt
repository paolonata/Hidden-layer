package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.ui.AppGridTile
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.rememberCloseOnPullDown

/**
 * The actual way to reach a hidden app: a drawer just like the regular one, scoped to
 * only the apps you've hidden, over a darker "incognito" tint so it reads as a distinct
 * space. Managing *which* apps are hidden lives one level deeper, behind the gear icon
 * (HiddenManagerScreen) — this screen is for opening apps, not for toggling visibility.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenDrawerScreen(
    state: LauncherUiState,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val closeOnPullDown = rememberCloseOnPullDown(onClose)

    Box(modifier = Modifier.fillMaxSize()) {
        BlurredWallpaperBackground(scrimAlpha = 0.5f)

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            placeholder = { Text("Cerca tra le app nascoste") },
                            leadingIcon = {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(alpha = 0.7f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                cursorColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Gestisci app nascoste", tint = Color.White)
                        }
                    }
                )
            }
        ) { padding ->
            if (state.hiddenApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nessuna app nascosta. Tieni premuto su un'app (in home o nel cassetto) e scegli \"Nascondi app\", oppure usa l'icona impostazioni qui sopra.",
                        modifier = Modifier.padding(32.dp),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .nestedScroll(closeOnPullDown)
                ) {
                    items(state.hiddenVisibleApps, key = { it.componentName.flattenToString() }) { app ->
                        AppGridTile(app = app, onTap = { onAppClick(app) }, onLongPress = { onAppLongPress(app) })
                    }
                }
            }
        }
    }
}

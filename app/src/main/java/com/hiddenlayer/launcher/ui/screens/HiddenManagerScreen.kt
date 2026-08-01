package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.ui.AppIcon
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenManagerScreen(
    state: LauncherUiState,
    onAppClick: (AppInfo) -> Unit,
    onToggleHidden: (AppInfo) -> Unit,
    onDone: () -> Unit
) {
    BackHandler(onBack = onDone)

    Box(modifier = Modifier.fillMaxSize()) {
        BlurredWallpaperBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    ),
                    title = { Text("App nascoste") },
                    navigationIcon = {
                        TextButton(onClick = onDone) { Text("Chiudi", color = Color.White) }
                    }
                )
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                item {
                    Text(
                        "Tocca un'app nascosta per aprirla. Usa l'interruttore per nasconderla o mostrarla nel cassetto e nella home. Nascondere un'app è solo un modo per pulire la vista: non serve alcun PIN.",
                        modifier = Modifier.padding(16.dp),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                items(state.allApps, key = { it.componentName.flattenToString() }) { app ->
                    val hidden = app.packageName in state.hiddenPackages
                    ListItem(
                        headlineContent = { Text(app.label, color = Color.White) },
                        leadingContent = { AppIcon(app = app, size = 36.dp) },
                        trailingContent = {
                            Switch(checked = hidden, onCheckedChange = { onToggleHidden(app) })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable(enabled = hidden) { onAppClick(app) }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                }
            }
        }
    }
}

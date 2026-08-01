package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.ui.AppIcon
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.rememberCloseOnPullDown

/**
 * Settings screen for choosing *which* apps are hidden — reached from a gear icon inside
 * HiddenDrawerScreen. Purely a toggle list: this is not how you open a hidden app (that's
 * the hidden drawer itself), so there is no tap-to-launch here on purpose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenManagerScreen(
    state: LauncherUiState,
    onToggleHidden: (AppInfo) -> Unit,
    onDone: () -> Unit
) {
    BackHandler(onBack = onDone)
    val closeOnPullDown = rememberCloseOnPullDown(onDone)

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
                    title = { Text("Gestisci app nascoste") },
                    navigationIcon = {
                        TextButton(onClick = onDone) { Text("Chiudi", color = Color.White) }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .nestedScroll(closeOnPullDown)
            ) {
                item {
                    Text(
                        "Scegli quali app nascondere. Non serve alcun PIN: è solo un filtro per pulire la vista. Per aprire un'app già nascosta, chiudi questa schermata e usa il cassetto delle app nascoste.",
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
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                }
            }
        }
    }
}

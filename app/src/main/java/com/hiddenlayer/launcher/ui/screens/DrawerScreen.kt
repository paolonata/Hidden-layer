package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hiddenlayer.launcher.DrawerMode
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.ui.AppGridTile
import com.hiddenlayer.launcher.ui.AppSearchField
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.DragHandle
import com.hiddenlayer.launcher.ui.closeOnDragDown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerScreen(
    state: LauncherUiState,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    onOpenHiddenDrawer: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val gridState = rememberLazyGridState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .closeOnDragDown(
                canClose = {
                    gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                },
                onClose = onClose
            )
    ) {
        BlurredWallpaperBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    DragHandle(onClose = onClose)
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        title = {
                            AppSearchField(
                                value = state.query,
                                onValueChange = onQueryChange,
                                placeholder = if (state.drawerMode == DrawerMode.BROWSE) {
                                    "Cerca app"
                                } else {
                                    "Scegli un'app"
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                            }
                        },
                        actions = {
                            if (state.drawerMode == DrawerMode.BROWSE) {
                                var menuExpanded by remember { mutableStateOf(false) }
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                                }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("App nascoste") },
                                        leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onOpenHiddenDrawer()
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        ) { padding ->
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(state.visibleApps, key = { it.componentName.flattenToString() }) { app ->
                    AppGridTile(app = app, onTap = { onAppClick(app) }, onLongPress = { onAppLongPress(app) })
                }
            }
        }
    }
}

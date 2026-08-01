package com.hiddenlayer.launcher.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.MenuOrigin
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.ui.AppIcon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: LauncherUiState,
    onAppTap: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo, MenuOrigin, Int) -> Unit,
    onDockSlotLongPress: (Int) -> Unit,
    onEmptyPageLongPress: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val pages = state.homePages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val dockSlots = remember(state.dockApps) { List(4) { i -> state.dockApps.getOrNull(i) } }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            HomePage(
                apps = pages.getOrElse(pageIndex) { emptyList() },
                onAppTap = onAppTap,
                onAppLongPress = { app -> onAppLongPress(app, MenuOrigin.HOME, -1) },
                onEmptyLongPress = onEmptyPageLongPress
            )
        }

        if (pages.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(3.dp)
                            .size(if (selected) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.White else Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }

        SwipeUpHandle(onOpenDrawer = onOpenDrawer)

        Dock(
            dockSlots = dockSlots,
            onAppTap = onAppTap,
            onAppLongPress = { app, slot -> onAppLongPress(app, MenuOrigin.DOCK, slot) },
            onEmptySlotLongPress = onDockSlotLongPress,
            onOpenDrawer = onOpenDrawer
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomePage(
    apps: List<AppInfo>,
    onAppTap: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    onEmptyLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onEmptyLongPress() })
            }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            userScrollEnabled = false,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps, key = { it.componentName.flattenToString() }) { app ->
                HomeIconTile(app = app, onTap = { onAppTap(app) }, onLongPress = { onAppLongPress(app) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeIconTile(app: AppInfo, onTap: () -> Unit, onLongPress: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .padding(4.dp)
    ) {
        AppIcon(app = app, size = 48.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SwipeUpHandle(onOpenDrawer: () -> Unit) {
    var dragAccum by remember { mutableStateOf(0f) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccum += dragAmount
                        if (dragAccum < -40f) onOpenDrawer()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.6f))
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Dock(
    dockSlots: List<AppInfo?>,
    onAppTap: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo, Int) -> Unit,
    onEmptySlotLongPress: (Int) -> Unit,
    onOpenDrawer: () -> Unit
) {
    Surface(color = Color.Black.copy(alpha = 0.25f), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dockSlots.forEachIndexed { index, app ->
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .combinedClickable(
                            onClick = { app?.let(onAppTap) },
                            onLongClick = {
                                if (app != null) onAppLongPress(app, index) else onEmptySlotLongPress(index)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (app != null) {
                        AppIcon(app = app, size = 44.dp)
                    } else {
                        Icon(Icons.Default.Add, contentDescription = "Aggiungi al dock", tint = Color.White.copy(alpha = 0.6f))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(onClick = onOpenDrawer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Apps, contentDescription = "Tutte le app", tint = Color.White)
            }
        }
    }
}

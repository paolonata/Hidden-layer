package com.hiddenlayer.launcher.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.MenuOrigin
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.data.HomeLayoutRepository
import com.hiddenlayer.launcher.ui.AppIcon
import kotlin.math.roundToInt

private const val HOME_COLUMNS = 4
private val TOP_GESTURE_EXCLUSION = 56.dp
private const val SWIPE_OPEN_THRESHOLD_PX = 40f
private val PAGE_MOVE_THRESHOLD = 96.dp
private val TAP_VS_DRAG_THRESHOLD = 12.dp
private val DRAG_ICON_SIZE = 56.dp

/**
 * Swipe-up-to-open-drawer is a single gesture detector on the whole screen (not one per
 * dock/handle) so it doesn't compete with the pager's own horizontal drag detection on
 * every touch frame. A drag that starts within TOP_GESTURE_EXCLUSION of the top edge is
 * ignored, leaving that strip free for the system's notification-shade / quick-settings
 * swipe-down gesture.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: LauncherUiState,
    onAppTap: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo, MenuOrigin, Int) -> Unit,
    onDockSlotLongPress: (Int) -> Unit,
    onEmptyPageLongPress: () -> Unit,
    onOpenDrawer: () -> Unit,
    onMoveAppToAdjacentPage: (AppInfo, Int) -> Unit
) {
    val pages = state.homePages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val dockSlots = remember(state.dockApps) {
        List(HomeLayoutRepository.DOCK_SIZE) { i -> state.dockApps.getOrNull(i) }
    }

    val density = LocalDensity.current
    val topExclusionPx = remember(density) { with(density) { TOP_GESTURE_EXCLUSION.toPx() } }
    val pageMoveThresholdPx = remember(density) { with(density) { PAGE_MOVE_THRESHOLD.toPx() } }
    val tapVsDragThresholdPx = remember(density) { with(density) { TAP_VS_DRAG_THRESHOLD.toPx() } }

    var dragAccum by remember { mutableStateOf(0f) }
    var dragArmed by remember { mutableStateOf(false) }

    var draggedApp by remember { mutableStateOf<AppInfo?>(null) }
    var dragStartPosition by remember { mutableStateOf(Offset.Zero) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            dragAccum = 0f
                            dragArmed = offset.y > topExclusionPx
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (dragArmed) {
                                change.consume()
                                dragAccum += dragAmount
                                if (dragAccum < -SWIPE_OPEN_THRESHOLD_PX) {
                                    dragArmed = false
                                    onOpenDrawer()
                                }
                            }
                        }
                    )
                }
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
                HomePage(
                    apps = pages.getOrElse(pageIndex) { emptyList() },
                    draggedApp = draggedApp,
                    onAppTap = onAppTap,
                    onEmptyLongPress = onEmptyPageLongPress,
                    onIconDragStart = { app, globalPosition ->
                        draggedApp = app
                        dragStartPosition = globalPosition
                        dragOffset = Offset.Zero
                    },
                    onIconDrag = { delta -> dragOffset += delta },
                    onIconDragEnd = {
                        val app = draggedApp
                        if (app != null) {
                            when {
                                dragOffset.getDistance() < tapVsDragThresholdPx ->
                                    onAppLongPress(app, MenuOrigin.HOME, -1)
                                dragOffset.x > pageMoveThresholdPx ->
                                    onMoveAppToAdjacentPage(app, +1)
                                dragOffset.x < -pageMoveThresholdPx ->
                                    onMoveAppToAdjacentPage(app, -1)
                            }
                        }
                        draggedApp = null
                        dragOffset = Offset.Zero
                    }
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

            SwipeUpHint()

            Dock(
                dockSlots = dockSlots,
                onAppTap = onAppTap,
                onAppLongPress = { app, slot -> onAppLongPress(app, MenuOrigin.DOCK, slot) },
                onEmptySlotLongPress = onDockSlotLongPress
            )
        }

        draggedApp?.let { app ->
            val position = dragStartPosition + dragOffset
            Box(
                modifier = Modifier.offset {
                    IntOffset(
                        (position.x - DRAG_ICON_SIZE.toPx() / 2).roundToInt(),
                        (position.y - DRAG_ICON_SIZE.toPx() / 2).roundToInt()
                    )
                }
            ) {
                AppIcon(app = app, size = DRAG_ICON_SIZE)
            }
        }
    }
}

/** Plain (non-lazy) grid: a home page holds a small, fixed number of icons that never
 * scroll, so the SubcomposeLayout machinery LazyVerticalGrid needs for real scrolling/
 * recycling only adds overhead here without buying anything. */
@Composable
private fun HomePage(
    apps: List<AppInfo>,
    draggedApp: AppInfo?,
    onAppTap: (AppInfo) -> Unit,
    onEmptyLongPress: () -> Unit,
    onIconDragStart: (AppInfo, Offset) -> Unit,
    onIconDrag: (Offset) -> Unit,
    onIconDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onEmptyLongPress() })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            apps.chunked(HOME_COLUMNS).forEach { rowApps ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowApps.forEach { app ->
                        Box(modifier = Modifier.weight(1f)) {
                            HomeIconTile(
                                app = app,
                                isBeingDragged = draggedApp?.componentName == app.componentName,
                                onTap = { onAppTap(app) },
                                onDragStart = { globalPosition -> onIconDragStart(app, globalPosition) },
                                onDrag = onIconDrag,
                                onDragEnd = onIconDragEnd
                            )
                        }
                    }
                    repeat(HOME_COLUMNS - rowApps.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Tap launches the app. A long-press starts tracking a drag immediately (the icon dims
 * to show it's "lifted"); the parent (HomeScreen) decides at release whether that turned
 * out to be a real drag (moved past TAP_VS_DRAG_THRESHOLD — moves the app to the previous/
 * next page if past PAGE_MOVE_THRESHOLD horizontally) or was just a long-press that never
 * really moved (shows the context menu, same as before). */
@Composable
private fun HomeIconTile(
    app: AppInfo,
    isBeingDragged: Boolean,
    onTap: () -> Unit,
    onDragStart: (globalPosition: Offset) -> Unit,
    onDrag: (delta: Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var globalTopLeft by remember { mutableStateOf(Offset.Zero) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { globalTopLeft = it.positionInRoot() }
            .alpha(if (isBeingDragged) 0.3f else 1f)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset -> onDragStart(globalTopLeft + localOffset) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
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

/** Purely decorative now — the actual gesture is handled once, on the whole screen, in
 * HomeScreen above. */
@Composable
private fun SwipeUpHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
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
    onEmptySlotLongPress: (Int) -> Unit
) {
    // All 5 slots (including the bottom-right one) are real, assignable apps — the
    // drawer only opens via the whole-screen swipe-up gesture, not a dedicated button.
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
        }
    }
}

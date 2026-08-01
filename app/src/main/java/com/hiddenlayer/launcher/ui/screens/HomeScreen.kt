package com.hiddenlayer.launcher.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
private val HOME_TILE_HEIGHT = 76.dp
private val PAGE_PADDING = 12.dp
private val ROW_SPACING = 12.dp
private val TOP_GESTURE_EXCLUSION = 56.dp
private const val SWIPE_OPEN_THRESHOLD_PX = 40f
private val PAGE_MOVE_THRESHOLD = 72.dp
private val TAP_VS_DRAG_THRESHOLD = 16.dp
private val DRAG_ICON_SIZE = 56.dp

/**
 * Swipe-up-to-open-drawer is a single gesture detector on the whole screen (not one per
 * dock/handle) so it doesn't compete with the pager's own horizontal drag detection on
 * every touch frame. A drag that starts within TOP_GESTURE_EXCLUSION of the top edge is
 * ignored, leaving that strip free for the system's notification-shade / quick-settings
 * swipe-down gesture.
 *
 * Dragging an icon between pages is tracked here rather than on the icon itself. Putting a
 * second gesture detector on the tile (a drag-after-long-press alongside the tap handler)
 * made the two cancel each other out: the tap detector consumes the down, which aborts the
 * long-press detector, so the drag never started AND the failed long press fell through to
 * the empty-area handler behind it — which is why long-pressing an icon sometimes produced
 * the wallpaper/"Home" menu with no "Nascondi app" in it. So the tile keeps one plain
 * combinedClickable (tap + long-click, the combination that always worked), long-click just
 * flags which app is being moved, and the drag itself is followed here on the *Initial*
 * pointer pass, which reaches this parent before any child can consume it.
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
    onMoveAppToAdjacentPage: (AppInfo, Int) -> Unit,
    onPageSizeChanged: (Int) -> Unit
) {
    val pages = state.homePages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val dockSlots = remember(state.dockApps) {
        List(HomeLayoutRepository.DOCK_SIZE) { i -> state.dockApps.getOrNull(i) }
    }

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val topExclusionPx = remember(density) { with(density) { TOP_GESTURE_EXCLUSION.toPx() } }
    val pageMoveThresholdPx = remember(density) { with(density) { PAGE_MOVE_THRESHOLD.toPx() } }
    val tapVsDragThresholdPx = remember(density) { with(density) { TAP_VS_DRAG_THRESHOLD.toPx() } }

    var swipeAccum by remember { mutableStateOf(0f) }
    var swipeArmed by remember { mutableStateOf(false) }

    var draggedApp by remember { mutableStateOf<AppInfo?>(null) }
    var dragOrigin by remember { mutableStateOf(Offset.Zero) }
    var dragCurrent by remember { mutableStateOf(Offset.Zero) }
    // Only true once the finger has actually travelled: a stationary long press shouldn't
    // make the icon dim and a copy of it jump under the finger, which read as a glitch.
    var dragVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var origin: Offset? = null

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (draggedApp != null) {
                            // Long-click just flagged an app as being moved: anchor the drag
                            // at wherever the finger is now, and swallow the events so the
                            // pager doesn't also start flinging between pages underneath.
                            if (origin == null) {
                                origin = change.position
                                dragOrigin = change.position
                            }
                            dragCurrent = change.position
                            if (!dragVisible && (dragCurrent - dragOrigin).getDistance() > tapVsDragThresholdPx) {
                                dragVisible = true
                            }
                            change.consume()
                        }
                        if (!change.pressed) break
                    }

                    val app = draggedApp
                    if (app != null) {
                        val moved = dragCurrent - dragOrigin
                        when {
                            moved.getDistance() < tapVsDragThresholdPx ->
                                onAppLongPress(app, MenuOrigin.HOME, -1)
                            moved.x > pageMoveThresholdPx -> onMoveAppToAdjacentPage(app, +1)
                            moved.x < -pageMoveThresholdPx -> onMoveAppToAdjacentPage(app, -1)
                        }
                        draggedApp = null
                        dragVisible = false
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            swipeAccum = 0f
                            swipeArmed = offset.y > topExclusionPx
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (swipeArmed && draggedApp == null) {
                                change.consume()
                                swipeAccum += dragAmount
                                if (swipeAccum < -SWIPE_OPEN_THRESHOLD_PX) {
                                    swipeArmed = false
                                    onOpenDrawer()
                                }
                            }
                        }
                    )
                }
        ) {
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                // As many rows as physically fit the screen, so a page fills up instead of
                // stopping at a hardcoded row count.
                val rowsPerPage = remember(maxHeight) {
                    ((maxHeight - PAGE_PADDING * 2 + ROW_SPACING) / (HOME_TILE_HEIGHT + ROW_SPACING))
                        .toInt()
                        .coerceAtLeast(1)
                }
                LaunchedEffect(rowsPerPage) { onPageSizeChanged(rowsPerPage * HOME_COLUMNS) }

                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                    HomePage(
                        apps = pages.getOrElse(pageIndex) { emptyList() },
                        draggedApp = if (dragVisible) draggedApp else null,
                        onAppTap = onAppTap,
                        onAppLongPress = { app ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            draggedApp = app
                        },
                        onEmptyLongPress = onEmptyPageLongPress
                    )
                }
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

        draggedApp?.takeIf { dragVisible }?.let { app ->
            Box(
                modifier = Modifier.offset {
                    IntOffset(
                        (dragCurrent.x - DRAG_ICON_SIZE.toPx() / 2).roundToInt(),
                        (dragCurrent.y - DRAG_ICON_SIZE.toPx() / 2).roundToInt()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = PAGE_PADDING),
            verticalArrangement = Arrangement.spacedBy(ROW_SPACING)
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
                                onLongPress = { onAppLongPress(app) }
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

/** One plain combinedClickable — no second competing gesture detector on the same tile.
 * Long-click hands the app off to HomeScreen, which decides on release whether it was a
 * drag between pages or a stationary long press that should open the context menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeIconTile(
    app: AppInfo,
    isBeingDragged: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(HOME_TILE_HEIGHT)
            .alpha(if (isBeingDragged) 0.3f else 1f)
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

/** Purely decorative — the actual gesture is handled once, on the whole screen, above. */
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

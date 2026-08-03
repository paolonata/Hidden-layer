package com.hiddenlayer.launcher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.MenuOrigin
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.data.HomeLayoutRepository
import com.hiddenlayer.launcher.ui.AppIcon
import com.hiddenlayer.launcher.ui.FocusPill
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

private const val HOME_COLUMNS = 4
// Solo l'icona, senza etichetta sotto: 76dp servivano a far stare anche il nome.
private val HOME_TILE_HEIGHT = 60.dp
private val PAGE_PADDING = 12.dp
private val ROW_SPACING = 12.dp
private val TOP_GESTURE_EXCLUSION = 56.dp
private const val SWIPE_OPEN_THRESHOLD_PX = 40f
private val PAGE_MOVE_THRESHOLD = 72.dp
private val TAP_VS_DRAG_THRESHOLD = 16.dp
private val DRAG_ICON_SIZE = 56.dp
private val DOCK_TOGGLE_THRESHOLD = 28.dp
private val DOCK_FALLBACK_HEIGHT = 132.dp

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
    onFocusShortcut: () -> Unit,
    onOpenFocus: () -> Unit,
    focusRemaining: StateFlow<Int>,
    onOpenDrawer: () -> Unit,
    onMoveAppToAdjacentPage: (AppInfo, Int) -> Unit,
    onDropOnDock: (AppInfo, Int) -> Unit,
    onDropOnHome: (AppInfo) -> Unit,
    onPageSizeChanged: (Int) -> Unit
) {
    val pages = state.homePages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val dockSlots = state.dockSlots

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val topExclusionPx = remember(density) { with(density) { TOP_GESTURE_EXCLUSION.toPx() } }
    val pageMoveThresholdPx = remember(density) { with(density) { PAGE_MOVE_THRESHOLD.toPx() } }
    val tapVsDragThresholdPx = remember(density) { with(density) { TAP_VS_DRAG_THRESHOLD.toPx() } }

    var swipeAccum by remember { mutableStateOf(0f) }
    var swipeArmed by remember { mutableStateOf(false) }

    // The second dock row is folded away until you swipe up on the dock itself. Knowing
    // where the dock starts is what keeps that gesture from colliding with the swipe-up
    // that opens the drawer: above this line opens the drawer, below it unfolds the dock.
    var dockExpanded by remember { mutableStateOf(false) }
    var dockZoneTop by remember { mutableStateOf(Float.MAX_VALUE) }
    // Top edge of each dock row, index 0 = the always-visible bottom one. Measured rather
    // than assumed, so a drop lands on the row the finger is actually over whatever the
    // row count happens to be.
    val dockRowTops = remember {
        mutableStateListOf<Float>().apply { repeat(HomeLayoutRepository.DOCK_ROWS) { add(Float.MAX_VALUE) } }
    }
    var rootWidthPx by remember { mutableStateOf(0f) }
    var rootHeightPx by remember { mutableStateOf(0f) }
    // Dock icons can be dragged too, so the release handler has to know where the icon came
    // from: back onto the grid means "take it out of the dock", not "move it a page along".
    var draggedFromDock by remember { mutableStateOf(false) }
    var draggedDockSlot by remember { mutableStateOf(-1) }

    var draggedApp by remember { mutableStateOf<AppInfo?>(null) }
    var dragOrigin by remember { mutableStateOf(Offset.Zero) }
    var dragCurrent by remember { mutableStateOf(Offset.Zero) }
    // Only true once the finger has actually travelled: a stationary long press shouldn't
    // make the icon dim and a copy of it jump under the finger, which read as a glitch.
    var dragVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                rootWidthPx = it.size.width.toFloat()
                rootHeightPx = it.size.height.toFloat()
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var origin: Offset? = null

                    // Con le file a scomparsa del dock aperte, il primo gesto che parte fuori
                    // dal dock serve solo a richiuderle — tocco o trascinamento che sia, come
                    // ci si aspetta da un pannello aperto. Sta qui e non su un velo a parte
                    // proprio per non aggiungere un secondo detector che si contenda i tocchi
                    // con le icone: siamo sul pass Initial, che arriva a questo genitore prima
                    // che una tile possa consumare il tocco e lanciare l'app che hai sfiorato.
                    val dismissingDock = dockExpanded && down.position.y < dockZoneTop
                    // Un long press sulla griglia trasforma il gesto in un trascinamento, e
                    // allora la chiusura del dock deve farsi da parte: altrimenti con le file
                    // aperte non ci sarebbe alcun modo di portarci sopra un'icona della home.
                    var draggingStarted = false

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (draggedApp != null) {
                            draggingStarted = true
                            // Long-click just flagged an app as being moved: anchor the drag
                            // at wherever the finger is now, and swallow the events so the
                            // pager doesn't also start flinging between pages underneath.
                            //
                            // L'ancoraggio qui è anche ciò che impedisce di riusare le
                            // coordinate del gesto precedente: senza, un long press fermo
                            // rilasciava usando dragCurrent vecchio e spostava l'app dove era
                            // finito il trascinamento di prima.
                            if (origin == null) {
                                origin = change.position
                                dragOrigin = change.position
                            }
                            dragCurrent = change.position
                            if (!dragVisible && (dragCurrent - dragOrigin).getDistance() > tapVsDragThresholdPx) {
                                dragVisible = true
                            }
                            change.consume()
                        } else if (dismissingDock) {
                            change.consume()
                        }
                        if (!change.pressed) break
                    }

                    if (dismissingDock && !draggingStarted) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        dockExpanded = false
                    }

                    val app = draggedApp
                    if (app != null) {
                        val moved = dragCurrent - dragOrigin
                        // Belt and braces: if the dock never reported its position, fall
                        // back to treating the bottom slice of the screen as the dock.
                        val dockTop = if (dockZoneTop == Float.MAX_VALUE) {
                            rootHeightPx - with(density) { DOCK_FALLBACK_HEIGHT.toPx() }
                        } else {
                            dockZoneTop
                        }
                        val overDock = rootHeightPx > 0f && dragCurrent.y > dockTop

                        when {
                            moved.getDistance() < tapVsDragThresholdPx -> onAppLongPress(
                                app,
                                if (draggedFromDock) MenuOrigin.DOCK else MenuOrigin.HOME,
                                draggedDockSlot
                            )

                            // Released over the dock: work out which slot it landed on from
                            // the drop position — column from x, and which of the two rows
                            // from y when the fold-out row is open.
                            overDock && rootWidthPx > 0f -> {
                                val columns = HomeLayoutRepository.DOCK_COLUMNS
                                val column = ((dragCurrent.x / rootWidthPx) * columns)
                                    .toInt()
                                    .coerceIn(0, columns - 1)
                                val row = if (!dockExpanded) {
                                    0
                                } else {
                                    dockRowTops.indices
                                        .filter { dockRowTops[it] <= dragCurrent.y }
                                        .maxByOrNull { dockRowTops[it] }
                                        ?: 0
                                }
                                onDropOnDock(app, row * columns + column)
                            }

                            // Dragged out of the dock and dropped on the grid.
                            draggedFromDock -> onDropOnHome(app)

                            moved.x > pageMoveThresholdPx -> onMoveAppToAdjacentPage(app, +1)
                            moved.x < -pageMoveThresholdPx -> onMoveAppToAdjacentPage(app, -1)
                        }
                        draggedApp = null
                        dragVisible = false
                        draggedFromDock = false
                        draggedDockSlot = -1
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
                            // Below the dock the swipe belongs to the dock's second row.
                            swipeArmed = offset.y > topExclusionPx && offset.y < dockZoneTop
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
                            draggedFromDock = false
                            draggedDockSlot = -1
                        },
                        onEmptyLongPress = onEmptyPageLongPress,
                        onEmptyDoubleTap = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFocusShortcut()
                        },
                        isMuted = state::isMuted
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

            // Conferma di avvio, non un elemento fisso: resta qualche secondo e sparisce.
            AnimatedVisibility(
                visible = state.focusToastVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FocusPill(remaining = focusRemaining, onClick = onOpenFocus)
                }
            }

            Dock(
                dockSlots = dockSlots,
                expanded = dockExpanded,
                onExpandedChange = { expanded ->
                    if (expanded != dockExpanded) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        dockExpanded = expanded
                    }
                },
                onZonePositioned = { top -> dockZoneTop = top },
                onRowPositioned = { rowIndex, top -> dockRowTops[rowIndex] = top },
                onAppTap = onAppTap,
                draggedApp = if (dragVisible) draggedApp else null,
                isMuted = state::isMuted,
                onAppLongPress = { app, slot ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    draggedApp = app
                    draggedFromDock = true
                    draggedDockSlot = slot
                },
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
    isMuted: (AppInfo) -> Boolean,
    onAppTap: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    onEmptyLongPress: () -> Unit,
    onEmptyDoubleTap: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Il doppio tap vive qui, sullo sfondo dietro la griglia: le icone hanno il
                // loro combinedClickable e consumano il tocco per prime, quindi un doppio tap
                // su un'app resta due aperture e non fa partire niente per sbaglio.
                detectTapGestures(
                    onDoubleTap = { onEmptyDoubleTap() },
                    onLongPress = { onEmptyLongPress() }
                )
            }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(HOME_COLUMNS),
            userScrollEnabled = false,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = PAGE_PADDING),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps, key = { it.componentName.flattenToString() }) { app ->
                HomeIconTile(
                    app = app,
                    isBeingDragged = draggedApp?.componentName == app.componentName,
                    onTap = { onAppTap(app) },
                    onLongPress = { onAppLongPress(app) },
                    grayscale = isMuted(app),
                    // Icons slide to their new spot when one leaves or joins the page,
                    // instead of snapping there in a single frame.
                    modifier = Modifier.animateItem()
                )
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
    onLongPress: () -> Unit,
    grayscale: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(HOME_TILE_HEIGHT)
            .alpha(if (isBeingDragged) 0.3f else 1f)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .padding(4.dp)
    ) {
        AppIcon(app = app, size = 48.dp, grayscale = grayscale)
    }
}

/**
 * Two rows of five. The bottom row is always on screen; the row above it stays folded away
 * until you swipe up on the dock, and folds back on a swipe down. The gesture lives on this
 * surface only, and HomeScreen is told where the surface starts (onZonePositioned) so its
 * own swipe-up — the one that opens the drawer — stands down for anything starting here.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Dock(
    dockSlots: List<AppInfo?>,
    draggedApp: AppInfo?,
    isMuted: (AppInfo) -> Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onZonePositioned: (Float) -> Unit,
    onRowPositioned: (Int, Float) -> Unit,
    onAppTap: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo, Int) -> Unit,
    onEmptySlotLongPress: (Int) -> Unit
) {
    val density = LocalDensity.current
    val toggleThresholdPx = remember(density) { with(density) { DOCK_TOGGLE_THRESHOLD.toPx() } }
    var dragAccum by remember { mutableStateOf(0f) }

    Surface(
        color = Color.Black.copy(alpha = 0.25f),
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { onZonePositioned(it.positionInRoot().y) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccum += dragAmount
                        if (dragAccum < -toggleThresholdPx) {
                            dragAccum = 0f
                            onExpandedChange(true)
                        } else if (dragAccum > toggleThresholdPx) {
                            dragAccum = 0f
                            onExpandedChange(false)
                        }
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DockHandle(expanded = expanded)

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Topmost row first: row 0 is the one nearest the bottom of the screen.
                    for (rowIndex in HomeLayoutRepository.DOCK_ROWS - 1 downTo 1) {
                        DockRow(
                            slots = dockSlots.rowSlots(rowIndex),
                            slotOffset = rowIndex * HomeLayoutRepository.DOCK_COLUMNS,
                            draggedApp = draggedApp,
                            isMuted = isMuted,
                            onAppTap = onAppTap,
                            onAppLongPress = onAppLongPress,
                            onEmptySlotLongPress = onEmptySlotLongPress,
                            modifier = Modifier.onGloballyPositioned {
                                onRowPositioned(rowIndex, it.positionInRoot().y)
                            }
                        )
                    }
                }
            }

            DockRow(
                slots = dockSlots.rowSlots(0),
                slotOffset = 0,
                draggedApp = draggedApp,
                isMuted = isMuted,
                onAppTap = onAppTap,
                onAppLongPress = onAppLongPress,
                onEmptySlotLongPress = onEmptySlotLongPress,
                modifier = Modifier
                    .navigationBarsPadding()
                    .onGloballyPositioned { onRowPositioned(0, it.positionInRoot().y) }
            )
        }
    }
}

private fun List<AppInfo?>.rowSlots(rowIndex: Int): List<AppInfo?> {
    val columns = HomeLayoutRepository.DOCK_COLUMNS
    return List(columns) { getOrNull(rowIndex * columns + it) }
}

/** Doubles as the affordance for the fold-out row: nothing sits on top of it to steal the
 * touch, and it widens slightly when the second row is out. */
@Composable
private fun DockHandle(expanded: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(if (expanded) 22.dp else 36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.6f))
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockRow(
    slots: List<AppInfo?>,
    slotOffset: Int,
    draggedApp: AppInfo?,
    isMuted: (AppInfo) -> Boolean,
    onAppTap: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo, Int) -> Unit,
    onEmptySlotLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        slots.forEachIndexed { index, app ->
            val slot = slotOffset + index
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .combinedClickable(
                        onClick = { app?.let(onAppTap) },
                        onLongClick = {
                            if (app != null) onAppLongPress(app, slot) else onEmptySlotLongPress(slot)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (app != null) {
                    AppIcon(
                        app = app,
                        size = 44.dp,
                        grayscale = isMuted(app),
                        modifier = Modifier.alpha(
                            if (draggedApp?.componentName == app.componentName) 0.3f else 1f
                        )
                    )
                } else {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Aggiungi al dock",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

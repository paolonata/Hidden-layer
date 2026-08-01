package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val CLOSE_ON_PULL_THRESHOLD = 72.dp
private val DRAG_DOWN_TO_CLOSE_THRESHOLD = 48.dp

/**
 * A NestedScrollConnection that closes the current screen once the user pulls down past
 * [CLOSE_ON_PULL_THRESHOLD] worth of scroll the child list didn't consume itself — i.e.
 * only once a LazyVerticalGrid/LazyColumn is already scrolled to its top and the user
 * keeps dragging down, mirroring the swipe-up gesture that opened it. Using nested scroll
 * (rather than a second raw pointer-drag detector) means it never fights the list's own
 * scrolling: it only ever sees the delta the list had nothing to do with.
 */
@Composable
fun rememberCloseOnPullDown(onClose: () -> Unit): NestedScrollConnection {
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { CLOSE_ON_PULL_THRESHOLD.toPx() } }
    var accumulated by remember { mutableStateOf(0f) }

    return remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0f) {
                    accumulated += available.y
                    if (accumulated > thresholdPx) {
                        accumulated = 0f
                        onClose()
                    }
                } else {
                    accumulated = 0f
                }
                return Offset.Zero
            }
        }
    }
}

/**
 * A plain drag-down-to-close gesture, meant for a non-scrollable zone (like a TopAppBar) —
 * unlike [rememberCloseOnPullDown], this doesn't depend on nested scroll dispatch from a
 * list, so it's unambiguous and always fires. It's the reliable way to close; the
 * nested-scroll version on the list itself is a bonus for pulling from within the content.
 */
@Composable
fun Modifier.dragDownToClose(onClose: () -> Unit): Modifier {
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { DRAG_DOWN_TO_CLOSE_THRESHOLD.toPx() } }
    var dragAccum by remember { mutableStateOf(0f) }

    return this.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragStart = { dragAccum = 0f },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                dragAccum += dragAmount
                if (dragAccum > thresholdPx) {
                    dragAccum = 0f
                    onClose()
                }
            }
        )
    }
}

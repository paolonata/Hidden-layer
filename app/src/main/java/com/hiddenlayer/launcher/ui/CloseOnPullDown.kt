package com.hiddenlayer.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val CLOSE_ON_PULL_THRESHOLD = 72.dp

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

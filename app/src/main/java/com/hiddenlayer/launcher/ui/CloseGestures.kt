package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hiddenlayer.launcher.ui.theme.HlPaper
import kotlin.math.abs

private val CLOSE_DRAG_THRESHOLD = 64.dp
private val HANDLE_DRAG_THRESHOLD = 32.dp

/**
 * Closes the screen when the user drags down far enough, from anywhere on it.
 *
 * The catch this solves: an ordinary `pointerInput` on a parent never sees the gesture when
 * an interactive child sits on top of it — a text field, a button, a scrollable grid —
 * because pointer events reach children first in the Main pass and are consumed there. That
 * is exactly why hanging this off the TopAppBar (which is almost entirely covered by the
 * search field) silently did nothing. So this observes the *Initial* pass instead, which is
 * dispatched parent-first, before any child can consume.
 *
 * It never consumes the events it sees, so typing in the search field and scrolling the grid
 * keep behaving exactly as before — this only watches. [canClose] gates it on the content
 * being scrolled to the top, so pulling down mid-list scrolls the list instead of closing.
 */
@Composable
fun Modifier.closeOnDragDown(canClose: () -> Boolean, onClose: () -> Unit): Modifier {
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { CLOSE_DRAG_THRESHOLD.toPx() } }

    return this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var totalDy = 0f
            var totalDx = 0f
            var fired = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                val delta = change.positionChange()
                totalDy += delta.y
                totalDx += delta.x

                if (!fired && totalDy > thresholdPx && abs(totalDy) > abs(totalDx) && canClose()) {
                    fired = true
                    onClose()
                }
                if (!change.pressed) break
            }
        }
    }
}

/**
 * The pill at the top of an open drawer: both a visible "you can pull this down" affordance
 * and a guaranteed-to-work drag target, since nothing sits on top of it to steal the touch.
 */
@Composable
fun DragHandle(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { HANDLE_DRAG_THRESHOLD.toPx() } }
    var dragAccum by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
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
            },
        contentAlignment = Alignment.Center
    ) {
        // 26×2dp e non più 40×4: resta un'affordance riconoscibile senza essere una barra
        // luminosa in cima a ogni schermata. L'area di tocco è il Box da 28dp che la
        // contiene, quindi rimpicciolire il disegno non rende il gesto più difficile.
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(HlPaper.copy(alpha = 0.28f))
        )
    }
}

package com.hiddenlayer.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private const val PAUSE_MILLIS = 3_000

private val RING_SIZE = 76.dp
private val RING_STROKE = 2.5.dp

/**
 * Il respiro dopo uno sblocco durante la Concentrazione.
 *
 * Sfondo **sfocato**, lo stesso di cassetto, Concentrazione e impostazioni: è il linguaggio
 * visivo del launcher, e nasconde la griglia sotto tanto quanto farebbe un nero pieno — che
 * era la versione precedente, corretta nella sostanza ma fuori posto rispetto a tutto il
 * resto. Vedere la griglia già lì, anche solo intravista, sarebbe un invito a partire prima
 * che l'anello finisca; una sfocatura non lascia niente da mirare.
 *
 * Nessun testo, nessuna domanda — non è un rimprovero, e ripetuto più volte al giorno una
 * domanda ("perché hai preso il telefono?") si sarebbe consumata in fretta. Il punto non è
 * fare riflettere, è interrompere il gesto automatico "sblocco → tocco la prima cosa che
 * vedo" con un momento fermo.
 *
 * Non è annullabile — né toccando lo schermo né con indietro — perché l'unica cosa che conta
 * qui è che i secondi passino: un modo per saltarlo è un modo per non farlo mai.
 *
 * L'anello è disegnato a mano con `Canvas` invece che con l'indicatore di Material3: qui non
 * si può compilare in locale per verificare quali parametri esistono nella versione del BOM
 * in uso (vedi CLAUDE.md, §2), quindi si evita di scommettere su un'API che potrebbe non
 * esserci ancora — un `drawArc` su primitive stabili da sempre non ha questo rischio.
 */
@Composable
fun UnlockPauseOverlay() {
    BackHandler {}

    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { progress = 1f }
    val sweep by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = PAUSE_MILLIS, easing = LinearEasing),
        label = "unlock-pause-sweep"
    )

    // Il respiro vero e proprio: l'anello pulsa piano, come un inspira/espira. È l'unica cosa
    // in movimento oltre al riempimento, e dà allo schermo fermo un ritmo invece di lasciarlo
    // semplicemente bloccato.
    val breath = rememberInfiniteTransition(label = "unlock-pause-breath")
    val breathScale by breath.animateFloat(
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "unlock-pause-breath-scale"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BlurredWallpaperBackground(scrimAlpha = 0.62f)

        Canvas(modifier = Modifier.size(RING_SIZE)) {
            val stroke = Stroke(width = RING_STROKE.toPx(), cap = StrokeCap.Round)
            val inset = (size.minDimension * (1f - breathScale)) / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                size.minDimension - inset * 2f,
                size.minDimension - inset * 2f
            )
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)

            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            drawArc(
                color = Color.White.copy(alpha = 0.88f),
                startAngle = -90f,
                sweepAngle = 360f * sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }
    }
}

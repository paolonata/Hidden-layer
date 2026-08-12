package com.hiddenlayer.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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

/**
 * Il respiro dopo uno sblocco durante la Concentrazione: nero pieno, un anello che si
 * riempie, niente altro.
 *
 * Nessun testo, nessuna domanda — non è un rimprovero, e ripetuto più volte al giorno una
 * domanda ("perché hai preso il telefono?") si sarebbe consumata in fretta. Il punto non è
 * fare riflettere, è solo interrompere il gesto automatico "sblocco → tocco la prima cosa che
 * vedo" con un momento fermo.
 *
 * Sfondo **opaco**, non un velo sulla home sottostante: vedere la griglia già lì, anche
 * offuscata, sarebbe un invito a partire prima che l'anello finisca. Non è annullabile — né
 * toccando lo schermo né con indietro — perché l'unica cosa che conta qui è che i secondi
 * passino: un modo per saltarlo è un modo per non farlo mai.
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
    LaunchedEffect(Unit) {
        progress = 1f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = PAUSE_MILLIS, easing = LinearEasing),
        label = "unlock-pause"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(40.dp)) {
            val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = Color.White.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = Color.White.copy(alpha = 0.85f),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = stroke
            )
        }
    }
}

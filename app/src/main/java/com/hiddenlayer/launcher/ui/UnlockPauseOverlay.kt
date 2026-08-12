package com.hiddenlayer.launcher.ui

import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Il respiro dopo uno sblocco durante la Concentrazione: una macchia opaca che si allarga dal
 * centro e scopre lo schermo poco per volta, invece di un velo che sparisce di colpo.
 *
 * Il tempo non è fisso: [durationMillis] cresce a ogni sblocco della stessa sessione, perché è
 * la ripetizione a essere il sintomo — il primo sblocco può avere un motivo, il quinto molto
 * meno.
 *
 * ### Perché è un Dialog e non un semplice overlay in composizione
 *
 * La finestra del launcher non arriva sotto la barra di stato e quella di navigazione, quindi
 * un `Box(fillMaxSize)` dentro `LauncherApp` lasciava scoperte due strisce in cima e in fondo
 * — si vedeva lo sfondo nitido mentre il resto era sfocato. Un `Dialog` è una finestra a sé,
 * e con `FLAG_LAYOUT_NO_LIMITS` si estende oltre le barre di sistema coprendo davvero tutto.
 * La strada alternativa — portare *tutto* il launcher edge-to-edge — avrebbe cambiato il
 * layout di ogni schermata per risolvere un problema che riguarda solo questa.
 *
 * Non è annullabile — né toccando fuori, né con indietro (`dismissOn*` a false) — perché
 * l'unica cosa che conta qui è che i secondi passino: un modo per saltarlo è un modo per non
 * farlo mai.
 */
@Composable
fun UnlockPauseOverlay(durationMillis: Int) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.apply {
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                // Niente oscuramento di sistema dietro: l'unica cosa che scurisce è la
                // macchia, e un dim aggiuntivo la renderebbe grigia invece che pulita.
                setDimAmount(0f)
                addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            }
        }

        var revealed by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(Unit) { revealed = 1f }
        val reveal by animateFloatAsState(
            targetValue = revealed,
            // Parte piano e accelera verso la fine: lo schermo resta chiuso per la maggior
            // parte dell'attesa e si apre nell'ultimo tratto, che è dove deve stare il peso.
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            label = "unlock-pause-reveal"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Il buco si ritaglia con BlendMode.Clear, che agisce solo dentro un layer
                // proprio: senza Offscreen cancellerebbe anche ciò che sta sotto nel buffer.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            BlurredWallpaperBackground(scrimAlpha = 0.62f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Il raggio finale copre l'angolo più lontano, così alla fine non resta
                // nessun bordo velato.
                val maxRadius = kotlin.math.hypot(size.width, size.height) / 2f
                val radius = maxRadius * reveal
                if (radius <= 0f) return@Canvas

                // Bordo sfumato invece che netto: è una macchia che si allarga, non un
                // cerchio che si ingrandisce.
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color.Black,
                            0.72f to Color.Black,
                            1f to Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center,
                    blendMode = BlendMode.Clear
                )
            }
        }
    }
}

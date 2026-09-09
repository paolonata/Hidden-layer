package com.hiddenlayer.launcher.ui

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.hiddenlayer.launcher.ui.theme.HlPaper
import com.hiddenlayer.launcher.ui.theme.HlWideMargin

/** Quanto ci mette la macchia ad aprirsi una volta che hai chiesto il telefono. */
private const val REVEAL_MILLIS = 900

/**
 * La home chiusa dopo uno sblocco durante la Concentrazione.
 *
 * **Non si riapre da sola, e questo è il punto.** Prima era un'attesa di qualche secondo:
 * non funzionava, perché un ritardo passivo non interrompe l'impulso — guardi lo schermo
 * pensando all'app che volevi aprire, e poi la apri lo stesso. Qui la griglia non risponde
 * finché non chiedi il telefono esplicitamente, che è una decisione invece che un'attesa.
 *
 * La via d'uscita c'è **sempre**, di proposito: l'utente vuole poter usare il telefono quando
 * serve davvero, non essere bloccato fuori come farebbe un'app di blocco vera. Quello che
 * cambia rispetto a prima è che passare costa un gesto intenzionale e lascia un segno — il
 * conteggio delle volte, che è l'unica cosa che rende visibile l'abitudine.
 *
 * ### Perché è un Dialog
 *
 * La finestra del launcher non arriva sotto la barra di stato e quella di navigazione, quindi
 * un overlay in composizione lasciava scoperte due strisce. Un `Dialog` è una finestra a sé:
 * con `FLAG_LAYOUT_NO_LIMITS` **e** `layoutInDisplayCutoutMode` copre davvero tutto, notch
 * compreso — senza il secondo il sistema tiene comunque la finestra sotto il ritaglio, ed era
 * il bordo superiore che restava scoperto. Essendo una finestra a parte, blocca anche i tocchi
 * diretti alla home sottostante senza bisogno di disabilitare niente nella griglia.
 */
@Composable
fun HomeLockOverlay(requestsSoFar: Int, onUnlock: () -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Nessuna scorciatoia: né indietro né toccando fuori. L'unica uscita è quella
            // dichiarata, altrimenti non sarebbe una decisione.
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
                setDimAmount(0f)
                addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    attributes = attributes.apply {
                        layoutInDisplayCutoutMode =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                            } else {
                                WindowManager.LayoutParams
                                    .LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                            }
                    }
                }
            }
        }

        var opening by remember { mutableStateOf(false) }
        val reveal by animateFloatAsState(
            targetValue = if (opening) 1f else 0f,
            animationSpec = tween(durationMillis = REVEAL_MILLIS, easing = FastOutSlowInEasing),
            // La home torna disponibile solo a macchia completamente aperta: sbloccarla prima
            // farebbe apparire la griglia sotto un velo ancora mezzo chiuso.
            finishedListener = { if (it >= 1f) onUnlock() },
            label = "home-lock-reveal"
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Il buco si ritaglia con BlendMode.Clear, che agisce solo dentro un layer
                    // proprio: senza Offscreen cancellerebbe anche ciò che sta sotto nel buffer.
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
                BlurredWallpaperBackground(scrimAlpha = 0.88f)

                if (reveal > 0f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
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

            // In basso a sinistra, non al centro. Al centro era una finestra di dialogo
            // travestita; qui il testo comincia dove comincia ogni altra riga del launcher, e
            // il pulsante finisce dove il pollice è già — che conta, visto che è l'unica cosa
            // toccabile della schermata.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .systemBarsPadding()
                    .padding(start = HlWideMargin, end = HlWideMargin, bottom = 60.dp)
                    // Sparisce mentre la macchia si apre: resta solo lo schermo che si scopre.
                    .alpha(1f - reveal)
            ) {
                Text(
                    text = "SESSIONE IN CORSO",
                    color = HlPaper.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.8.sp
                )

                Spacer(Modifier.height(18.dp))

                // Due righe grandi e sottili al posto della didascalia che c'era prima: è la
                // sola cosa che questa schermata ha da dire, e detta piano si legge davvero
                // invece di essere scavalcata come un avviso di sistema.
                Text(
                    text = "La home è chiusa.\nPer usare il telefono, chiedilo.",
                    color = HlPaper,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.W200,
                    lineHeight = 42.sp
                )

                Spacer(Modifier.height(28.dp))

                OutlinePill(
                    text = "Mi serve il telefono",
                    onClick = { if (!opening) opening = true }
                )

                // Dalla seconda volta in poi: la prima può avere un motivo, è la ripetizione a
                // essere il sintomo. Detto e basta, senza rimproveri — il numero parla da solo.
                if (requestsSoFar >= 1) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Sarebbe la ${requestsSoFar + 1}ª volta in questa sessione",
                        color = HlPaper.copy(alpha = 0.38f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

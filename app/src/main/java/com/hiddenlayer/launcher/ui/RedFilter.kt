package com.hiddenlayer.launcher.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Il rosso **esatto** dentro il launcher, che l'overlay di sistema non può ottenere.
 *
 * Qui disegniamo noi, quindi possiamo usare `BlendMode.Modulate` — il multiply vero — sul
 * contenuto già disegnato: ogni pixel viene moltiplicato per il rosso, e i canali verde e blu
 * finiscono a **zero**, non "attenuati". Su un pannello OLED vuol dire che i subpixel verdi e
 * blu restano spenti, che è esattamente ciò che serve per non perdere l'adattamento al buio.
 *
 * Fuori dal launcher questo non è replicabile: la composizione fra finestre la fa
 * SurfaceFlinger con alpha blending e basta, quindi lì resta un velo (vedi `RedOverlayService`).
 *
 * [CompositingStrategy.Offscreen] serve a confinare il blend: senza, il multiply andrebbe a
 * finire su ciò che è già nel buffer sotto di noi invece che solo sul nostro contenuto.
 */
fun Modifier.redFilter(enabled: Boolean): Modifier {
    if (!enabled) return this
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(color = RED_TINT, blendMode = BlendMode.Modulate)
        }
}

/**
 * Non rosso puro: un filo di verde tiene in vita l'antialiasing del testo, che a canale
 * singolo secco diventa seghettato e faticoso da leggere al buio. Il blu resta a zero, ed è
 * quello che conta per la visione notturna.
 */
private val RED_TINT = Color(0xFFFF1A00)

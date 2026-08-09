package com.hiddenlayer.launcher.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp

/**
 * Il rosso **esatto** dentro il launcher, che l'overlay di sistema non può ottenere.
 *
 * Qui disegniamo noi, quindi possiamo usare `BlendMode.Modulate` — il multiply vero — sul
 * contenuto già disegnato: ogni pixel viene moltiplicato per il colore del filtro. A
 * [redIntensity] massima il verde e il blu finiscono a **zero**, non "attenuati" — su un
 * pannello OLED vuol dire che quei subpixel restano proprio spenti, che è esattamente ciò che
 * serve per non perdere l'adattamento al buio. A intensità più basse il tinta si avvicina al
 * bianco (moltiplicare per bianco è l'identità), quindi il filtro sfuma con continuità invece
 * di essere acceso o spento.
 *
 * [dimLevel] è un secondo strato, non lo stesso: un velo nero sopra il rosso già applicato,
 * che toglie luce invece di togliere colore. Le due manopole restano separate perché è il blu
 * a rompere l'adattamento al buio, non la luminosità — vedi `NightModeRepository`.
 *
 * Fuori dal launcher questo non è replicabile: la composizione fra finestre la fa
 * SurfaceFlinger con alpha blending e basta, quindi lì resta un velo (vedi `RedOverlayService`).
 *
 * [CompositingStrategy.Offscreen] serve a confinare il blend: senza, il multiply andrebbe a
 * finire su ciò che è già nel buffer sotto di noi invece che solo sul nostro contenuto.
 */
fun Modifier.redFilter(enabled: Boolean, redIntensity: Float, dimLevel: Float): Modifier {
    if (!enabled) return this
    val tint = lerp(Color.White, RED_TINT, redIntensity.coerceIn(0f, 1f))
    val dim = dimLevel.coerceIn(0f, 1f)
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawRect(color = tint, blendMode = BlendMode.Modulate)
            if (dim > 0f) drawRect(color = Color.Black.copy(alpha = dim))
        }
}

/**
 * Il rosso a intensità piena. Non rosso puro: un filo di verde tiene in vita l'antialiasing
 * del testo, che a canale singolo secco diventa seghettato e faticoso da leggere al buio. Il
 * blu resta a zero, ed è quello che conta per la visione notturna.
 */
private val RED_TINT = Color(0xFFFF1A00)

/**
 * Sfondo piatto e **davvero neutro** (R = G = B) da mostrare al posto del vero sfondo quando
 * la modalità rossa è accesa.
 *
 * Il vero sfondo — una foto di cielo notturno, spesso già rossa o arancione di suo (nebulose a
 * emissione) — non si limita a diventare "rosso": resta la stessa foto, con tutto il suo
 * dettaglio e i suoi colori originali che il multiply non azzera (moltiplicare del rosso per
 * del rosso dà ancora rosso). Il risultato è illeggibile: sembra la foto originale con un
 * filtro sopra, non un pannello da strumentazione notturna. Un fondo neutro invece, moltiplicato
 * per lo stesso rosso, dà un rosso **piatto e uniforme** — le icone restano l'unica cosa a
 * risaltare.
 */
val NightNeutralBackground = Color(0xFF262626)

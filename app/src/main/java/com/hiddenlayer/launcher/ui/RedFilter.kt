package com.hiddenlayer.launcher.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/**
 * Il rosso **esatto** dentro il launcher, che l'overlay di sistema non può ottenere.
 *
 * ### Perché non è un multiply
 *
 * La prima versione moltiplicava il contenuto per il rosso. Sembra la cosa ovvia, ma un
 * multiply per un rosso puro **tiene solo il canale rosso e butta via gli altri due**: tutto
 * ciò che differiva solo per verde e blu diventa identico. Bianco `(1, 1, 1)` e rosso acceso
 * `(0.9, 0.3, 0.1)` finiscono entrambi a rosso quasi massimo, quindi una scritta bianca su
 * un'icona rossa **scompare dentro il suo stesso sfondo** — bug reale, l'icona ASIAIR era
 * illeggibile.
 *
 * La cosa giusta è passare per la **luminanza**: prima si calcola quanto è chiaro un pixel
 * (`0.2126·R + 0.7152·G + 0.0722·B`, i pesi con cui l'occhio percepisce la luce), poi si
 * riversa quel valore sul solo canale rosso. Ora bianco e rosso acceso restano distinti,
 * perché lo erano in luminanza: il bianco ha luminanza 1.0, quel rosso circa 0.41. Il
 * contrasto originale sopravvive, tradotto in tonalità di rosso.
 *
 * Il blu resta a **zero**, che è l'unica cosa che conta davvero per l'adattamento al buio; il
 * verde resta a una frazione minima ([GREEN_RESIDUE]) perché l'antialiasing del testo su un
 * canale solo diventa seghettato e faticoso da leggere.
 *
 * [redIntensity] interpola con l'identità, quindi il filtro sfuma con continuità invece di
 * essere acceso o spento. [dimLevel] è un secondo strato, non lo stesso: un velo nero sopra
 * il tutto, che toglie luce invece di togliere colore.
 *
 * Fuori dal launcher niente di questo è replicabile: la composizione fra finestre la fa
 * SurfaceFlinger con alpha blending e basta (vedi `RedOverlayService`).
 */
fun Modifier.redFilter(enabled: Boolean, redIntensity: Float, dimLevel: Float): Modifier {
    if (!enabled) return this
    val intensity = redIntensity.coerceIn(0f, 1f)
    val dim = dimLevel.coerceIn(0f, 1f)

    return this.drawWithContent {
        drawIntoCanvas { canvas ->
            // saveLayer + colorFilter applica la matrice a **tutto** ciò che viene disegnato
            // dentro, in un colpo solo. Funziona da API 26, a differenza di RenderEffect.
            val paint = Paint().apply {
                colorFilter = ColorFilter.colorMatrix(nightVisionMatrix(intensity))
            }
            canvas.saveLayer(Rect(Offset.Zero, size), paint)
            drawContent()
            canvas.restore()
        }
        if (dim > 0f) drawRect(color = Color.Black.copy(alpha = dim))
    }
}

// Pesi standard della luminanza percepita (Rec. 709): l'occhio è molto più sensibile al verde
// che al blu, quindi una media semplice dei tre canali darebbe un grigio sbagliato.
private const val LUMA_R = 0.2126f
private const val LUMA_G = 0.7152f
private const val LUMA_B = 0.0722f

/** Quanto verde sopravvive. Serve solo a non rendere seghettato l'antialiasing del testo: il
 * blu, che è quello che rovina la visione notturna, resta a zero. */
private const val GREEN_RESIDUE = 0.06f

/** Interpola fra la matrice identità (nessun effetto) e la conversione luminanza → rosso. */
private fun nightVisionMatrix(t: Float): ColorMatrix {
    fun mix(target: Float, identity: Float) = identity + (target - identity) * t
    return ColorMatrix(
        floatArrayOf(
            mix(LUMA_R, 1f), mix(LUMA_G, 0f), mix(LUMA_B, 0f), 0f, 0f,
            mix(LUMA_R * GREEN_RESIDUE, 0f), mix(LUMA_G * GREEN_RESIDUE, 1f), mix(LUMA_B * GREEN_RESIDUE, 0f), 0f, 0f,
            0f, 0f, mix(0f, 1f), 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

/**
 * Sfondo da mostrare al posto di quello vero quando la modalità rossa è accesa: **nero puro**.
 *
 * Prima era un grigio (`0xFF262626`), che filtrato dava un fondo rosso spento e uniforme —
 * leggibile, ma "lavato". Le app con tema rosso nativo, quelle che l'utente ha in mano al
 * telescopio, non fanno così: disegnano **rosso su nero**, e il nero è nero davvero. Su OLED è
 * anche l'unica scelta giusta in assoluto, perché un pixel nero è un pixel **spento**: non
 * emette luce, quindi non c'è niente da attenuare e niente che disturbi chi ti sta accanto.
 * Ogni grigio, per quanto scuro, è luce emessa a vuoto su tutto lo schermo.
 *
 * Il vero sfondo va comunque sostituito e non semplicemente filtrato: una foto di cielo
 * notturno è spesso già rossa o arancione di suo (nebulose a emissione), e filtrarla lascia
 * comunque tutto il suo dettaglio, illeggibile dietro le icone.
 */
val NightNeutralBackground = Color(0xFF000000)

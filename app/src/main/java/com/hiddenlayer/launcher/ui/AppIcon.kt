package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import com.hiddenlayer.launcher.data.AppInfo

/**
 * Quanto sbiadisce l'icona di un'app in grigio durante una sessione.
 *
 * Il solo togliere il colore non basta: su un'icona già bianca, nera o grigia — e ce ne sono
 * parecchie — il filtro di saturazione non cambia un pixel, e non si capiva più quali app
 * fossero bloccate. La trasparenza invece agisce sul rapporto con lo sfondo, non sui colori
 * dell'icona, quindi si vede sempre, qualunque cosa ci sia dentro. I due segnali insieme
 * (scolorita **e** sbiadita) rendono la distinzione leggibile a colpo d'occhio.
 */
private const val MUTED_ALPHA = 0.4f

/**
 * [faded] segue [grayscale] di default, perché nelle griglie i due segnali servono insieme.
 * Si separano solo dove l'icona non è un elemento fra tanti ma il soggetto — la richiesta di
 * conferma — e sbiadirla la renderebbe difficile da riconoscere proprio nel momento in cui
 * devi capire al volo quale app stai per aprire.
 */
@Composable
fun AppIcon(
    app: AppInfo,
    size: Dp,
    modifier: Modifier = Modifier,
    grayscale: Boolean = false,
    faded: Boolean = grayscale
) {
    // app.icon is already a decoded Bitmap (see AppRepository) — this just wraps it,
    // no decoding happens here, so paging/scrolling never pays that cost.
    val bitmap = remember(app.icon) { app.icon.asImageBitmap() }
    // Colour is half of an icon's pull, so a muted app is drained of it rather than hidden.
    val desaturated = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }
    Image(
        bitmap = bitmap,
        contentDescription = app.label,
        colorFilter = if (grayscale) desaturated else null,
        alpha = if (faded) MUTED_ALPHA else 1f,
        modifier = modifier.size(size)
    )
}

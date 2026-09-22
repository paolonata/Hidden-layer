package com.hiddenlayer.launcher.ui.theme

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Le misure che cambiano fra telefono e tablet, e la regola per usarle.
 *
 * **Il problema degli schermi larghi non è che ci sta poco: è che ci sta troppo.** Una riga di
 * testo lunga 1280dp è illeggibile — l'occhio perde il capo della riga successiva — e una
 * griglia di quattro icone su un tablet lascia vuoti in cui ogni icona sembra persa. Le due
 * cose vanno trattate in modo opposto, ed è tutta la logica di questo file:
 *
 * - quello che si **legge** (impostazioni, spiegazioni, elenchi con interruttore) viene
 *   limitato a una larghezza comoda e centrato: [HlContentWidth];
 * - quello che si **scorre con l'occhio** (le griglie di icone) cresce col contenitore,
 *   aggiungendo colonne invece di allargare le celle.
 */

/** La larghezza massima di una colonna di contenuto leggibile. Oltre, una riga di testo
 * diventa un rigo da seguire col dito. Sotto questa soglia — cioè su qualunque telefono —
 * non cambia assolutamente niente rispetto a prima. */
val HlContentWidth = 600.dp

/** I fogli in basso non hanno motivo di essere larghi quanto un tablet: il pulsante finisce
 * lontano dal pollice e il titolo galleggia da solo in cima a una fascia vuota. */
val HlSheetWidth = 480.dp

/** Il dock non si allarga all'infinito. Con cinque posizioni distribuite su 1280dp le icone
 * finiscono a duecento dp l'una dall'altra e smettono di leggersi come una fila. */
val HlDockWidth = 620.dp

/**
 * Un tablet secondo la definizione di Android: il **lato corto** dello schermo da 600dp in su.
 *
 * Si guarda il lato corto di proposito, non la larghezza corrente: così la risposta non cambia
 * ruotando il dispositivo. Un telefono in orizzontale è largo quanto un tablet ma ha 400dp di
 * altezza, e trattarlo da tablet vorrebbe dire icone grandi su tre righe schiacciate. Le
 * colonne, che invece devono aumentare anche lì, si calcolano dalla larghezza vera e non da
 * questa.
 */
@Composable
@ReadOnlyComposable
fun isLargeScreen(): Boolean = LocalConfiguration.current.smallestScreenWidthDp >= 600

/** Limita la larghezza di una colonna di contenuto. Va accompagnato da
 * `Modifier.align(Alignment.CenterHorizontally)` dentro una `Column`, altrimenti il contenuto
 * resta incollato a sinistra invece di stare in mezzo. */
fun Modifier.readableWidth(max: Dp = HlContentWidth): Modifier = this.widthIn(max = max)

/**
 * Quante colonne stanno in [available], dato che una cella non dovrebbe scendere sotto
 * [targetCell].
 *
 * `min` esiste per non **togliere** colonne agli schermi stretti: su un telefono il conto dà
 * tre o quattro, e il risultato deve restare quello che c'era prima del supporto ai tablet.
 * `max` esiste per non trasformare un tablet in un foglio di francobolli.
 */
fun columnsFor(available: Dp, targetCell: Dp, spacing: Dp, min: Int, max: Int): Int {
    if (available <= 0.dp) return min
    val cell = (targetCell + spacing).value
    if (cell <= 0f) return min
    val fits = ((available + spacing).value / cell).toInt()
    return fits.coerceIn(min, max)
}

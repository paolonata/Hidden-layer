package com.hiddenlayer.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * **Il launcher è sempre scuro**, qualunque tema abbia il sistema.
 *
 * Prima seguiva `isSystemInDarkTheme()`. Non è una scelta che abbia senso qui: mettere una
 * home schiarita davanti a chi accende lo schermo al buio è il contrario di quello che serve,
 * e metà delle superfici (il fondo delle app nascoste, la modalità DUMB, il velo dei popup)
 * sono scure per costruzione — con lo schema chiaro attivo il testo di sistema ci finiva
 * sopra illeggibile invece di adattarsi.
 *
 * Lo `ColorScheme` di Material serve ancora ai pochi componenti di sistema rimasti (il cursore
 * della luminosità, gli `AlertDialog`), ma i colori veri stanno in `Tokens.kt` e vengono
 * applicati esplicitamente: è l'unico modo di avere una tavolozza a un accento solo senza
 * combattere con i venti ruoli di Material.
 */
private val DarkColors = darkColorScheme(
    background = HlBackground,
    surface = HlSurface,
    onBackground = HlPaper,
    onSurface = HlPaper,
    primary = HlPaper,
    onPrimary = HlBackground,
    // Material userebbe un rosso saturo per gli errori. Qui non esiste nessun colore saturo:
    // un errore si dice scrivendolo, non accendendolo.
    error = HlPaper,
    onError = HlBackground
)

/**
 * Un carattere solo, pesi leggeri, niente maiuscoletto automatico.
 *
 * I numeri grandi (durate, countdown, percentuali) sono `W200` con crenatura negativa: a
 * quelle dimensioni un peso normale diventa un blocco che urla, mentre il tratto sottile si
 * legge come una cifra su una pagina.
 */
private val HlTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.W200, fontSize = 88.sp, letterSpacing = (-4).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.W200, fontSize = 76.sp, letterSpacing = (-3).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.W200, fontSize = 60.sp, letterSpacing = (-2.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Light, fontSize = 34.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Light, fontSize = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Light, fontSize = 24.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = HlLabelSize,
        letterSpacing = HlLabelTracking
    )
)

@Composable
fun HiddenLayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, typography = HlTypography, content = content)
}

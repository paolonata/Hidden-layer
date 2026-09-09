package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.ui.theme.HlTouchTarget
import kotlinx.coroutines.delay

private const val HOLD_SECONDS = 5

/**
 * The pause between reaching for a muted app and opening it. The point isn't to forbid
 * anything — it's that the reach is automatic and the wait isn't, so by the time the button
 * turns on the impulse has usually passed.
 *
 * Il titolo è la domanda più corta possibile — «Instagram, adesso?» — invece di «Vuoi davvero
 * aprire Instagram?». Non è brevità per brevità: la frase lunga si legge come un avviso di
 * sistema da scavalcare, quella corta come una domanda vera, e la differenza è tutta lì
 * dentro. Il record è sempre detto **in avanti**, quanto manca a batterlo, mai come distanza
 * da colmare: qui serve una spinta, non un rimprovero.
 */
@Composable
fun FrictionPrompt(
    app: AppInfo,
    streakSeconds: Int,
    recordSeconds: Int,
    onOpenAnyway: () -> Unit,
    onDismiss: () -> Unit
) {
    var secondsLeft by remember(app.componentName) { mutableIntStateOf(HOLD_SECONDS) }

    LaunchedEffect(app.componentName) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
    }

    PromptSheet(onDismiss = onDismiss) {
        AppIcon(app = app, size = 52.dp, grayscale = true, faded = false)

        Spacer(Modifier.height(18.dp))

        Text(
            text = "${app.label}, adesso?",
            color = PromptTitle,
            fontSize = 24.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = when {
                recordSeconds <= 0 ->
                    "Sei in concentrazione. Resisti da ${formatFocusDuration(streakSeconds)}."
                streakSeconds >= recordSeconds ->
                    "Resisti da ${formatFocusDuration(streakSeconds)}: è il tuo record."
                else -> "Resisti da ${formatFocusDuration(streakSeconds)}. Ne mancano " +
                    "${formatFocusDuration(recordSeconds - streakSeconds)} per battere il tuo record."
            },
            color = PromptBody,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryPill(
                text = "Resta sul pezzo",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(18.dp))
            // L'uscita non è più un secondo pulsante affiancato al primo: due pulsanti uguali
            // sono due scelte pari, e queste due non lo sono. Il conto alla rovescia resta
            // identico — cinque secondi — ma adesso è un numero in monospazio che scorre di
            // fianco, non un bottone che si accende.
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = HlTouchTarget)
                    .clickable(enabled = secondsLeft == 0, onClick = onOpenAnyway)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (secondsLeft > 0) "apri · $secondsLeft" else "apri comunque",
                    color = if (secondsLeft > 0) PromptDisabledContent else PromptSecondaryContent,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

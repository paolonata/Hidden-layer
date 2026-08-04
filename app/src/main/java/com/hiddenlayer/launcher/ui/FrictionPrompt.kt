package com.hiddenlayer.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiddenlayer.launcher.data.AppInfo
import kotlinx.coroutines.delay

private const val HOLD_SECONDS = 5

/**
 * The pause between reaching for a muted app and opening it. The point isn't to forbid
 * anything — it's that the reach is automatic and the wait isn't, so by the time the button
 * turns on the impulse has usually passed.
 *
 * Rendered as an in-place overlay rather than a system dialog so it can carry the same
 * rounded look as the rest of the launcher; a platform AlertDialog would drop a squared-off
 * Material surface on top of everything and break that. I colori vengono da PromptStyle,
 * condivisi con la scelta della durata.
 */
@Composable
fun FrictionPrompt(
    app: AppInfo,
    streakSeconds: Int,
    recordSeconds: Int,
    onOpenAnyway: () -> Unit,
    onDismiss: () -> Unit
) {
    // È un overlay in composizione, non un Dialog: il tasto indietro va gestito a mano,
    // altrimenti l'unico modo di uscire è toccare fuori dalla carta.
    BackHandler(onBack = onDismiss)

    var secondsLeft by remember(app.componentName) { mutableIntStateOf(HOLD_SECONDS) }

    LaunchedEffect(app.componentName) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PromptScrim)
            // Tapping the backdrop backs out — the easy gesture is the one that keeps you
            // on task, not the one that breaks it.
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(28.dp)
                .clip(PromptCardShape)
                .background(PromptSurface)
                .border(1.dp, PromptBorder, PromptCardShape)
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            AppIcon(app = app, size = 56.dp, grayscale = true)

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Vuoi davvero aprire ${app.label}?",
                color = PromptTitle,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Sei in una sessione di concentrazione.",
                color = PromptCaption,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            // Il record è detto sempre in avanti — quanto manca a batterlo — mai come
            // distanza da colmare: qui serve una spinta, non un rimprovero.
            Text(
                text = when {
                    recordSeconds <= 0 -> "Stai resistendo da ${formatFocusDuration(streakSeconds)}."
                    streakSeconds >= recordSeconds ->
                        "Stai resistendo da ${formatFocusDuration(streakSeconds)}: è il tuo record."
                    else -> "Stai resistendo da ${formatFocusDuration(streakSeconds)} — " +
                        "ne mancano ${formatFocusDuration(recordSeconds - streakSeconds)} " +
                        "per battere il tuo record di ${formatFocusDuration(recordSeconds)}."
                },
                color = PromptBody,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                PromptButton(
                    label = "Resta sul pezzo",
                    prominent = true,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                PromptButton(
                    label = if (secondsLeft > 0) "Apri ($secondsLeft)" else "Apri comunque",
                    prominent = false,
                    enabled = secondsLeft == 0,
                    onClick = onOpenAnyway,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PromptButton(
    label: String,
    prominent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val background = if (prominent) PromptAccent else PromptSecondary
    val content = when {
        !enabled -> PromptDisabledContent
        prominent -> PromptAccentContent
        else -> PromptSecondaryContent
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(46.dp)
            .clip(PromptPillShape)
            .background(background)
            .border(
                1.dp,
                if (prominent) Color.Transparent else PromptSecondaryBorder,
                PromptPillShape
            )
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = label,
            color = content,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

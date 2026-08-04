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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * La scelta della durata, subito dopo il doppio tap sulla home.
 *
 * Tre opzioni e basta: se avviare una sessione richiede di pensare, non la avvii. Le durate
 * fuori da queste tre esistono, ma stanno dove si ragiona sui minuti — la schermata
 * Concentrazione, raggiungibile da qui con "Altra durata…".
 *
 * Stessa carta della richiesta di conferma delle app in grigio (i colori stanno in
 * PromptStyle), non un AlertDialog di sistema, che poserebbe una superficie squadrata sopra
 * tutto il resto.
 */
@Composable
fun FocusDurationPrompt(
    options: List<Int>,
    onPick: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    // Come per la conferma di apertura: overlay in composizione, quindi il tasto indietro
    // non lo chiuderebbe da solo.
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PromptScrim)
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
                .padding(horizontal = 22.dp, vertical = 26.dp)
        ) {
            Text(
                text = "Per quanto?",
                color = PromptTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                options.forEachIndexed { index, minutes ->
                    if (index > 0) Spacer(Modifier.width(10.dp))
                    DurationPill(
                        label = durationLabel(minutes),
                        onClick = { onPick(minutes) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Altra durata…",
                color = PromptCaption,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(PromptPillShape)
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun DurationPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(52.dp)
            .clip(PromptPillShape)
            .background(PromptAccent)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = PromptAccentContent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/** "30 min", "1 ora", "2 ore" — come le diresti, non come sono salvate. */
fun durationLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes == 60 -> "1 ora"
    minutes % 60 == 0 -> "${minutes / 60} ore"
    else -> "${minutes / 60}h ${minutes % 60}"
}

package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * La scelta della durata, subito dopo il doppio tap sulla home.
 *
 * Tre opzioni e basta: se avviare una sessione richiede di pensare, non la avvii. Le durate
 * fuori da queste tre esistono, ma stanno dove si ragiona sui minuti — la schermata
 * Concentrazione, raggiungibile da qui con "Altra durata…".
 *
 * Stesso foglio in basso degli altri due popup (`PromptSheet`), non un `AlertDialog` di
 * sistema, che poserebbe una superficie squadrata sopra tutto il resto.
 */
@Composable
fun FocusDurationPrompt(
    options: List<Int>,
    onPick: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    PromptSheet(onDismiss = onDismiss) {
        Text(
            text = "Per quanto?",
            color = PromptTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { minutes ->
                PrimaryPill(
                    text = durationLabel(minutes),
                    onClick = { onPick(minutes) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextAction(
                text = "Altra durata…",
                onClick = onOpenSettings,
                color = PromptCaption,
                fontSize = 14.sp
            )
        }
    }
}

/** "30 min", "1 ora", "2 ore" — come le diresti, non come sono salvate. */
fun durationLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes == 60 -> "1 ora"
    minutes % 60 == 0 -> "${minutes / 60} ore"
    else -> "${minutes / 60}h ${minutes % 60}"
}

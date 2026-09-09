package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * La conferma per uscire da DUMB prima della scadenza.
 *
 * L'uscita anticipata c'è di proposito: una modalità da cui non si esce viene disinstallata al
 * primo imprevisto, ed è la stessa ragione per cui la home chiusa durante la Concentrazione ha
 * sempre un "Mi serve il telefono". Ma costa una conferma e **viene contata per sempre**.
 *
 * Le due azioni non sono pari, e si vede: quella che la mano preme per riflesso — la pill
 * piena — è "Resto in DUMB"; uscire è testo. È l'unico punto di questo foglio in cui la
 * gerarchia visiva sta facendo un lavoro.
 */
@Composable
fun DumbExitPrompt(
    earlyExits: Int,
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    PromptSheet(onDismiss = onDismiss) {
        Text(
            text = "Uscire dalla modalità DUMB?",
            color = PromptTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = if (earlyExits > 0) {
                "Sarebbe la ${earlyExits + 1}ª volta che la interrompi prima della fine."
            } else {
                "La sessione non è finita. L'uscita viene contata."
            },
            color = PromptBody,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryPill(
                text = "Resto in DUMB",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(18.dp))
            TextAction(
                text = "Esci",
                onClick = onConfirmExit,
                color = PromptSecondaryContent,
                fontSize = 14.sp
            )
        }
    }
}

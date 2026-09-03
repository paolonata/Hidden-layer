package com.hiddenlayer.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
 * La conferma per uscire da DUMB prima della scadenza.
 *
 * L'uscita anticipata c'è di proposito: una modalità da cui non si esce viene disinstallata al
 * primo imprevisto, ed è la stessa ragione per cui la home chiusa durante la Concentrazione ha
 * sempre un "Mi serve il telefono". Ma costa una conferma e **viene contata per sempre**.
 *
 * I due pulsanti sono invertiti rispetto all'abitudine: quello pieno, quello che la mano preme
 * per riflesso, è "Resto in DUMB". Uscire è il testo secondario. È l'unico punto di questa
 * schermata in cui la gerarchia visiva sta facendo un lavoro.
 */
@Composable
fun DumbExitPrompt(
    earlyExits: Int,
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
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
                text = "Uscire dalla modalità DUMB?",
                color = PromptTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (earlyExits > 0) {
                    "Sarebbe la ${earlyExits + 1}ª volta che la interrompi prima della fine."
                } else {
                    "La sessione non è finita. L'uscita viene contata."
                },
                color = PromptBody,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(22.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(PromptPillShape)
                    .background(PromptAccent)
                    .clickable(onClick = onDismiss)
            ) {
                Text(
                    text = "Resto in DUMB",
                    color = PromptAccentContent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Esci comunque",
                color = PromptCaption,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(PromptPillShape)
                    .clickable(onClick = onConfirmExit)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

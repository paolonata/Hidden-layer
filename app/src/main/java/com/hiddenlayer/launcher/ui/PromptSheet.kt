package com.hiddenlayer.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Il foglio comune ai tre popup: scelta della durata, conferma di apertura, uscita da DUMB.
 *
 * **Sta in basso, non al centro.** Una carta centrata è la forma di una finestra di sistema:
 * interrompe, si pianta in mezzo a quello che stavi guardando, e mette i pulsanti a metà
 * schermo — cioè lontano dal pollice, sui telefoni di oggi. Un foglio ancorato al bordo
 * inferiore si legge nella stessa direzione del resto dell'interfaccia e ha le azioni dove la
 * mano è già.
 *
 * Il contenuto è **allineato a sinistra**, non centrato: il testo centrato costringe l'occhio
 * a ritrovare l'inizio di ogni riga, ed è una scelta che ha senso per un titolo di due parole,
 * non per una frase.
 *
 * Il tocco sul velo chiude: il gesto facile è quello che ti lascia dov'eri, non quello che ti
 * porta via. È un overlay in composizione e non un `Dialog`, quindi il tasto indietro va
 * gestito a mano — senza, l'unica uscita sarebbe il velo.
 */
@Composable
fun PromptSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
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
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(18.dp)
                .fillMaxWidth()
                .clip(PromptCardShape)
                .background(PromptSurface)
                // Il tocco sul foglio non deve arrivare al velo sotto, o toccare il titolo
                // chiuderebbe il popup.
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                )
                .padding(horizontal = 26.dp, vertical = 28.dp),
            content = content
        )
    }
}

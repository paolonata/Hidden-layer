package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiddenlayer.launcher.ui.theme.HlPaper
import com.hiddenlayer.launcher.ui.theme.HlTouchTarget
import kotlinx.coroutines.flow.StateFlow

/**
 * Il segnale che una sessione di concentrazione è in corso, letto dalla home.
 *
 * Le icone grigie dicono che qualcosa è cambiato ma non quanto manca, e una sessione di cui
 * ti dimentichi non ti tiene: qui il tempo resta sotto gli occhi ogni volta che sblocchi il
 * telefono, che è metà del lavoro.
 *
 * **Non è più una pill**: era una capsula smerigliata col pallino acceso, cioè un badge
 * luminoso sulla home di un launcher fatto per non farti guardare la home. Ora è una riga di
 * testo in monospazio — `24:58 · CONCENTRAZIONE` — che si legge se la cerchi e sparisce
 * dall'attenzione se non la cerchi. Il monospazio serve a una cosa precisa: le cifre non
 * cambiano larghezza mentre scorrono, quindi la riga non balla ogni secondo.
 *
 * Toccandola si apre Concentrazione, non si termina la sessione: fermarsi deve costare un
 * tocco in più che iniziare, altrimenti la scorciatoia finisce per lavorare contro di te.
 */
@Composable
fun FocusPill(remaining: StateFlow<Int>, onClick: () -> Unit) {
    // Il countdown viene raccolto qui dentro e non passato dall'alto: così il tick di ogni
    // secondo invalida solo questa riga, non la schermata che la contiene.
    val remainingSeconds by remaining.collectAsState()

    Box(
        modifier = Modifier
            // Il testo si è alleggerito, l'area di tocco no.
            .defaultMinSize(minHeight = HlTouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${formatFocusRemaining(remainingSeconds)} · CONCENTRAZIONE",
            color = HlPaper.copy(alpha = 0.45f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )
    }
}

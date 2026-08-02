package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow

private val PILL_SHAPE = RoundedCornerShape(percent = 50)
private val PILL_HEIGHT = 30.dp

/**
 * Il segnale che una sessione di concentrazione è in corso, letto dalla home.
 *
 * Le icone grigie dicono che qualcosa è cambiato ma non quanto manca, e una sessione di cui
 * ti dimentichi non ti tiene: qui il tempo resta sotto gli occhi ogni volta che sblocchi il
 * telefono, che è metà del lavoro.
 *
 * Toccandola si apre Concentrazione, non si termina la sessione: fermarsi deve costare un
 * tocco in più che iniziare, altrimenti la scorciatoia finisce per lavorare contro di te.
 * Stessa pill smerigliata del campo di ricerca del cassetto, in piccolo.
 */
@Composable
fun FocusPill(remaining: StateFlow<Int>, onClick: () -> Unit) {
    // Il countdown viene raccolto qui dentro e non passato dall'alto: così il tick di ogni
    // secondo invalida solo questa pill, non la schermata che la contiene.
    val remainingSeconds by remaining.collectAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(PILL_HEIGHT)
            .clip(PILL_SHAPE)
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), PILL_SHAPE)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp)
    ) {
        // Un punto pieno invece di un'icona a orologio: costa niente da disegnare e si legge
        // come "in corso" senza aggiungere una seconda forma alla pill.
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatFocusRemaining(remainingSeconds),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp
        )
    }
}

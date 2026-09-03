package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.AppInfo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Il nero non è pieno: un grigio molto scuro si legge come una superficie, il nero assoluto
 * come uno schermo spento — e su OLED fa anche da specchio. */
private val DumbBackground = Color(0xFF0B0B0C)

/** Tre grigi, e basta. Niente accenti, niente bianco pieno: qui non deve esserci niente che
 * attiri l'occhio, perché lo scopo dichiarato della modalità è togliere la voglia di stare
 * al telefono. */
private val DumbPrimary = Color(0xFFB6B6B8)
private val DumbSecondary = Color(0xFF6E6E72)
private val DumbFaint = Color(0xFF3A3A3D)

/**
 * La modalità DUMB: per il tempo che hai scelto il launcher **non è più un launcher**.
 *
 * Non è il cassetto con meno app, non è la home con delle icone in grigio: è questa schermata
 * e nient'altro. Quando è attiva `LauncherApp` non compone niente del resto — niente home,
 * niente dock, niente cassetto, niente sfondo, e nemmeno i rilevatori di gesto che li
 * aprirebbero. Non c'è un gesto segreto da ricordarsi di non fare: non esiste proprio più
 * niente da toccare.
 *
 * Per lo stesso motivo qui non ci sono **icone**. Un'icona è un logo, cioè esattamente la cosa
 * progettata per farsi notare da mezzo metro; cinque nomi scritti in grigio si leggono solo se
 * li stai cercando. La lista è corta, ferma e nello stesso ordine: se il telefono è noioso da
 * guardare, hai molte meno ragioni per guardarlo.
 */
@Composable
fun DumbScreen(
    state: LauncherUiState,
    onAppTap: (AppInfo) -> Unit,
    onRequestExit: () -> Unit
) {
    // Il tasto indietro non porta da nessuna parte: è la scorciatoia che, in tutte le altre
    // schermate, chiude quello che hai aperto. Qui non c'è niente sotto da riaprire.
    BackHandler {}

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            // Al minuto, non al secondo: i secondi che scorrono sono qualcosa da guardare.
            delay(20_000L)
        }
    }

    val clockFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val apps = state.dumbApps

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DumbBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(72.dp))

            Text(
                text = clockFormat.format(Date(nowMillis)),
                color = DumbSecondary,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.height(8.dp))
            Text(
                // Un orario di fine, non un conto alla rovescia: guardare i minuti scendere è
                // già un modo di stare al telefono, e sapere che mancano "47 minuti" invita a
                // ricontrollare. Un'ora fissa la si legge una volta e si sa.
                text = "fino alle ${clockFormat.format(Date(state.dumbEndsAt))}",
                color = DumbFaint,
                fontSize = 15.sp
            )

            Spacer(Modifier.height(56.dp))

            if (apps.isEmpty()) {
                Text(
                    text = "Nessuna app disponibile.\nEsci e scegline qualcuna.",
                    color = DumbSecondary,
                    fontSize = 18.sp
                )
            } else {
                apps.forEach { app ->
                    Text(
                        text = app.label,
                        color = DumbPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppTap(app) }
                            .padding(vertical = 16.dp)
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            if (state.dumbEarlyExits > 0) {
                Text(
                    // Non un rimprovero, un dato: la ripetizione è il sintomo, e vederla
                    // scritta costa più di un'attesa. Stessa idea dello storico della
                    // Concentrazione.
                    text = "Hai interrotto ${state.dumbEarlyExits} volte.",
                    color = DumbFaint,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
            }
            Text(
                // In grigio scurissimo e senza contorni: la via d'uscita c'è — una modalità da
                // cui non si esce viene disinstallata al primo imprevisto — ma non deve essere
                // la prima cosa che vedi.
                text = "Esci",
                color = DumbFaint,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable(onClick = onRequestExit)
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            )
        }
    }
}

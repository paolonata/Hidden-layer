package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.DumbRepository
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.DragHandle
import com.hiddenlayer.launcher.ui.Hairline
import com.hiddenlayer.launcher.ui.PrimaryPill
import com.hiddenlayer.launcher.ui.ScreenHeader
import com.hiddenlayer.launcher.ui.SectionLabel
import com.hiddenlayer.launcher.ui.TextAction
import com.hiddenlayer.launcher.ui.closeOnDragDown
import com.hiddenlayer.launcher.ui.durationLabel
import com.hiddenlayer.launcher.ui.theme.HlPaper
import com.hiddenlayer.launcher.ui.theme.HlPaper42
import com.hiddenlayer.launcher.ui.theme.HlPaper55
import com.hiddenlayer.launcher.ui.theme.HlScreenMargin

/**
 * Dove si prepara la modalità DUMB: le tre posizioni libere e la durata. Poi si entra, e da
 * lì non c'è più niente da regolare — è il punto della modalità.
 *
 * Segue lo stesso linguaggio della Concentrazione, che è la funzione a cui somiglia: durata
 * come numeri in fila con la selezione sottolineata, righe senza icone, un solo pulsante
 * pieno. Le due schermate si aprono dallo stesso menu e fanno cose vicine — se avessero due
 * grafiche diverse sembrerebbero due app.
 */
@Composable
fun DumbSettingsScreen(
    state: LauncherUiState,
    onSetDuration: (Int) -> Unit,
    onPickSlot: (Int) -> Unit,
    onClearSlot: (Int) -> Unit,
    onStart: () -> Unit,
    onDone: () -> Unit
) {
    BackHandler(onBack = onDone)
    val listState = rememberLazyListState()
    val fixed = state.dumbFixed
    val byComponent = state.allApps.associateBy { it.componentName }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .closeOnDragDown(
                canClose = {
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                },
                onClose = onDone
            )
    ) {
        BlurredWallpaperBackground(scrimAlpha = 0.88f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            DragHandle(onClose = onDone)
            Column(modifier = Modifier.padding(horizontal = HlScreenMargin)) {
                ScreenHeader(label = "Modalità DUMB", actionText = "Chiudi", onAction = onDone)
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = "Per il tempo che scegli il telefono diventa un elenco di cinque " +
                            "nomi su fondo nero. Niente icone, niente cassetto, niente ricerca: " +
                            "non c'è un gesto da evitare, non c'è proprio più niente da toccare.",
                        color = HlPaper55,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = HlScreenMargin, vertical = 16.dp)
                    )
                    Hairline(modifier = Modifier.padding(horizontal = HlScreenMargin))
                }

                item { Label("Sempre disponibili") }

                if (fixed.isEmpty()) {
                    item {
                        Text(
                            text = "Il sistema non riporta un telefono o un'app di messaggi " +
                                "predefiniti con un'icona nel launcher: quelle due posizioni " +
                                "resteranno vuote.",
                            color = HlPaper.copy(alpha = 0.45f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = HlScreenMargin)
                        )
                    }
                } else {
                    items(fixed.size) { index ->
                        val app = byComponent[fixed[index]]
                        SlotRow(
                            title = app?.label ?: "—",
                            // Non sono modificabili, e va detto perché: sono lette dal sistema,
                            // quindi seguono l'app che usi davvero.
                            subtitle = "Predefinita di sistema, non modificabile",
                            dimmed = false
                        )
                    }
                }

                item {
                    Label("Le tue tre")
                }

                items(DumbRepository.CHOSEN_SLOTS) { slot ->
                    val app = state.dumbChosen.getOrNull(slot)?.let { byComponent[it] }
                    SlotRow(
                        title = app?.label ?: "Posizione libera",
                        subtitle = if (app != null) "Tocca per sostituirla" else "Tocca per sceglierne una",
                        dimmed = app == null,
                        onClick = { onPickSlot(slot) },
                        action = if (app != null) {
                            { TextAction("Togli", { onClearSlot(slot) }, color = HlPaper42) }
                        } else {
                            null
                        }
                    )
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = HlScreenMargin)) {
                        Spacer(Modifier.height(24.dp))
                        SectionLabel("Durata")
                        Spacer(Modifier.height(12.dp))
                        // Come nella Concentrazione: numeri in fila, il selezionato
                        // sottolineato. Nessuna pill, nessun riquadro.
                        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            DumbRepository.PRESET_MINUTES.forEach { minutes ->
                                val selected = minutes == state.dumbDurationMinutes
                                Text(
                                    text = durationLabel(minutes),
                                    color = if (selected) HlPaper else HlPaper42,
                                    fontSize = 15.sp,
                                    textDecoration = if (selected) TextDecoration.Underline else null,
                                    modifier = Modifier
                                        .clickable { onSetDuration(minutes) }
                                        .padding(vertical = 14.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        PrimaryPill(
                            text = "Entra in DUMB per ${durationLabel(state.dumbDurationMinutes)}",
                            onClick = onStart,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.dumbEarlyExits > 0) {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Finora l'hai interrotta ${state.dumbEarlyExits} volte " +
                                    "prima della scadenza.",
                                color = HlPaper.copy(alpha = 0.45f),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                    Hairline(modifier = Modifier.padding(horizontal = HlScreenMargin))
                }

                item {
                    Column(modifier = Modifier.padding(HlScreenMargin)) {
                        SectionLabel("Scala di grigi su tutto il telefono", color = HlPaper)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (state.dumbGrayscaleAvailable) {
                                "Attiva. Entrando in DUMB tutto il telefono passa in bianco e " +
                                    "nero — anche dentro le app — e torna a colori all'uscita."
                            } else {
                                "Non attiva: senza, il grigio resta dentro il launcher e le app " +
                                    "che apri restano a colori.\n\n" +
                                    "Per estenderlo a tutto il telefono serve un permesso di " +
                                    "sistema che Android non concede a un'app normale. Si dà " +
                                    "una volta sola, da computer con il telefono collegato:\n\n" +
                                    "adb shell pm grant com.hiddenlayer.launcher " +
                                    "android.permission.WRITE_SECURE_SETTINGS\n\n" +
                                    "Se preferisci non farlo, la modalità funziona lo stesso."
                            },
                            color = HlPaper.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            lineHeight = 20.sp
                        )
                    }
                    Hairline(modifier = Modifier.padding(horizontal = HlScreenMargin))
                }

                item {
                    Text(
                        text = "Puoi uscire prima della scadenza: la via d'uscita c'è sempre, " +
                            "perché una modalità da cui non si esce la disinstalli al primo " +
                            "imprevisto. Ma costa una conferma e viene contata.\n\n" +
                            "Quello che DUMB non può fare: le notifiche continuano ad arrivare " +
                            "e toccarle apre l'app, perché nessun launcher senza root può " +
                            "intercettare quel tocco. Restano raggiungibili anche le app " +
                            "recenti e la ricerca di sistema. DUMB toglie la strada normale — " +
                            "quella che percorri senza accorgertene — non tutte le strade.",
                        color = HlPaper.copy(alpha = 0.40f),
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(HlScreenMargin)
                    )
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    SectionLabel(
        text = text,
        modifier = Modifier.padding(
            start = HlScreenMargin,
            end = HlScreenMargin,
            top = 24.dp,
            bottom = 8.dp
        )
    )
}

/** Una posizione della modalità: nome, una riga di spiegazione, ed eventualmente un'azione.
 * Senza icona — in DUMB le icone non ci sono, e mostrarle qui prometterebbe una schermata
 * diversa da quella in cui si entra. */
@Composable
private fun SlotRow(
    title: String,
    subtitle: String,
    dimmed: Boolean,
    onClick: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = HlScreenMargin, end = HlScreenMargin, top = 12.dp, bottom = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (dimmed) HlPaper.copy(alpha = 0.50f) else HlPaper,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, color = HlPaper.copy(alpha = 0.45f), fontSize = 12.sp)
        }
        action?.invoke()
    }
}

package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.DumbRepository
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.DragHandle
import com.hiddenlayer.launcher.ui.closeOnDragDown
import com.hiddenlayer.launcher.ui.durationLabel

/**
 * Dove si prepara la modalità DUMB: le tre posizioni libere e la durata. Poi si entra, e da
 * lì non c'è più niente da regolare — è il punto della modalità.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        BlurredWallpaperBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    DragHandle(onClose = onDone)
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White
                        ),
                        title = { Text("Modalità DUMB") },
                        navigationIcon = {
                            TextButton(onClick = onDone) { Text("Chiudi", color = Color.White) }
                        }
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                item {
                    Text(
                        text = "Per il tempo che scegli il telefono diventa un elenco di cinque " +
                            "nomi su fondo nero. Niente icone, niente cassetto, niente ricerca: " +
                            "non c'è un gesto da evitare, non c'è proprio più niente da toccare.",
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(16.dp)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                }

                item {
                    SectionLabel("Sempre disponibili")
                }
                if (fixed.isEmpty()) {
                    item {
                        Text(
                            text = "Il sistema non riporta un telefono o un'app di messaggi " +
                                "predefiniti con un'icona nel launcher: quelle due posizioni " +
                                "resteranno vuote.",
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(fixed.size) { index ->
                        val app = byComponent[fixed[index]]
                        ListItem(
                            headlineContent = {
                                Text(app?.label ?: "—", color = Color.White)
                            },
                            supportingContent = {
                                Text(
                                    // Non sono modificabili, e va detto perché: sono lette dal
                                    // sistema, quindi seguono l'app che usi davvero.
                                    "Telefono e messaggi predefiniti di sistema. Non modificabili.",
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }

                item {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    SectionLabel("Le tue tre")
                }

                items(DumbRepository.CHOSEN_SLOTS) { slot ->
                    val app = state.dumbChosen.getOrNull(slot)?.let { byComponent[it] }
                    ListItem(
                        headlineContent = {
                            Text(
                                text = app?.label ?: "Posizione libera",
                                color = if (app != null) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                        },
                        supportingContent = {
                            Text(
                                text = if (app != null) "Tocca per sostituirla" else "Tocca per sceglierne una",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        trailingContent = {
                            if (app != null) {
                                TextButton(onClick = { onClearSlot(slot) }) {
                                    Text("Togli", color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                        },
                        modifier = Modifier.clickable { onPickSlot(slot) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }

                item {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    SectionLabel("Durata — ${durationLabel(state.dumbDurationMinutes)}")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DumbRepository.PRESET_MINUTES.forEach { minutes ->
                            val selected = minutes == state.dumbDurationMinutes
                            Button(
                                onClick = { onSetDuration(minutes) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) {
                                        Color.White.copy(alpha = 0.9f)
                                    } else {
                                        Color.White.copy(alpha = 0.15f)
                                    },
                                    contentColor = if (selected) Color(0xFF17181B) else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(durationLabel(minutes), maxLines = 1)
                            }
                        }
                    }
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = onStart,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.92f),
                                contentColor = Color(0xFF17181B)
                            )
                        ) {
                            Text("Entra in DUMB per ${durationLabel(state.dumbDurationMinutes)}")
                        }
                        if (state.dumbEarlyExits > 0) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Finora l'hai interrotta ${state.dumbEarlyExits} volte prima della scadenza.",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                }

                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Scala di grigi su tutto il telefono",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
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
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
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
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.55f),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

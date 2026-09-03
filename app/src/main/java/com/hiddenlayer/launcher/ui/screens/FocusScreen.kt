package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.data.FocusRepository
import com.hiddenlayer.launcher.ui.AppIcon
import com.hiddenlayer.launcher.ui.AppSearchField
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.DragHandle
import com.hiddenlayer.launcher.ui.closeOnDragDown
import com.hiddenlayer.launcher.ui.formatFocusDuration
import com.hiddenlayer.launcher.ui.formatTimeAgo
import com.hiddenlayer.launcher.ui.formatFocusRemaining
import kotlinx.coroutines.flow.StateFlow

private val PILL_SHAPE = RoundedCornerShape(percent = 50)

/**
 * Focus sessions. A launcher can't actually stop an app from opening, so this works on the
 * thing that matters instead: reaching for a distracting app is a reflex, and both levers
 * here interrupt the reflex rather than trying to block it — the colour drains out of the
 * icon, and opening it takes a deliberate wait.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    state: LauncherUiState,
    focusRemaining: StateFlow<Int>,
    onSetDuration: (Int) -> Unit,
    onToggleApp: (AppInfo) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onResetStats: () -> Unit,
    onDone: () -> Unit
) {
    BackHandler(onBack = onDone)
    val listState = rememberLazyListState()
    var query by remember { mutableStateOf("") }

    // Deliberately not re-sorted with the selected ones on top: the list would reshuffle
    // under your finger every time you flick a switch.
    //
    // Le nascoste vanno escluse: questa schermata si raggiunge dalla home in due gesti e non
    // è protetta dallo sblocco, quindi elencarle qui per nome — con tanto di ricerca — le
    // rivelerebbe a chiunque prenda in mano il telefono.
    val apps = remember(state.allApps, state.hiddenPackages, query) {
        state.allApps
            .filter { it.packageName !in state.hiddenPackages }
            .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
    }

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
        BlurredWallpaperBackground(scrimAlpha = 0.45f)

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // La barra di stato la paga la Column, non la TopAppBar: sopra di essa
                // c'e' la maniglia, che altrimenti finirebbe sotto l'orologio di sistema.
                // Per questo gli inset della TopAppBar sono azzerati — sommati a questi
                // lascerebbero un buco alto quanto la barra.
                Column(modifier = Modifier.statusBarsPadding()) {
                    DragHandle(onClose = onDone)
                    TopAppBar(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White
                        ),
                        title = { Text("Concentrazione") },
                        navigationIcon = {
                            TextButton(onClick = onDone) { Text("Chiudi", color = Color.White) }
                        }
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                item {
                    if (state.focusActive) {
                        RunningSession(remaining = focusRemaining, onStop = onStop)
                    } else {
                        DurationPicker(
                            minutes = state.focusDurationMinutes,
                            onSetDuration = onSetDuration,
                            onStart = onStart
                        )
                    }
                }

                item {
                    FocusHistory(
                        state = state,
                        onResetStats = onResetStats
                    )
                }

                item {
                    Text(
                        "Le app che selezioni qui sotto, durante una sessione, perdono il colore e chiedono conferma prima di aprirsi. Restano comunque raggiungibili: l'obiettivo è farti fermare un attimo, non impedirtelo.",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }

                item {
                    // The choice is saved as you make it and reused by every future session,
                    // so this is a one-off setup rather than something to redo each time.
                    Text(
                        text = when (val selected = state.focusPackages.size) {
                            0 -> "Nessuna app selezionata"
                            1 -> "1 app selezionata · scelta permanente"
                            else -> "$selected app selezionate · scelta permanente"
                        },
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                item {
                    AppSearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Cerca app",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                items(apps, key = { it.componentName.flattenToString() }) { app ->
                    val muted = app.packageName in state.focusPackages
                    ListItem(
                        headlineContent = { Text(app.label, color = Color.White) },
                        leadingContent = { AppIcon(app = app, size = 36.dp, grayscale = muted) },
                        trailingContent = {
                            Switch(checked = muted, onCheckedChange = { onToggleApp(app) })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                }
            }
        }
    }
}

@Composable
private fun DurationPicker(minutes: Int, onSetDuration: (Int) -> Unit, onStart: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(20.dp)
    ) {
        Text(
            text = "$minutes min",
            color = Color.White,
            fontSize = 44.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(Modifier.height(16.dp))

        // One tap for the usual lengths, plus a stepper for anything in between — deciding
        // "how long" should never be the reason a session doesn't get started.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FocusRepository.PRESET_MINUTES.forEach { preset ->
                DurationChip(
                    label = "$preset",
                    selected = preset == minutes,
                    onClick = { onSetDuration(preset) }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton(Icons.Default.Remove, "Meno tempo") {
                onSetDuration(minutes - FocusRepository.STEP_MINUTES)
            }
            Spacer(Modifier.width(20.dp))
            Text(
                "regola di ${FocusRepository.STEP_MINUTES} min",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp
            )
            Spacer(Modifier.width(20.dp))
            StepperButton(Icons.Default.Add, "Più tempo") {
                onSetDuration(minutes + FocusRepository.STEP_MINUTES)
            }
        }

        Spacer(Modifier.height(22.dp))

        PrimaryPill(label = "Inizia", onClick = onStart)
    }
}

@Composable
private fun RunningSession(remaining: StateFlow<Int>, onStop: () -> Unit) {
    // Raccolto qui, non nel corpo di FocusScreen: il tick al secondo non deve ricomporre
    // l'elenco delle app con i loro interruttori.
    val remainingSeconds by remaining.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(20.dp)
    ) {
        Text(
            text = "Sessione in corso",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 14.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatFocusRemaining(remainingSeconds),
            color = Color.White,
            fontSize = 52.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(Modifier.height(20.dp))
        PrimaryPill(label = "Termina ora", onClick = onStop, prominent = false)
    }
}

/**
 * Lo storico di sempre: il record di resistenza e la classifica delle app che apri comunque.
 *
 * Non c'è un elenco delle singole sessioni di proposito — quello che serve non è cos'è
 * successo martedì, ma quali app cedono sistematicamente, e quello si vede solo sommando.
 *
 * Le app nascoste sono già escluse in scrittura; qui c'è comunque il filtro, perché un'app
 * può essere nascosta dopo aver accumulato aperture e il suo nome finirebbe in chiaro in una
 * schermata non protetta.
 */
@Composable
private fun FocusHistory(state: LauncherUiState, onResetStats: () -> Unit) {
    val now = remember(state.focusBreaks) { System.currentTimeMillis() }
    val byPackage = remember(state.allApps) { state.allApps.associateBy { it.packageName } }
    val breaks = remember(state.focusBreaks, state.hiddenPackages) {
        state.focusBreaks.filter { it.packageName !in state.hiddenPackages }
    }
    val totalBreaks = remember(breaks) { breaks.sumOf { it.count } }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Record di resistenza",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (state.focusRecordSeconds > 0) {
                formatFocusDuration(state.focusRecordSeconds)
            } else {
                "nessuno ancora"
            },
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "il tratto più lungo sotto blocco senza aprire niente",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )

        if (state.focusSessionCount > 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = buildString {
                    append(if (state.focusSessionCount == 1) "1 sessione" else "${state.focusSessionCount} sessioni")
                    append(" · ")
                    append(if (totalBreaks == 1) "1 apertura forzata" else "$totalBreaks aperture forzate")
                    if (totalBreaks > 0) {
                        append(" · ")
                        append("%.1f a sessione".format(totalBreaks.toFloat() / state.focusSessionCount))
                    }
                },
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "Aperte comunque, nonostante il blocco",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))

        if (breaks.isEmpty()) {
            Text(
                text = "Ancora nessuna. Quando aprirai un'app durante una sessione la troverai qui.",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp
            )
        } else {
            breaks.forEach { entry ->
                val app = byPackage[entry.packageName]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    if (app != null) {
                        AppIcon(app = app, size = 30.dp)
                    } else {
                        Spacer(Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app?.label ?: entry.packageName,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "ultima volta ${formatTimeAgo(entry.lastAtMillis, now)}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "${entry.count}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

        }

        // Fuori dall'elenco: serve anche quando di cedimenti non ce ne sono, per esempio per
        // cancellare un record falsato da un cambio di ora di sistema, che altrimenti
        // resterebbe imbattibile per sempre.
        if (state.focusSessionCount > 0 || state.focusRecordSeconds > 0 || breaks.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Azzera statistiche",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(PILL_SHAPE)
                    .clickable(onClick = onResetStats)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
    }
}

@Composable
private fun DurationChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(40.dp)
            .width(52.dp)
            .clip(PILL_SHAPE)
            .background(if (selected) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = if (selected) 0f else 0.22f), PILL_SHAPE)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF17181B) else Color.White.copy(alpha = 0.85f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(PILL_SHAPE)
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), PILL_SHAPE)
            .clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = description, tint = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun PrimaryPill(label: String, onClick: () -> Unit, prominent: Boolean = true) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(PILL_SHAPE)
            .background(if (prominent) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = if (prominent) 0f else 0.22f), PILL_SHAPE)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (prominent) Color(0xFF17181B) else Color.White.copy(alpha = 0.9f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

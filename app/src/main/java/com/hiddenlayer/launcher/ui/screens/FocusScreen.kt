package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.data.FocusRepository
import com.hiddenlayer.launcher.ui.AppSearchField
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.DragHandle
import com.hiddenlayer.launcher.ui.Hairline
import com.hiddenlayer.launcher.ui.MinimalSwitch
import com.hiddenlayer.launcher.ui.MonoValue
import com.hiddenlayer.launcher.ui.PrimaryPill
import com.hiddenlayer.launcher.ui.ScreenHeader
import com.hiddenlayer.launcher.ui.SectionLabel
import com.hiddenlayer.launcher.ui.TextAction
import com.hiddenlayer.launcher.ui.closeOnDragDown
import com.hiddenlayer.launcher.ui.formatFocusDuration
import com.hiddenlayer.launcher.ui.formatFocusRemaining
import com.hiddenlayer.launcher.ui.formatTimeAgo
import com.hiddenlayer.launcher.ui.theme.HlPaper
import com.hiddenlayer.launcher.ui.theme.HlPaper42
import com.hiddenlayer.launcher.ui.theme.HlPaper55
import com.hiddenlayer.launcher.ui.theme.HlScreenMargin
import com.hiddenlayer.launcher.ui.theme.HlWideMargin
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Il fondo della sessione in corso: piatto e ancora più cupo del resto, perché lì non c'è
 * niente da guardare. */
private val RunningBackground = Color(0xFF100F0D)

/**
 * Focus sessions. A launcher can't actually stop an app from opening, so this works on the
 * thing that matters instead: reaching for a distracting app is a reflex, and both levers
 * here interrupt the reflex rather than trying to block it — the colour drains out of the
 * icon, and opening it takes a deliberate wait.
 *
 * Il setup e la sessione in corso hanno **due impaginazioni diverse di proposito**. Il setup
 * è una schermata di scelte, quindi c'è un pulsante pieno e una lista. La sessione in corso
 * non ha niente da scegliere: è un foglio con un numero grande, tre righe di dati e una via
 * d'uscita scritta piccola in fondo — nulla che inviti a restarci.
 */
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
        // Il fondo va disegnato **qui**, sulla radice, e non dentro RunningSession: quella sta
        // dentro la Column che si paga gli inset, quindi un fondo suo si fermerebbe prima
        // delle barre di sistema e lascerebbe due strisce di sfondo sfocato in cima e in
        // fondo a una schermata che dev'essere piatta.
        if (state.focusActive) {
            Box(modifier = Modifier.fillMaxSize().background(RunningBackground))
        } else {
            BlurredWallpaperBackground(scrimAlpha = 0.82f)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            DragHandle(onClose = onDone)

            // Sessione in corso: niente elenco, niente storico, niente da regolare. Guardare
            // le impostazioni di una sessione mentre è in corso è già un modo di starci sopra.
            if (state.focusActive) {
                RunningSession(
                    state = state,
                    remaining = focusRemaining,
                    onStop = onStop,
                    onClose = onDone
                )
                return@Column
            }

            Column(modifier = Modifier.padding(horizontal = HlScreenMargin)) {
                ScreenHeader(label = "Concentrazione", actionText = "Chiudi", onAction = onDone)
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item {
                    DurationPicker(
                        minutes = state.focusDurationMinutes,
                        onSetDuration = onSetDuration,
                        onStart = onStart
                    )
                }

                item { FocusHistory(state = state, onResetStats = onResetStats) }

                item {
                    Text(
                        text = "Le app che selezioni qui sotto, durante una sessione, perdono " +
                            "il colore e chiedono conferma prima di aprirsi. Restano comunque " +
                            "raggiungibili: l'obiettivo è farti fermare un attimo, non " +
                            "impedirtelo.",
                        modifier = Modifier.padding(horizontal = HlScreenMargin, vertical = 12.dp),
                        color = HlPaper55,
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
                        modifier = Modifier.padding(horizontal = HlScreenMargin),
                        color = HlPaper,
                        fontSize = 14.sp
                    )
                }

                item {
                    AppSearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Cerca app",
                        modifier = Modifier.padding(horizontal = HlScreenMargin, vertical = 10.dp)
                    )
                }

                items(apps, key = { it.componentName.flattenToString() }) { app ->
                    val muted = app.packageName in state.focusPackages
                    // Niente icona nella riga: qui l'informazione è il **nome** e lo stato
                    // dell'interruttore. Venti icone a colori in una lista da scorrere sono
                    // venti richiami in una schermata che serve a toglierne.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleApp(app) }
                            .padding(start = HlScreenMargin, end = HlScreenMargin - 10.dp)
                    ) {
                        Text(
                            text = app.label,
                            color = if (muted) HlPaper else HlPaper.copy(alpha = 0.72f),
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        MinimalSwitch(checked = muted, onCheckedChange = { onToggleApp(app) })
                    }
                    Hairline(modifier = Modifier.padding(horizontal = HlScreenMargin))
                }
            }
        }
    }
}

/**
 * La scelta della durata: un numero grande allineato a sinistra, i preset come numeri in
 * fila, e un `± 5` scritto per il resto.
 *
 * Prima era tutto centrato, con cinque pill smerigliate e due pulsanti tondi `+`/`−`: nove
 * forme disegnate per impostare un numero. Adesso le forme sono zero — la selezione è una
 * sottolineatura, che è il modo più leggero che c'è di dire "questo".
 */
@Composable
private fun DurationPicker(minutes: Int, onSetDuration: (Int) -> Unit, onStart: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = HlScreenMargin)) {
        Spacer(Modifier.height(22.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$minutes",
                color = HlPaper,
                fontSize = 76.sp,
                fontWeight = FontWeight.W200,
                letterSpacing = (-3).sp
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "min",
                color = HlPaper55,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.weight(1f)
            ) {
                FocusRepository.PRESET_MINUTES.forEach { preset ->
                    val selected = preset == minutes
                    Text(
                        text = "$preset",
                        color = if (selected) HlPaper else HlPaper42,
                        fontSize = 15.sp,
                        textDecoration = if (selected) TextDecoration.Underline else null,
                        modifier = Modifier
                            .clickable { onSetDuration(preset) }
                            .padding(vertical = 14.dp)
                    )
                }
            }

            // I due segni restano separati e leggibili come tali: comprimerli in un solo
            // "± 5" da toccare a sinistra o a destra sarebbe stato più pulito e del tutto
            // indovinabile solo da chi l'ha scritto.
            Text(
                text = "−",
                color = HlPaper55,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable { onSetDuration(minutes - FocusRepository.STEP_MINUTES) }
                    .padding(horizontal = 10.dp, vertical = 14.dp)
            )
            Text(
                text = "+",
                color = HlPaper55,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable { onSetDuration(minutes + FocusRepository.STEP_MINUTES) }
                    .padding(horizontal = 10.dp, vertical = 14.dp)
            )
        }

        Spacer(Modifier.height(18.dp))

        PrimaryPill(text = "Inizia", onClick = onStart, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(26.dp))
    }
}

/**
 * La sessione in corso.
 *
 * Il countdown è grande perché è l'unica cosa che serve sapere, e **sotto c'è l'orario di
 * fine**: un orario si legge una volta e si sa, mentre i minuti che scendono invitano a
 * ricontrollare fra dieci minuti — che è esattamente il gesto da non incoraggiare.
 *
 * Le tre righe sotto sono lo stesso ragionamento dello storico: rendere visibile a che punto
 * sei, senza commentarlo.
 */
@Composable
private fun RunningSession(
    state: LauncherUiState,
    remaining: StateFlow<Int>,
    onStop: () -> Unit,
    onClose: () -> Unit
) {
    // Raccolto qui, non nel corpo di FocusScreen: il tick al secondo non deve ricomporre
    // l'elenco delle app con i loro interruttori.
    val remainingSeconds by remaining.collectAsState()
    val clock = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // La durata del tratto di resistenza si ricava qui dall'istante di inizio: in `uiState`
    // sta l'inizio, che cambia solo quando cedi, e la sottrazione la fa questa composizione,
    // che sta già ricomponendo a ogni tick del countdown.
    val streakSeconds = remember(remainingSeconds, state.focusStreakStartMillis) {
        val start = state.focusStreakStartMillis
        if (start <= 0L) 0 else ((System.currentTimeMillis() - start) / 1000L).toInt().coerceAtLeast(0)
    }
    val toRecord = (state.focusRecordSeconds - streakSeconds).coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxSize().padding(HlWideMargin)) {
        Text(
            text = "SESSIONE IN CORSO",
            color = HlPaper.copy(alpha = 0.35f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.8.sp
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = formatFocusRemaining(remainingSeconds),
            color = HlPaper,
            fontSize = 88.sp,
            fontWeight = FontWeight.W200,
            letterSpacing = (-4).sp
        )
        if (state.focusEndsAt > 0L) {
            Text(
                text = "fino alle ${clock.format(Date(state.focusEndsAt))}",
                color = HlPaper42,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(34.dp))

        KeyValueRow("Stai resistendo da", formatFocusDuration(streakSeconds))
        KeyValueRow(
            key = "Al record mancano",
            value = if (state.focusRecordSeconds == 0) {
                "—"
            } else if (toRecord == 0) {
                "è tuo"
            } else {
                formatFocusDuration(toRecord)
            }
        )
        KeyValueRow("App in grigio", "${state.focusPackages.size}")

        Spacer(Modifier.height(26.dp))

        Text(
            text = "Le notifiche continuano ad arrivare e toccarle apre l'app: nessun launcher " +
                "senza root può intercettare quel tocco. Restano raggiungibili anche le app " +
                "recenti e la ricerca di sistema.",
            color = HlPaper.copy(alpha = 0.40f),
            fontSize = 12.sp
        )

        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Terminare è testo sottolineato, non un pulsante: fermarsi deve costare almeno
            // quanto iniziare.
            TextAction(
                text = "Termina ora",
                onClick = onStop,
                color = HlPaper.copy(alpha = 0.45f),
                fontSize = 14.sp,
                underline = true
            )
            Spacer(Modifier.width(24.dp))
            TextAction(text = "Chiudi", onClick = onClose, color = HlPaper.copy(alpha = 0.30f))
        }
    }
}

@Composable
private fun KeyValueRow(key: String, value: String) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
        ) {
            Text(text = key, color = HlPaper55, fontSize = 13.sp, modifier = Modifier.weight(1f))
            MonoValue(text = value)
        }
        Hairline()
    }
}

/**
 * Lo storico di sempre: il record di resistenza e la classifica delle app che apri comunque.
 *
 * Non c'è un elenco delle singole sessioni di proposito — quello che serve non è cos'è
 * successo martedì, ma quali app cedono sistematicamente, e quello si vede solo sommando.
 *
 * I numeri sono **in monospazio**: una classifica con le cifre allineate si legge come una
 * tabella, cioè in un colpo d'occhio, mentre in tondo va letta riga per riga.
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

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = HlScreenMargin)) {
        Hairline()
        Spacer(Modifier.height(22.dp))

        SectionLabel("Record di resistenza")
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (state.focusRecordSeconds > 0) {
                formatFocusDuration(state.focusRecordSeconds)
            } else {
                "nessuno ancora"
            },
            color = HlPaper,
            fontSize = 38.sp,
            fontWeight = FontWeight.W200
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "il tratto più lungo sotto blocco senza aprire niente",
            color = HlPaper.copy(alpha = 0.45f),
            fontSize = 12.sp
        )

        if (state.focusSessionCount > 0) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                MonoValue(
                    text = if (state.focusSessionCount == 1) "1 sessione" else "${state.focusSessionCount} sessioni",
                    color = HlPaper55,
                    fontSize = 12.sp
                )
                MonoValue(
                    text = if (totalBreaks == 1) "1 apertura" else "$totalBreaks aperture",
                    color = HlPaper55,
                    fontSize = 12.sp
                )
                if (totalBreaks > 0) {
                    MonoValue(
                        text = "%.1f a sessione".format(totalBreaks.toFloat() / state.focusSessionCount),
                        color = HlPaper55,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        SectionLabel("Aperte comunque")
        Spacer(Modifier.height(10.dp))

        if (breaks.isEmpty()) {
            Text(
                text = "Ancora nessuna. Quando aprirai un'app durante una sessione la troverai qui.",
                color = HlPaper.copy(alpha = 0.45f),
                fontSize = 13.sp
            )
        } else {
            breaks.forEach { entry ->
                val app = byPackage[entry.packageName]
                // Niente icona: in una classifica di app che ti fregano, l'icona è il logo
                // dell'app che ti frega.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app?.label ?: entry.packageName,
                            color = HlPaper,
                            fontSize = 15.sp
                        )
                        Text(
                            text = formatTimeAgo(entry.lastAtMillis, now),
                            color = HlPaper.copy(alpha = 0.38f),
                            fontSize = 11.sp
                        )
                    }
                    MonoValue(text = "${entry.count}")
                }
            }
        }

        // Fuori dall'elenco: serve anche quando di cedimenti non ce ne sono, per esempio per
        // cancellare un record falsato da un cambio di ora di sistema, che altrimenti
        // resterebbe imbattibile per sempre.
        if (state.focusSessionCount > 0 || state.focusRecordSeconds > 0 || breaks.isNotEmpty()) {
            TextAction(
                text = "Azzera statistiche",
                onClick = onResetStats,
                color = HlPaper55,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(8.dp))
        Hairline()
    }
}

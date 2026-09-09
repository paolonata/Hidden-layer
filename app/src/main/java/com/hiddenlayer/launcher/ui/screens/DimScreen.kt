package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.DimRepository
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.DragHandle
import com.hiddenlayer.launcher.ui.Hairline
import com.hiddenlayer.launcher.ui.MinimalSwitch
import com.hiddenlayer.launcher.ui.PrimaryPill
import com.hiddenlayer.launcher.ui.ScreenHeader
import com.hiddenlayer.launcher.ui.SectionLabel
import com.hiddenlayer.launcher.ui.theme.HlBackground
import com.hiddenlayer.launcher.ui.theme.HlPaper
import com.hiddenlayer.launcher.ui.theme.HlPaper55
import com.hiddenlayer.launcher.ui.theme.HlScreenMargin

/**
 * La luminosità extra: un velo nero sopra tutto, per scendere sotto il minimo di sistema.
 *
 * Attenuare è l'unica cosa che un overlay sa fare bene — sovrapporre del nero riduce la luce
 * in modo esatto, senza gli effetti collaterali di un velo colorato. Per questo qui c'è un
 * cursore solo e nessuna tinta.
 *
 * Il fondo è **quasi pieno**, senza lo sfondo del telefono: questa schermata si apre per
 * rendere lo schermo più scuro, e mostrarci dietro una foto sarebbe una contraddizione.
 *
 * L'avviso del permesso mancante era rosso salmone — l'unico colore saturo rimasto in tutto
 * il launcher, e proprio in una schermata che si usa al buio. Adesso è un'etichetta in
 * maiuscoletto: l'informazione resta, il richiamo cromatico no.
 */
@Composable
fun DimScreen(
    state: LauncherUiState,
    onToggle: (Boolean) -> Unit,
    onLevelChange: (Float) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onDone: () -> Unit
) {
    BackHandler(onBack = onDone)
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize().background(HlBackground)) {
        // Lo sfondo sfocato resta sotto un velo praticamente pieno: serve solo a non far
        // sembrare questa schermata un rettangolo staccato dal resto del launcher.
        BlurredWallpaperBackground(scrimAlpha = 0.96f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            DragHandle(onClose = onDone)
            Column(modifier = Modifier.padding(horizontal = HlScreenMargin)) {
                ScreenHeader(label = "Luminosità extra", actionText = "Chiudi", onAction = onDone)
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = HlScreenMargin)) {
                        Spacer(Modifier.height(24.dp))

                        // Il valore è il soggetto della schermata, quindi è grande. Prima era
                        // una riga di testo ("Intensità — 45%") in mezzo alle altre: l'unica
                        // cosa che si viene a leggere qui, scritta come una didascalia.
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${(state.dimLevel * 100).toInt()}",
                                color = if (state.dimEnabled) HlPaper else HlPaper.copy(alpha = 0.35f),
                                fontSize = 76.sp,
                                fontWeight = FontWeight.W200,
                                letterSpacing = (-3).sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "%",
                                color = HlPaper55,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Traccia da 2dp e pallino pieno: il cursore di Material ha una
                        // traccia da 4dp, un pallino da 20 e un alone attorno — su uno
                        // schermo scuro è la cosa più luminosa della pagina, ed è il comando
                        // di una funzione che serve proprio a togliere luce.
                        Slider(
                            value = state.dimLevel,
                            onValueChange = onLevelChange,
                            valueRange = DimRepository.MIN_LEVEL..DimRepository.MAX_LEVEL,
                            enabled = state.dimEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = HlPaper,
                                activeTrackColor = HlPaper,
                                inactiveTrackColor = HlPaper.copy(alpha = 0.18f)
                            ),
                            track = {
                                val range = DimRepository.MAX_LEVEL - DimRepository.MIN_LEVEL
                                val fraction = if (range > 0f) {
                                    ((state.dimLevel - DimRepository.MIN_LEVEL) / range).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(HlPaper.copy(alpha = 0.18f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .height(2.dp)
                                            .background(
                                                if (state.dimEnabled) HlPaper else HlPaper.copy(alpha = 0.35f)
                                            )
                                    )
                                }
                            },
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(
                                            if (state.dimEnabled) HlPaper else HlPaper.copy(alpha = 0.35f)
                                        )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(24.dp))
                    }
                    Hairline(modifier = Modifier.padding(horizontal = HlScreenMargin))
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = HlScreenMargin,
                                end = HlScreenMargin - 10.dp,
                                top = 8.dp,
                                bottom = 8.dp
                            )
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Attenuazione", color = HlPaper, fontSize = 15.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Vale su tutto il telefono",
                                color = HlPaper.copy(alpha = 0.45f),
                                fontSize = 12.sp
                            )
                        }
                        MinimalSwitch(checked = state.dimEnabled, onCheckedChange = onToggle)
                    }
                    Hairline(modifier = Modifier.padding(horizontal = HlScreenMargin))
                }

                // L'avviso compare solo quando serve davvero: attenuazione accesa ma overlay
                // non concesso, cioè il caso in cui non succede niente e sembrerebbe che la
                // funzione sia rotta.
                if (state.dimEnabled && !state.dimOverlayAllowed) {
                    item {
                        Column(modifier = Modifier.padding(HlScreenMargin)) {
                            SectionLabel("Permesso mancante", color = HlPaper)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Per attenuare lo schermo serve \"Visualizza sopra altre " +
                                    "app\": è l'unico modo che ha un'app di disegnare sopra le " +
                                    "altre, e si concede solo da una schermata di sistema.",
                                color = HlPaper.copy(alpha = 0.70f),
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            PrimaryPill(
                                text = "Concedi il permesso",
                                onClick = onRequestOverlayPermission,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Hairline(modifier = Modifier.padding(horizontal = HlScreenMargin))
                    }
                }

                item {
                    Text(
                        text = "Il cursore non arriva al 100% di proposito: il velo non riceve " +
                            "i tocchi, quindi con uno schermo tutto nero non vedresti più dove " +
                            "premere per riaccenderlo.\n\n" +
                            "Puoi spegnerla da qualunque app con il pulsante sulla notifica, " +
                            "senza tornare qui.\n\n" +
                            "Non copre la schermata di blocco né alcune finestre di sistema, e " +
                            "le app bancarie possono farla sparire mentre sono aperte.",
                        color = HlPaper.copy(alpha = 0.45f),
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(HlScreenMargin)
                    )
                }

                // Documentazione, non interfaccia: sta in fondo perché si legge una volta
                // sola, quando qualcosa non torna.
                item {
                    Text(
                        text = "Su MIUI/HyperOS il risparmio energetico può chiudere il servizio " +
                            "e far sparire l'attenuazione: metti Hidden Layer tra le app senza " +
                            "restrizioni di batteria. Rientrando nel launcher riparte comunque " +
                            "da sé.\n\n" +
                            "Se il tuo Android ha già \"Luminosità extra\" tra le impostazioni " +
                            "di accessibilità, quella è preferibile: agisce sul display invece " +
                            "di sovrapporre un velo, quindi non ha nessuno di questi limiti.",
                        color = HlPaper.copy(alpha = 0.32f),
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(
                            start = HlScreenMargin,
                            end = HlScreenMargin,
                            bottom = HlScreenMargin
                        )
                    )
                }
            }
        }
    }
}

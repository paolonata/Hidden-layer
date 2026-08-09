package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
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
import com.hiddenlayer.launcher.data.NightModeRepository
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.DragHandle
import com.hiddenlayer.launcher.ui.closeOnDragDown

private val NightRed = Color(0xFFFF4A3A)

/**
 * La modalità rossa per l'astrofotografia: due manopole, non una.
 *
 * Il rosso toglie il blu — è quello che rompe l'adattamento al buio — e l'attenuazione toglie
 * luce. Sono cose diverse e ne servono dosi diverse a seconda di quanto è buio il posto,
 * quindi restano due cursori separati invece di un unico "intensità".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NightModeScreen(
    state: LauncherUiState,
    onToggle: (Boolean) -> Unit,
    onRedChange: (Float) -> Unit,
    onDimChange: (Float) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onDone: () -> Unit
) {
    BackHandler(onBack = onDone)
    val listState = rememberLazyListState()

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
        BlurredWallpaperBackground(neutral = state.nightModeEnabled)

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
                        title = { Text("Modalità rossa") },
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
                    ListItem(
                        headlineContent = { Text("Modalità rossa", color = Color.White) },
                        supportingContent = {
                            Text(
                                "Filtra lo schermo per non perdere l'adattamento al buio.",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        },
                        trailingContent = {
                            Switch(checked = state.nightModeEnabled, onCheckedChange = onToggle)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                }

                // L'avviso sul permesso compare solo quando serve davvero: modalità accesa ma
                // overlay non concesso, cioè il caso in cui il rosso c'è solo qui dentro e
                // sembrerebbe che la funzione sia rotta.
                if (state.nightModeEnabled && !state.nightOverlayAllowed) {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Il filtro è attivo solo dentro il launcher.",
                                color = NightRed,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Per filtrare anche le altre app serve il permesso " +
                                    "\"Visualizza sopra altre app\". Senza, aprendo una " +
                                    "qualsiasi app lo schermo torna bianco.",
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onRequestOverlayPermission) {
                                Text("Concedi il permesso")
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    }
                }

                item {
                    LevelSlider(
                        title = "Rosso",
                        description = "Quanto blu togliere. È il blu a rovinare la visione " +
                            "notturna, più che la luminosità.",
                        value = state.nightRedIntensity,
                        range = NightModeRepository.MIN_RED..NightModeRepository.MAX_RED,
                        enabled = state.nightModeEnabled,
                        onChange = onRedChange
                    )
                }

                item {
                    LevelSlider(
                        title = "Attenuazione",
                        description = "Abbassa la luce oltre il minimo di sistema. Utile " +
                            "quando anche la luminosità più bassa del telefono è troppa.",
                        value = state.nightDimLevel,
                        range = NightModeRepository.MIN_DIM..NightModeRepository.MAX_DIM,
                        enabled = state.nightModeEnabled,
                        onChange = onDimChange
                    )
                }

                item {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Text(
                        text = "Cosa aspettarsi. Dentro il launcher il rosso è esatto: con " +
                            "il cursore al massimo verde e blu vengono azzerati, lo sfondo è " +
                            "nero puro e su OLED quei pixel restano proprio spenti. È lo " +
                            "stesso risultato delle app che hanno un tema rosso loro.\n\n" +
                            "Sulle altre app no, ed è un limite di Android, non una " +
                            "regolazione da trovare: un'app può solo stendere un velo sopra " +
                            "le altre, non ridipingerne il contenuto. Un velo può scurire e " +
                            "far virare al rosso, ma non può trasformare il bianco in rosso " +
                            "lasciando il nero nero — per quello servirebbe il multiply, che " +
                            "fra finestre diverse non è concesso a nessuna app senza root.\n\n" +
                            "In pratica: dove un'app ha un suo tema rosso (o almeno un tema " +
                            "scuro), usalo — il velo serve a coprire tutto il resto. Con le " +
                            "app scure il risultato migliora molto abbassando Rosso e " +
                            "alzando Attenuazione.\n\n" +
                            "Il velo non copre la schermata di blocco né alcune finestre di " +
                            "sistema, e le app bancarie possono farlo sparire di proposito " +
                            "mentre sono aperte.\n\n" +
                            "Su MIUI/HyperOS: se il risparmio energetico chiude il servizio " +
                            "il filtro sparisce da solo. Metti Hidden Layer tra le app senza " +
                            "restrizioni di batteria. Puoi spegnere la modalità in qualsiasi " +
                            "momento dalla notifica, senza tornare qui.",
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelSlider(
    title: String,
    description: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            "$title — ${(value * 100).toInt()}%",
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            description,
            color = Color.White.copy(alpha = if (enabled) 0.7f else 0.3f)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = NightRed,
                activeTrackColor = NightRed,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

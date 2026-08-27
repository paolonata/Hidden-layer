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
import com.hiddenlayer.launcher.data.DimRepository
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.DragHandle
import com.hiddenlayer.launcher.ui.closeOnDragDown

private val Warn = Color(0xFFFFB4A3)

/**
 * La luminosità extra: un velo nero sopra tutto, per scendere sotto il minimo di sistema.
 *
 * Attenuare è l'unica cosa che un overlay sa fare bene — sovrapporre del nero riduce la luce
 * in modo esatto, senza gli effetti collaterali di un velo colorato. Per questo qui c'è un
 * cursore solo e nessuna tinta.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                        title = { Text("Luminosità extra") },
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
                        headlineContent = { Text("Luminosità extra", color = Color.White) },
                        supportingContent = {
                            Text(
                                "Scende sotto la luminosità minima di sistema.",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        },
                        trailingContent = {
                            Switch(checked = state.dimEnabled, onCheckedChange = onToggle)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                }

                // L'avviso sul permesso compare solo quando serve davvero: attenuazione accesa
                // ma overlay non concesso, cioè il caso in cui non succede niente e sembrerebbe
                // che la funzione sia rotta.
                if (state.dimEnabled && !state.dimOverlayAllowed) {
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Manca il permesso, quindi non sta succedendo niente.",
                                color = Warn,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Per attenuare lo schermo serve \"Visualizza sopra altre " +
                                    "app\": è l'unico modo che ha un'app di disegnare sopra " +
                                    "le altre, e si concede solo da una schermata di sistema.",
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
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            "Intensità — ${(state.dimLevel * 100).toInt()}%",
                            color = if (state.dimEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Regolala mentre è accesa, così vedi subito l'effetto.",
                            color = Color.White.copy(alpha = if (state.dimEnabled) 0.7f else 0.3f)
                        )
                        Slider(
                            value = state.dimLevel,
                            onValueChange = onLevelChange,
                            valueRange = DimRepository.MIN_LEVEL..DimRepository.MAX_LEVEL,
                            enabled = state.dimEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White.copy(alpha = 0.85f),
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                }

                item {
                    Text(
                        text = "Il cursore non arriva al 100% di proposito: il velo non " +
                            "riceve i tocchi, quindi se fosse completamente opaco lo schermo " +
                            "diventerebbe nero e non vedresti più dove premere per " +
                            "riaccenderlo.\n\n" +
                            "Puoi spegnerla in qualsiasi momento dalla notifica, senza " +
                            "tornare qui — serve, perché con lo schermo molto scuro cercare " +
                            "un interruttore è scomodo.\n\n" +
                            "Non copre la schermata di blocco né alcune finestre di sistema, " +
                            "e le app bancarie possono farla sparire di proposito mentre " +
                            "sono aperte.\n\n" +
                            "Su MIUI/HyperOS: se il risparmio energetico chiude il servizio " +
                            "l'attenuazione sparisce da sola. Metti Hidden Layer tra le app " +
                            "senza restrizioni di batteria. Rientrando nel launcher riparte " +
                            "comunque da sé.\n\n" +
                            "Se il tuo Android ha già \"Luminosità extra\" fra le " +
                            "impostazioni di accessibilità, quella è preferibile: agisce sul " +
                            "display invece di sovrapporre un velo, quindi non ha nessuno di " +
                            "questi limiti.",
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

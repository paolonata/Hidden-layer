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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    onSetDuration: (Int) -> Unit,
    onToggleApp: (AppInfo) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDone: () -> Unit
) {
    BackHandler(onBack = onDone)
    val listState = rememberLazyListState()
    var query by remember { mutableStateOf("") }

    // Deliberately not re-sorted with the selected ones on top: the list would reshuffle
    // under your finger every time you flick a switch.
    val apps = remember(state.allApps, query) {
        state.allApps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
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
                Column {
                    DragHandle(onClose = onDone)
                    TopAppBar(
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
                        RunningSession(
                            remainingSeconds = state.focusRemainingSeconds,
                            onStop = onStop
                        )
                    } else {
                        DurationPicker(
                            minutes = state.focusDurationMinutes,
                            onSetDuration = onSetDuration,
                            onStart = onStart
                        )
                    }
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
private fun RunningSession(remainingSeconds: Int, onStop: () -> Unit) {
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
            text = formatRemaining(remainingSeconds),
            color = Color.White,
            fontSize = 52.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(Modifier.height(20.dp))
        PrimaryPill(label = "Termina ora", onClick = onStop, prominent = false)
    }
}

private fun formatRemaining(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
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

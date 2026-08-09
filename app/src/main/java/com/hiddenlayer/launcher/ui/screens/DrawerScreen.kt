package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hiddenlayer.launcher.DrawerMode
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.ui.AppGridTile
import com.hiddenlayer.launcher.ui.AppSearchField
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.DragHandle
import com.hiddenlayer.launcher.ui.SecureScreen
import com.hiddenlayer.launcher.ui.closeOnDragDown

/** Quanto dura la dissolvenza fra richiesta di sblocco e app nascoste. Corta: è un cambio di
 * posto, non un'animazione da guardare. */
private const val FADE_MILLIS = 200

/** Chrome's incognito grey: flat, cold and deliberately not "your wallpaper, but darker". */
private val IncognitoSurface = Color(0xFF202124)
private val IncognitoAccent = Color(0xFFBDC1C6)

/**
 * Cassetto normale e app nascoste sono la **stessa schermata in due versioni**, decise da
 * [hiddenDrawer] e mai scambiate mentre sei dentro. Quale delle due arriva lo dice `Screen`,
 * non un campo dello stato letto all'ingresso: erano lo stesso `Screen.DRAWER` e chiudere il
 * cassetto per riaprire subito quello nascosto ricadeva sulla composizione ancora in uscita,
 * che si riapriva com'era (vedi il commento sull'enum `Screen`).
 *
 * Ci si è arrivati per gradi. Era un pager: lo swipe a sinistra ci portava, e siccome un pager
 * mostra la pagina già durante il trascinamento bastava una scorsa accidentale per scoprire
 * che esisteva. Poi sono stati due punti in fondo al cassetto da toccare due volte: niente
 * scorrimento laterale, ma restava un elemento visibile — e un elemento visibile, prima o poi,
 * qualcuno lo tocca. Adesso **non c'è nessun ingresso disegnato da nessuna parte**: alle app
 * nascoste si arriva solo con lo swipe su a **due dita** dalla home (vedi `HomeScreen`), che
 * non lascia traccia sullo schermo e non capita per sbaglio.
 *
 * Da qui si esce come da qualunque altra schermata — X, trascinamento verso il basso,
 * indietro — e si torna sempre alla home, che è da dove si è entrati. Uscendo lo sblocco
 * decade subito ([onRelock]): tenerlo valido fino a quando il launcher va in pausa
 * significherebbe che chi prende il telefono in mano nei dieci secondi dopo entra senza
 * impronta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerScreen(
    state: LauncherUiState,
    hiddenDrawer: Boolean,
    canUseBiometrics: () -> Boolean,
    onRequestBiometric: () -> Unit,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    onHiddenAppClick: (AppInfo) -> Unit,
    onHiddenAppLongPress: (AppInfo) -> Unit,
    onPinSubmit: (String) -> Unit,
    onClearUnlockError: () -> Unit,
    onOpenSettings: () -> Unit,
    onRelock: () -> Unit,
    onClose: () -> Unit
) {
    val locked = state.unlockRequired && !state.vaultUnlocked

    if (hiddenDrawer) SecureScreen()

    val leave = {
        if (hiddenDrawer) onRelock()
        onClose()
    }

    BackHandler(onBack = leave)

    LaunchedEffect(locked) {
        if (hiddenDrawer && locked && canUseBiometrics()) onRequestBiometric()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BlurredWallpaperBackground(scrimAlpha = 0.28f, neutral = state.nightModeEnabled)
        // Sulle nascoste lo sfondo reale sparisce dietro una superficie incognito piatta, così
        // la pagina si legge come un altro posto e non come una versione più scura di questo.
        if (hiddenDrawer) {
            Box(modifier = Modifier.fillMaxSize().background(IncognitoSurface))
        }

        if (!hiddenDrawer) {
            AllAppsPage(
                state = state,
                onQueryChange = onQueryChange,
                onAppClick = onAppClick,
                onAppLongPress = onAppLongPress,
                onClose = leave
            )
        } else {
            AnimatedContent(
                targetState = locked,
                transitionSpec = {
                    fadeIn(tween(FADE_MILLIS)) togetherWith fadeOut(tween(FADE_MILLIS))
                },
                label = "vault"
            ) { isLocked ->
                if (isLocked) {
                    UnlockPage(
                        error = state.unlockError,
                        canUseBiometrics = canUseBiometrics(),
                        onBiometricRequest = onRequestBiometric,
                        onPinSubmit = onPinSubmit,
                        onClearError = onClearUnlockError,
                        onClose = leave
                    )
                } else {
                    HiddenAppsPage(
                        state = state,
                        onQueryChange = onQueryChange,
                        onAppClick = onHiddenAppClick,
                        onAppLongPress = onHiddenAppLongPress,
                        onOpenSettings = onOpenSettings,
                        onClose = leave
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllAppsPage(
    state: LauncherUiState,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    onClose: () -> Unit
) {
    val gridState = rememberLazyGridState()

    Box(modifier = Modifier.fillMaxSize().closeOnPull(gridState, onClose)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    DragHandle(onClose = onClose)
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        title = {
                            AppSearchField(
                                value = state.query,
                                onValueChange = onQueryChange,
                                placeholder = if (state.drawerMode == DrawerMode.BROWSE) {
                                    "Cerca app"
                                } else {
                                    "Scegli un'app"
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                            }
                        }
                    )
                }
            }
        ) { padding ->
            AppGrid(
                apps = state.visibleApps,
                gridState = gridState,
                padding = padding,
                isMuted = state::isMuted,
                onAppClick = onAppClick,
                onAppLongPress = onAppLongPress
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HiddenAppsPage(
    state: LauncherUiState,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    val gridState = rememberLazyGridState()

    Box(modifier = Modifier.fillMaxSize().closeOnPull(gridState, onClose)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    DragHandle(onClose = onClose)
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        title = {
                            AppSearchField(
                                value = state.query,
                                onValueChange = onQueryChange,
                                placeholder = "Cerca tra le nascoste",
                                leadingIcon = Icons.Default.VisibilityOff
                            )
                        },
                        // Una X come nel cassetto normale. Prima qui c'era solo un'icona
                        // decorativa e l'unica uscita evidente era il tasto indietro di
                        // sistema, che per una schermata aperta con un gesto non si trova.
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Chiudi",
                                    tint = IncognitoAccent
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onOpenSettings) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Gestisci app nascoste",
                                    tint = Color.White
                                )
                            }
                        }
                    )
                }
            }
        ) { padding ->
            if (state.hiddenApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nessuna app nascosta. Tieni premuto su un'app e scegli \"Nascondi app\", oppure usa l'icona impostazioni qui sopra.",
                        modifier = Modifier.padding(32.dp),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            } else {
                AppGrid(
                    apps = state.hiddenVisibleApps,
                    gridState = gridState,
                    padding = padding,
                    isMuted = state::isMuted,
                    onAppClick = onAppClick,
                    onAppLongPress = onAppLongPress
                )
            }
        }
    }
}

@Composable
private fun UnlockPage(
    error: Boolean,
    canUseBiometrics: Boolean,
    onBiometricRequest: () -> Unit,
    onPinSubmit: (String) -> Unit,
    onClearError: () -> Unit,
    onClose: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 8) pin = it.filter(Char::isDigit)
                onClearError()
            },
            label = { Text("PIN", color = Color.White.copy(alpha = 0.8f)) },
            singleLine = true,
            isError = error,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.7f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                cursorColor = Color.White
            )
        )

        if (error) {
            Spacer(Modifier.height(8.dp))
            Text("PIN errato", color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("Chiudi", color = Color.White) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onPinSubmit(pin) }) { Text("Sblocca") }
            if (canUseBiometrics) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onBiometricRequest) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("Impronta", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AppGrid(
    apps: List<AppInfo>,
    gridState: LazyGridState,
    padding: PaddingValues,
    isMuted: (AppInfo) -> Boolean,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit
) {
    LazyVerticalGrid(
        state = gridState,
        // Five per row, come il dock: senza etichette sotto le icone la riga da quattro
        // lasciava troppo vuoto ai lati.
        columns = GridCells.Fixed(5),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().padding(padding)
    ) {
        items(apps, key = { it.componentName.flattenToString() }) { app ->
            AppGridTile(
                app = app,
                onTap = { onAppClick(app) },
                onLongPress = { onAppLongPress(app) },
                grayscale = isMuted(app)
            )
        }
    }
}

@Composable
private fun Modifier.closeOnPull(gridState: LazyGridState, onClose: () -> Unit): Modifier =
    this.closeOnDragDown(
        canClose = {
            gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
        },
        onClose = onClose
    )

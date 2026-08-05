package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
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

/** Il valore di `drawerStartPage` che significa "apri direttamente sulle nascoste": succede
 * solo tornando dalle loro impostazioni. */
private const val PAGE_HIDDEN = 1

/** Quanto dura la dissolvenza fra cassetto e app nascoste. Corta: è un cambio di posto, non
 * un'animazione da guardare. */
private const val FADE_MILLIS = 200

// Stessa presenza della maniglietta in cima al cassetto (vedi DragHandle in
// CloseGestures.kt): stesso bianco al 60%, stessa aria attorno. Devono leggersi come un
// elemento dell'interfaccia al pari degli altri, non come un dettaglio da cercare.
private val DOT_SIZE = 8.dp
private val DOT_SPACING = 10.dp
private val DOT_COLOR_ALPHA = 0.6f
private val TOUCH_WIDTH = 96.dp
private val TOUCH_HEIGHT = 44.dp

/** Chrome's incognito grey: flat, cold and deliberately not "your wallpaper, but darker". */
private val IncognitoSurface = Color(0xFF202124)
private val IncognitoAccent = Color(0xFFBDC1C6)

/**
 * Cassetto e app nascoste sono due schermate **sovrapposte**, non due pagine affiancate.
 *
 * Era un pager: lo swipe a sinistra ci portava, e siccome un pager mostra la pagina già
 * durante il trascinamento bastava una scorsa accidentale per scoprire che esisteva. Ora ci
 * si arriva solo col doppio tap sui due punti in fondo, e il passaggio è una **dissolvenza**:
 * uno scorrimento laterale racconterebbe comunque che c'è "la pagina di fianco", che è
 * esattamente l'unica cosa che non deve trapelare. Sparito il pager, sparisce anche l'ultimo
 * indizio — nessuna resistenza al bordo, nessun rimbalzo, niente da provare.
 *
 * Con lo sblocco attivo si arriva alla richiesta di PIN invece che alle app. La protezione da
 * screenshot e anteprime nei recenti si accende appena si passa di là.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerScreen(
    state: LauncherUiState,
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
    onClose: () -> Unit
) {
    val locked = state.unlockRequired && !state.vaultUnlocked
    // Si parte dalle nascoste solo tornando dalle loro impostazioni. `remember` senza chiavi
    // basta: uscendo dal cassetto questa schermata viene smontata, quindi al rientro il
    // valore viene riletto — e lockVault azzera drawerStartPage.
    var showHidden by remember { mutableStateOf(state.drawerStartPage == PAGE_HIDDEN) }

    if (showHidden) SecureScreen()

    // Dalle nascoste il tasto indietro riporta al cassetto normale invece di chiudere tutto:
    // senza lo swipe non ci sarebbe altro modo di tornare senza uscire.
    BackHandler(enabled = !showHidden, onBack = onClose)
    BackHandler(enabled = showHidden) { showHidden = false }

    // Search text belongs to the page you're on, not to the drawer as a whole.
    LaunchedEffect(showHidden) { onQueryChange("") }

    LaunchedEffect(showHidden, locked) {
        if (showHidden && locked && canUseBiometrics()) onRequestBiometric()
    }

    // Passando alle nascoste lo sfondo reale sparisce dietro una superficie incognito piatta,
    // così la pagina si legge come un altro posto e non come una versione più scura di questo.
    val incognito by animateFloatAsState(
        targetValue = if (showHidden) 1f else 0f,
        animationSpec = tween(FADE_MILLIS),
        label = "incognito"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        BlurredWallpaperBackground(scrimAlpha = 0.28f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(IncognitoSurface.copy(alpha = incognito))
        )

        AnimatedContent(
            targetState = showHidden,
            transitionSpec = {
                fadeIn(tween(FADE_MILLIS)) togetherWith fadeOut(tween(FADE_MILLIS))
            },
            label = "drawer-page"
        ) { hidden ->
            when {
                !hidden -> AllAppsPage(
                    state = state,
                    onQueryChange = onQueryChange,
                    onAppClick = onAppClick,
                    onAppLongPress = onAppLongPress,
                    onRevealHidden = {
                        // Inerte mentre stai scegliendo un'app per la home o per il dock:
                        // lì il cassetto è un selettore e non c'è nessun altro posto dove
                        // andare. I punti restano disegnati, così non cambiano di aspetto a
                        // seconda del modo — sono decorazione, e devono sembrarlo sempre.
                        if (state.drawerMode == DrawerMode.BROWSE) showHidden = true
                    },
                    onClose = onClose
                )

                locked -> UnlockPage(
                    error = state.unlockError,
                    canUseBiometrics = canUseBiometrics(),
                    onBiometricRequest = onRequestBiometric,
                    onPinSubmit = onPinSubmit,
                    onClearError = onClearUnlockError,
                    onClose = onClose
                )

                else -> HiddenAppsPage(
                    state = state,
                    onQueryChange = onQueryChange,
                    onAppClick = onHiddenAppClick,
                    onAppLongPress = onHiddenAppLongPress,
                    onOpenSettings = onOpenSettings,
                    onClose = onClose
                )
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
    onRevealHidden: () -> Unit,
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
            },
            bottomBar = { HiddenDoorDots(onOpen = onRevealHidden) }
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
                        navigationIcon = {
                            Icon(
                                Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = IncognitoAccent,
                                modifier = Modifier.padding(start = 12.dp)
                            )
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

/**
 * I due punti in fondo al cassetto: **l'unica strada** per le app nascoste.
 *
 * Sta in basso e non sulla maniglietta in cima perché è lì che arriva il pollice senza
 * cambiare presa, e ha lo stesso aspetto della maniglietta perché deve sembrare un elemento
 * dell'interfaccia come gli altri. I due punti sono **uguali fra loro**: un indicatore di
 * pagina, con uno acceso e uno spento, direbbe che esiste una seconda pagina — che è
 * esattamente ciò che non deve trapelare.
 *
 * Si apre solo col **doppio tap**. Un tocco singolo non fa niente e non produce nessun
 * segnale, quindi chi ci finisce sopra per caso non scopre nulla; e non essendoci un `onTap`
 * nello stesso rilevatore, il tocco singolo non viene nemmeno ritardato. L'area sensibile è
 * un rettangolo centrato attorno ai punti, non tutta la striscia in fondo, così un dito
 * appoggiato al bordo mentre leggi non la attiva.
 */
@Composable
private fun HiddenDoorDots(onOpen: () -> Unit) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            // Con spacedBy e basta i punti si impacchettano a sinistra dell'area sensibile e
            // finiscono fuori asse rispetto alla maniglietta, che è centrata davvero.
            horizontalArrangement = Arrangement.spacedBy(DOT_SPACING, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .width(TOUCH_WIDTH)
                .height(TOUCH_HEIGHT)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpen()
                        }
                    )
                },
            content = {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .size(DOT_SIZE)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = DOT_COLOR_ALPHA))
                    )
                }
            }
        )
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

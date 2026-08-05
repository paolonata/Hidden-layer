package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.hiddenlayer.launcher.DrawerMode
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.ui.AppGridTile
import com.hiddenlayer.launcher.ui.AppSearchField
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.DragHandle
import com.hiddenlayer.launcher.ui.SecureScreen
import com.hiddenlayer.launcher.ui.closeOnDragDown

private const val PAGE_ALL_APPS = 0
private const val PAGE_HIDDEN = 1

/** Quanto vanno tenuti premuti i due punti in fondo. Molto più della soglia di sistema
 * (mezzo secondo): deve essere un gesto che non si fa mai per caso, perché è l'unica cosa che
 * rivela l'esistenza delle app nascoste. */
private const val SECRET_HOLD_MILLIS = 1_500L

/** Quanto può scivolare il dito senza annullare il tocco lungo. */
private val HOLD_SLOP = 12.dp
private val DOT_SIZE = 5.dp
private val DOT_SPACING = 7.dp
private val TOUCH_WIDTH = 96.dp
private val TOUCH_HEIGHT = 44.dp

/** Chrome's incognito grey: flat, cold and deliberately not "your wallpaper, but darker". */
private val IncognitoSurface = Color(0xFF202124)
private val IncognitoAccent = Color(0xFFBDC1C6)

/**
 * The drawer is two pages side by side: all apps, and — one swipe to the left — the hidden
 * ones. Making them pages of a pager rather than separate screens is what lets the
 * transition follow the finger instead of being a jump, and it means nothing in the UI has
 * to advertise that the hidden page exists (there is no menu entry for it anywhere).
 *
 * The flip side of a horizontal swipe is that it can be triggered by accident, so when the
 * lock is switched on the second page renders the unlock prompt rather than the apps — a
 * stray swipe reaches a PIN screen, never the contents. Screenshot/recents protection kicks
 * in as soon as the pager starts heading that way, not once it arrives.
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
    BackHandler(onBack = onClose)

    val pagerState = rememberPagerState(
        initialPage = state.drawerStartPage.coerceIn(PAGE_ALL_APPS, PAGE_HIDDEN),
        pageCount = { 2 }
    )
    val locked = state.unlockRequired && !state.vaultUnlocked
    val scope = rememberCoroutineScope()

    val headingForHidden = pagerState.currentPage == PAGE_HIDDEN || pagerState.targetPage == PAGE_HIDDEN
    if (headingForHidden) SecureScreen()

    // Search text belongs to the page you're on, not to the drawer as a whole.
    LaunchedEffect(pagerState.currentPage) { onQueryChange("") }

    // Only once the swipe has settled — not while merely peeking mid-drag.
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress, locked) {
        if (!pagerState.isScrollInProgress &&
            pagerState.currentPage == PAGE_HIDDEN &&
            locked &&
            canUseBiometrics()
        ) {
            onRequestBiometric()
        }
    }

    // Travelling towards the hidden page fades the wallpaper out behind a flat, near-black
    // incognito surface, so by the time you arrive the wallpaper is gone entirely and the
    // page reads as a separate place rather than a darker shade of the same one.
    val progress = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(PAGE_ALL_APPS.toFloat(), PAGE_HIDDEN.toFloat())

    Box(modifier = Modifier.fillMaxSize()) {
        BlurredWallpaperBackground(scrimAlpha = 0.28f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(IncognitoSurface.copy(alpha = progress))
        )

        HorizontalPager(
            state = pagerState,
            // Lo swipe NON porta più alle app nascoste: era troppo facile da scovare, e
            // trattandosi di un pager la pagina si affacciava già durante il trascinamento —
            // bastava una scorsa accidentale per sapere che c'era qualcosa. Ci si arriva solo
            // col tocco lungo sui due punti in fondo. Lo scorrimento resta abilitato mentre
            // sei sulla pagina nascosta, così torni indietro con il gesto naturale.
            //
            // While picking an app for the home screen or the dock there is nowhere else to go.
            userScrollEnabled = state.drawerMode == DrawerMode.BROWSE && headingForHidden,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                PAGE_ALL_APPS -> AllAppsPage(
                    state = state,
                    onQueryChange = onQueryChange,
                    onAppClick = onAppClick,
                    onAppLongPress = onAppLongPress,
                    onRevealHidden = {
                        // Inerte mentre stai scegliendo un'app per la home o per il dock:
                        // lì il cassetto è un selettore e non c'è nessun altro posto dove
                        // andare. I punti restano disegnati, così non cambiano di aspetto a
                        // seconda del modo — sono decorazione, e devono sembrarlo sempre.
                        if (state.drawerMode == DrawerMode.BROWSE) {
                            scope.launch { pagerState.animateScrollToPage(PAGE_HIDDEN) }
                        }
                    },
                    onClose = onClose
                )

                else -> if (locked) {
                    UnlockPage(
                        error = state.unlockError,
                        canUseBiometrics = canUseBiometrics(),
                        onBiometricRequest = onRequestBiometric,
                        onPinSubmit = onPinSubmit,
                        onClearError = onClearUnlockError,
                        onClose = onClose
                    )
                } else {
                    HiddenAppsPage(
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
            bottomBar = { HiddenDoorDots(onHold = onRevealHidden) }
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
 * cambiare presa. Sono due punti spenti e uguali fra loro, non un indicatore di pagina con
 * uno acceso: devono leggersi come decorazione, non come "esiste una seconda pagina".
 *
 * Si apre solo tenendoli premuti per SECRET_HOLD_MILLIS — molto più della soglia di sistema,
 * che è mezzo secondo. Un tocco normale non fa niente e non dà alcun segnale: chi ci finisce
 * sopra per caso non scopre nulla. L'area sensibile è un rettangolo centrato attorno ai punti,
 * non tutta la striscia in fondo, così un dito appoggiato al bordo mentre leggi non la attiva.
 */
@Composable
private fun HiddenDoorDots(onHold: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val slopPx = remember(density) { with(density) { HOLD_SLOP.toPx() } }

    Box(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(DOT_SPACING),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .width(TOUCH_WIDTH)
                .height(TOUCH_HEIGHT)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var travelled = 0f
                        // withTimeoutOrNull torna null solo se scade il tempo, cioè se il dito
                        // è rimasto giù e fermo per tutta la durata.
                        val heldStill = withTimeoutOrNull(SECRET_HOLD_MILLIS) {
                            while (true) {
                                val change = awaitPointerEvent().changes
                                    .firstOrNull { it.id == down.id } ?: break
                                travelled += change.positionChange().getDistance()
                                if (!change.pressed || travelled > slopPx) break
                            }
                        } == null

                        if (heldStill) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHold()
                            do {
                                val change = awaitPointerEvent().changes
                                    .firstOrNull { it.id == down.id } ?: break
                                change.consume()
                            } while (change.pressed)
                        }
                    }
                },
            content = {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .size(DOT_SIZE)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.30f))
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

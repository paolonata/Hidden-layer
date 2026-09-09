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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiddenlayer.launcher.DrawerMode
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.ui.AppGridTile
import com.hiddenlayer.launcher.ui.AppSearchField
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.DragHandle
import com.hiddenlayer.launcher.ui.Hairline
import com.hiddenlayer.launcher.ui.ScreenHeader
import com.hiddenlayer.launcher.ui.SectionLabel
import com.hiddenlayer.launcher.ui.SecureScreen
import com.hiddenlayer.launcher.ui.TextAction
import com.hiddenlayer.launcher.ui.closeOnDragDown
import com.hiddenlayer.launcher.ui.theme.HlBackgroundDeep
import com.hiddenlayer.launcher.ui.theme.HlPaper
import com.hiddenlayer.launcher.ui.theme.HlPaper42
import com.hiddenlayer.launcher.ui.theme.HlPaper55
import com.hiddenlayer.launcher.ui.theme.HlScreenMargin
import com.hiddenlayer.launcher.ui.theme.HlWideMargin

/** Quanto dura la dissolvenza fra richiesta di sblocco e app nascoste. Corta: è un cambio di
 * posto, non un'animazione da guardare. */
private const val FADE_MILLIS = 200

/** L'accento delle app nascoste. Era il grigio-azzurro incognito di Chrome (#BDC1C6): fuori
 * posto ora che tutto il launcher ha un unico accento caldo, e comunque un colore in più. */
private val IncognitoAccent = HlPaper42

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
 * Da qui si esce come da qualunque altra schermata — "Chiudi", trascinamento verso il basso,
 * indietro — e si torna sempre alla home, che è da dove si è entrati. Uscendo lo sblocco
 * decade subito ([onRelock]): tenerlo valido fino a quando il launcher va in pausa
 * significherebbe che chi prende il telefono in mano nei dieci secondi dopo entra senza
 * impronta.
 */
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
        // Il velo è molto più fitto di prima (0.28 → 0.80): lo sfondo del telefono resta
        // percepibile come profondità, ma smette di essere un'immagine da guardare dietro
        // alle icone.
        BlurredWallpaperBackground(scrimAlpha = 0.80f)
        // Sulle nascoste lo sfondo reale sparisce dietro una superficie piatta, così la
        // pagina si legge come un altro posto e non come una versione più scura di questo.
        if (hiddenDrawer) {
            Box(modifier = Modifier.fillMaxSize().background(HlBackgroundDeep))
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

/**
 * L'impalcatura comune alle due pagine: maniglia, intestazione, ricerca, poi il contenuto.
 *
 * Ha preso il posto dello `Scaffold` con `TopAppBar`. Quella barra portava con sé 64dp di
 * altezza fissa, una X in un cerchio di ripple e un titolo in corpo grande, per dire una cosa
 * che ora dicono due parole in maiuscoletto — e in mezzo ci stava schiacciato il campo di
 * ricerca, che è l'unica cosa lì dentro con cui si interagisce davvero.
 */
@Composable
private fun DrawerFrame(
    label: String,
    actionText: String,
    onAction: () -> Unit,
    onClose: () -> Unit,
    labelColor: Color,
    search: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        DragHandle(onClose = onClose)
        Column(modifier = Modifier.padding(horizontal = HlScreenMargin)) {
            ScreenHeader(
                label = label,
                actionText = actionText,
                onAction = onAction,
                labelColor = labelColor
            )
            search()
        }
        content()
    }
}

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
        DrawerFrame(
            label = if (state.drawerMode == DrawerMode.BROWSE) "Tutte le app" else "Scegli un'app",
            actionText = "Chiudi",
            onAction = onClose,
            onClose = onClose,
            labelColor = HlPaper55,
            search = {
                AppSearchField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = if (state.drawerMode == DrawerMode.BROWSE) {
                        "Cerca app"
                    } else {
                        "Scegli un'app"
                    }
                )
            }
        ) {
            AppGrid(
                apps = state.visibleApps,
                gridState = gridState,
                isMuted = state::isMuted,
                onAppClick = onAppClick,
                onAppLongPress = onAppLongPress,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

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
        DrawerFrame(
            label = "Riservate",
            // "Impostazioni" scritto invece dell'ingranaggio. L'ingranaggio era l'unico
            // elemento riconoscibile a colpo d'occhio di tutta la schermata — cioè la cosa
            // che qualcuno che si trovasse qui per caso toccherebbe per prima.
            actionText = "Impostazioni",
            onAction = onOpenSettings,
            onClose = onClose,
            labelColor = IncognitoAccent,
            search = {
                AppSearchField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = "Cerca tra le nascoste",
                    leadingIcon = Icons.Default.VisibilityOff
                )
            }
        ) {
            if (state.hiddenApps.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessuna app nascosta. Tieni premuto su un'app e scegli " +
                            "\"Nascondi app\", oppure apri le impostazioni qui sopra.",
                        modifier = Modifier.padding(HlWideMargin),
                        color = HlPaper55,
                        fontSize = 13.sp
                    )
                }
            } else {
                AppGrid(
                    apps = state.hiddenVisibleApps,
                    gridState = gridState,
                    isMuted = state::isMuted,
                    onAppClick = onAppClick,
                    onAppLongPress = onAppLongPress,
                    modifier = Modifier.weight(1f)
                )
                // Il promemoria di come ci si arriva. Sta scritto qui e da nessun'altra parte:
                // è l'unico posto già protetto in cui dirlo senza rivelare niente a chi la
                // schermata non l'ha mai vista.
                Text(
                    text = "Nessun ingresso disegnato. Si arriva qui solo con lo swipe su a " +
                        "due dita dalla home.",
                    color = HlPaper.copy(alpha = 0.30f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = HlScreenMargin, vertical = 14.dp)
                )
            }
        }
    }
}

/**
 * La richiesta del PIN.
 *
 * L'`OutlinedTextField` di Material portava etichetta fluttuante, contenitore squadrato e
 * bordo rosso in caso di errore: tre convenzioni di un modulo da compilare, per un gesto che
 * dura due secondi. Al suo posto **quattro caselle con il solo bordo inferiore**, che è la
 * forma con cui si scrive un PIN ovunque, e un `BasicTextField` invisibile sopra a raccogliere
 * i tasti — il campo c'è, semplicemente non si vede.
 *
 * Le caselle sono quattro a riposo ma diventano tante quante le cifre digitate, fino a otto:
 * il PIN salvato può essere più lungo di quattro, e mostrarne sempre e solo quattro farebbe
 * sembrare rotto un inserimento perfettamente valido.
 */
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
    val boxCount = maxOf(4, pin.length).coerceAtMost(8)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0A09))
            .systemBarsPadding()
            .padding(HlWideMargin),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "RISERVATE",
            color = HlPaper42,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.8.sp
        )

        Spacer(Modifier.height(26.dp))

        Box {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(boxCount) { index ->
                    val filled = index < pin.length
                    Column(
                        modifier = Modifier.width(46.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.height(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (filled) {
                                Text("•", color = HlPaper, fontSize = 26.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    HlPaper.copy(alpha = if (filled) 0.50f else 0.20f)
                                )
                        )
                    }
                }
            }
            // Il campo vero, steso sopra le caselle e completamente trasparente: raccoglie i
            // tasti e il fuoco, e il tocco sulle caselle lo raggiunge perché è lui a stare
            // davanti. Disegnare le caselle e basta lascerebbe una schermata su cui la
            // tastiera non si apre mai.
            BasicTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 8) pin = it.filter(Char::isDigit)
                    onClearError()
                },
                singleLine = true,
                textStyle = TextStyle(color = Color.Transparent),
                cursorBrush = SolidColor(Color.Transparent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onPinSubmit(pin) }),
                modifier = Modifier.matchParentSize()
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = if (error) {
                "PIN errato. Riprova."
            } else {
                "Impronta o PIN. Lo sblocco vale solo finché resti su questa schermata."
            },
            color = HlPaper.copy(alpha = 0.45f),
            fontSize = 13.sp
        )

        Spacer(Modifier.height(30.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canUseBiometrics) {
                TextAction(
                    text = "Impronta",
                    onClick = onBiometricRequest,
                    color = HlPaper,
                    fontSize = 15.sp,
                    underline = true
                )
                Spacer(Modifier.width(22.dp))
            }
            // Compare solo quando c'è qualcosa da mandare. Il tasto "fine" della tastiera fa
            // lo stesso, ma non si può contare su una tastiera che l'utente vede solo mentre
            // scrive: senza questo, con le dita già alzate non ci sarebbe più modo di
            // confermare.
            if (pin.isNotEmpty()) {
                TextAction(
                    text = "Sblocca",
                    onClick = { onPinSubmit(pin) },
                    color = HlPaper,
                    fontSize = 15.sp,
                    underline = true
                )
                Spacer(Modifier.width(22.dp))
            }
            TextAction(text = "Chiudi", onClick = onClose, color = HlPaper42, fontSize = 15.sp)
        }
    }
}

@Composable
private fun AppGrid(
    apps: List<AppInfo>,
    gridState: LazyGridState,
    isMuted: (AppInfo) -> Boolean,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    // Separate solo quando serve: fuori da una sessione isMuted è sempre falso per tutti, e
    // "bloccate" resta vuoto — niente intestazione, griglia unica come prima. Durante una
    // sessione invece le icone bloccate finiscono in mezzo a quelle disponibili, ed è proprio
    // lì che si vuole sapere al volo cosa si può ancora aprire.
    val blocked = apps.filter(isMuted)
    val available = if (blocked.isEmpty()) apps else apps.filterNot(isMuted)

    LazyVerticalGrid(
        state = gridState,
        // Five per row, come il dock: senza etichette sotto le icone la riga da quattro
        // lasciava troppo vuoto ai lati.
        columns = GridCells.Fixed(5),
        contentPadding = PaddingValues(HlScreenMargin),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(available, key = { it.componentName.flattenToString() }) { app ->
            AppGridTile(
                app = app,
                onTap = { onAppClick(app) },
                onLongPress = { onAppLongPress(app) },
                grayscale = isMuted(app)
            )
        }

        if (blocked.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "blocked-section-header") {
                Column(modifier = Modifier.padding(top = 10.dp, bottom = 14.dp)) {
                    Hairline()
                    Spacer(Modifier.height(14.dp))
                    SectionLabel("Bloccate · ${blocked.size}")
                }
            }
            items(blocked, key = { it.componentName.flattenToString() }) { app ->
                AppGridTile(
                    app = app,
                    onTap = { onAppClick(app) },
                    onLongPress = { onAppLongPress(app) },
                    grayscale = true,
                    // Il fondo appena accennato è il **secondo** segnale, oltre alla
                    // desaturazione: togliere il colore a un'icona già in bianco e nero non
                    // cambia un pixel, ed è per questo che serve qualcosa che agisca sul
                    // rapporto con lo sfondo invece che sui colori dell'icona.
                    tinted = true
                )
            }
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

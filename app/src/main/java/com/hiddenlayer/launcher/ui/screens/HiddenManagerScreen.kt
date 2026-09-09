package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.ui.AppIcon
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.DragHandle
import com.hiddenlayer.launcher.ui.Hairline
import com.hiddenlayer.launcher.ui.MinimalSwitch
import com.hiddenlayer.launcher.ui.MonoValue
import com.hiddenlayer.launcher.ui.PrimaryPill
import com.hiddenlayer.launcher.ui.PromptBody
import com.hiddenlayer.launcher.ui.PromptSheet
import com.hiddenlayer.launcher.ui.PromptTitle
import com.hiddenlayer.launcher.ui.ScreenHeader
import com.hiddenlayer.launcher.ui.SectionLabel
import com.hiddenlayer.launcher.ui.SecureScreen
import com.hiddenlayer.launcher.ui.TextAction
import com.hiddenlayer.launcher.ui.closeOnDragDown
import com.hiddenlayer.launcher.ui.theme.HlCardShape
import com.hiddenlayer.launcher.ui.theme.HlPaper
import com.hiddenlayer.launcher.ui.theme.HlPaper42
import com.hiddenlayer.launcher.ui.theme.HlPaper55
import com.hiddenlayer.launcher.ui.theme.HlScreenMargin
import com.hiddenlayer.launcher.ui.theme.HlSurface

/** Il raggio della carta, ripetuto sulla prima e sull'ultima riga di ogni gruppo. */
private val CARD_RADIUS = 20.dp

/**
 * Settings screen for the vault: whether opening it needs an unlock, and which apps are in
 * it. Purely a toggle list for the apps — this is not how you open a hidden app (that's the
 * hidden drawer itself), so there is no tap-to-launch here on purpose.
 *
 * L'elenco si legge come **una carta**, non come righe a tutta larghezza: una riga che va da
 * bordo a bordo ha la forma di un elemento di menu, e qui invece è una lista di interruttori.
 * Come sia costruita davvero — riga per riga, per non perdere la pigrizia della lista — sta
 * scritto su `AppToggleRow`. Le nascoste stanno in cima, sotto la loro etichetta: sono la
 * risposta alla domanda per cui si apre questa schermata.
 */
@Composable
fun HiddenManagerScreen(
    state: LauncherUiState,
    onToggleHidden: (AppInfo) -> Unit,
    onSetPin: (String) -> Unit,
    onDisableLock: () -> Unit,
    onDone: () -> Unit
) {
    BackHandler(onBack = onDone)
    SecureScreen()
    val listState = rememberLazyListState()
    var showPinDialog by remember { mutableStateOf(false) }

    val hidden = remember(state.allApps, state.hiddenPackages) {
        state.allApps.filter { it.packageName in state.hiddenPackages }
    }
    val visible = remember(state.allApps, state.hiddenPackages) {
        state.allApps.filter { it.packageName !in state.hiddenPackages }
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
        BlurredWallpaperBackground(scrimAlpha = 0.88f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            DragHandle(onClose = onDone)
            Column(modifier = Modifier.padding(horizontal = HlScreenMargin)) {
                ScreenHeader(label = "App nascoste", actionText = "Chiudi", onAction = onDone)
            }

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                if (state.vaultUnavailable) {
                    item {
                        Text(
                            text = "L'archivio cifrato delle app nascoste non è leggibile su " +
                                "questo dispositivo: l'elenco risulta vuoto e le modifiche non " +
                                "vengono salvate. Di solito succede quando la chiave viene " +
                                "invalidata da un cambio del blocco schermo.",
                            color = HlPaper,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = HlScreenMargin, vertical = 12.dp)
                        )
                    }
                }

                item {
                    Text(
                        text = "Spariscono da home, cassetto e ricerca. Si aprono solo dal " +
                            "cassetto riservato.",
                        color = HlPaper.copy(alpha = 0.50f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = HlScreenMargin, vertical = 14.dp)
                    )
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = HlScreenMargin)
                            .fillMaxWidth()
                            .clip(HlCardShape)
                            .background(HlSurface)
                            .padding(start = 20.dp, end = 10.dp, top = 12.dp, bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Richiedi sblocco", color = HlPaper, fontSize = 15.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Impronta, con PIN di riserva.",
                                color = HlPaper55,
                                fontSize = 12.sp
                            )
                        }
                        MinimalSwitch(
                            checked = state.unlockRequired,
                            onCheckedChange = { enabled ->
                                if (enabled) showPinDialog = true else onDisableLock()
                            }
                        )
                    }
                }

                if (hidden.isNotEmpty()) {
                    item { GroupLabel("Nascoste · ${hidden.size}") }
                    itemsIndexed(
                        items = hidden,
                        key = { _, app -> "h-" + app.componentName.flattenToString() }
                    ) { index, app ->
                        AppToggleRow(
                            app = app,
                            checked = true,
                            first = index == 0,
                            last = index == hidden.lastIndex,
                            onToggle = { onToggleHidden(app) }
                        )
                    }
                }

                item { GroupLabel("Tutte le altre") }
                itemsIndexed(
                    items = visible,
                    key = { _, app -> "v-" + app.componentName.flattenToString() }
                ) { index, app ->
                    AppToggleRow(
                        app = app,
                        checked = false,
                        first = index == 0,
                        last = index == visible.lastIndex,
                        onToggle = { onToggleHidden(app) }
                    )
                }

                // In fondo, la versione installata. Serve a rispondere in un secondo alla
                // domanda che è già costata due giri di segnalazioni: "questo APK contiene
                // davvero la correzione di cui stiamo parlando?".
                item {
                    val context = LocalContext.current
                    val version = remember(context) {
                        runCatching {
                            context.packageManager
                                .getPackageInfo(context.packageName, 0)
                                .versionName
                        }.getOrNull() ?: "sconosciuta"
                    }
                    MonoValue(
                        text = "HIDDEN LAYER $version",
                        color = HlPaper55,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        modifier = Modifier.padding(HlScreenMargin)
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        PinSetupSheet(
            onConfirm = { pin ->
                showPinDialog = false
                onSetPin(pin)
            },
            onDismiss = { showPinDialog = false }
        )
    }
}

/** Una carta unica al posto di righe a tutta larghezza, che si leggerebbero come un menu di
 * sistema invece che come una lista di interruttori.
 *
 * Il fondo però lo disegna **ogni riga per conto suo**, arrotondando solo la prima e
 * l'ultima del gruppo: raccoglierle davvero dentro un solo contenitore vorrebbe dire
 * comporle tutte insieme dentro un unico `item`, cioè duecento righe con la loro icona
 * costruite in un colpo all'apertura della schermata. Il risultato a schermo è identico, e
 * la lista resta pigra come deve. */
@Composable
private fun GroupLabel(text: String) {
    SectionLabel(
        text = text,
        modifier = Modifier.padding(
            start = HlScreenMargin,
            end = HlScreenMargin,
            top = 24.dp,
            bottom = 10.dp
        )
    )
}

@Composable
private fun AppToggleRow(
    app: AppInfo,
    checked: Boolean,
    first: Boolean,
    last: Boolean,
    onToggle: () -> Unit
) {
    val shape = RoundedCornerShape(
        topStart = if (first) CARD_RADIUS else 0.dp,
        topEnd = if (first) CARD_RADIUS else 0.dp,
        bottomStart = if (last) CARD_RADIUS else 0.dp,
        bottomEnd = if (last) CARD_RADIUS else 0.dp
    )
    Column(
        modifier = Modifier
            .padding(horizontal = HlScreenMargin)
            .fillMaxWidth()
            .clip(shape)
            .background(HlSurface)
    ) {
        if (!first) Hairline(modifier = Modifier.padding(horizontal = 20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(start = 20.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)
        ) {
            // Qui l'icona resta, a differenza dell'elenco della Concentrazione: là si sceglie
            // cosa mettere in grigio e il nome basta, qui si decide cosa sparirà dalla home e
            // riconoscere l'icona giusta è metà del compito.
            AppIcon(app = app, size = 34.dp)
            Spacer(Modifier.width(14.dp))
            Text(
                text = app.label,
                color = if (checked) HlPaper else HlPaper.copy(alpha = 0.60f),
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            MinimalSwitch(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}

/**
 * Enabling the lock always sets a PIN: biometrics can stop working (new fingerprint, wet
 * hands, sensor failure) and there has to be a way back into your own apps.
 *
 * Era un `AlertDialog` di Material — superficie squadrata, pulsanti in maiuscoletto, due
 * `OutlinedTextField` con etichetta fluttuante: la finestra di sistema che tutto il resto del
 * launcher ha smesso di usare. Ora è lo stesso foglio in basso degli altri popup, con due
 * campi ridotti a una riga e un filetto. **La validazione non cambia**: minimo quattro cifre,
 * e i due PIN devono coincidere.
 */
@Composable
private fun PinSetupSheet(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    PromptSheet(onDismiss = onDismiss) {
        Text(
            text = "Imposta un PIN",
            color = PromptTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Serve come alternativa all'impronta, per non restare fuori dalle tue app.",
            color = PromptBody,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(22.dp))

        PinLine(value = pin, placeholder = "PIN, almeno 4 cifre") {
            pin = it
            error = null
        }
        Spacer(Modifier.height(18.dp))
        PinLine(value = confirmPin, placeholder = "Ripetilo") {
            confirmPin = it
            error = null
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            // Nessun rosso: il messaggio dice già cosa non va, e un colore d'allarme qui
            // sarebbe l'unico colore saturo di tutta l'interfaccia.
            Text(text = it, color = HlPaper, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryPill(
                text = "Conferma",
                onClick = {
                    when {
                        pin.length < 4 -> error = "Il PIN deve avere almeno 4 cifre"
                        pin != confirmPin -> error = "I PIN non coincidono"
                        else -> onConfirm(pin)
                    }
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(18.dp))
            TextAction(text = "Annulla", onClick = onDismiss, color = HlPaper42, fontSize = 14.sp)
        }
    }
}

/** Un campo PIN ridotto all'osso: testo, un filetto sotto, niente contenitore. */
@Composable
private fun PinLine(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= 8) onValueChange(it.filter(Char::isDigit)) },
            singleLine = true,
            textStyle = TextStyle(color = HlPaper, fontSize = 20.sp, letterSpacing = 6.sp),
            cursorBrush = SolidColor(HlPaper),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(modifier = Modifier.height(34.dp), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = HlPaper42, fontSize = 15.sp)
                    }
                    inner()
                }
            }
        )
        Hairline()
    }
}

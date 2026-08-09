package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.ui.AppIcon
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.DragHandle
import com.hiddenlayer.launcher.ui.SecureScreen
import com.hiddenlayer.launcher.ui.closeOnDragDown

/**
 * Settings screen for the vault: whether opening it needs an unlock, and which apps are in
 * it. Purely a toggle list for the apps — this is not how you open a hidden app (that's the
 * hidden drawer itself), so there is no tap-to-launch here on purpose.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                        title = { Text("Impostazioni app nascoste") },
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
                if (state.vaultUnavailable) {
                    item {
                        Text(
                            text = "L'archivio cifrato delle app nascoste non è leggibile su " +
                                "questo dispositivo: l'elenco risulta vuoto e le modifiche non " +
                                "vengono salvate. Di solito succede quando la chiave viene " +
                                "invalidata da un cambio del blocco schermo.",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    }
                }

                item {
                    ListItem(
                        headlineContent = { Text("Richiedi sblocco", color = Color.White) },
                        supportingContent = {
                            Text(
                                "Chiede impronta o PIN prima di aprire le app nascoste.",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = state.unlockRequired,
                                onCheckedChange = { enabled ->
                                    if (enabled) showPinDialog = true else onDisableLock()
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                }

                item {
                    Text(
                        "Scegli quali app nascondere: spariscono dalla home e dal cassetto, ricerca compresa, e restano raggiungibili solo da qui. Per aprirne una, chiudi questa schermata e usa il cassetto delle app nascoste.",
                        modifier = Modifier.padding(16.dp),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                items(state.allApps, key = { it.componentName.flattenToString() }) { app ->
                    val hidden = app.packageName in state.hiddenPackages
                    ListItem(
                        headlineContent = { Text(app.label, color = Color.White) },
                        leadingContent = { AppIcon(app = app, size = 36.dp) },
                        trailingContent = {
                            Switch(checked = hidden, onCheckedChange = { onToggleHidden(app) })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
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
                    Text(
                        text = "Hidden Layer $version",
                        color = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        PinSetupDialog(
            onConfirm = { pin ->
                showPinDialog = false
                onSetPin(pin)
            },
            onDismiss = { showPinDialog = false }
        )
    }
}

/** Enabling the lock always sets a PIN: biometrics can stop working (new fingerprint, wet
 * hands, sensor failure) and there has to be a way back into your own apps. */
@Composable
private fun PinSetupDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Imposta un PIN") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Serve come alternativa all'impronta, per non restare fuori dalle tue app.")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) pin = it.filter(Char::isDigit) },
                    label = { Text("PIN (min. 4 cifre)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.padding(top = 12.dp)
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 8) confirmPin = it.filter(Char::isDigit) },
                    label = { Text("Conferma PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.padding(top = 8.dp)
                )
                error?.let {
                    Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    pin.length < 4 -> error = "Il PIN deve avere almeno 4 cifre"
                    pin != confirmPin -> error = "I PIN non coincidono"
                    else -> onConfirm(pin)
                }
            }) { Text("Conferma") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

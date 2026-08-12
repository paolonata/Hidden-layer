package com.hiddenlayer.launcher.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.hiddenlayer.launcher.ContextMenuState
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.LauncherViewModel
import com.hiddenlayer.launcher.MenuOrigin
import com.hiddenlayer.launcher.Screen
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.data.FocusRepository
import com.hiddenlayer.launcher.data.HomeLayoutRepository
import com.hiddenlayer.launcher.ui.screens.DrawerScreen
import com.hiddenlayer.launcher.ui.screens.FocusScreen
import com.hiddenlayer.launcher.ui.screens.HiddenManagerScreen
import com.hiddenlayer.launcher.ui.screens.HomeScreen

/** Nesting depth of each screen, used purely to pick the slide direction: going to a
 * shallower screen plays as "closing" (slides down), going deeper plays as "opening"
 * (slides up) — e.g. HIDDEN_MANAGER is nested one level under the drawer. */
private fun screenDepth(screen: Screen): Int = when (screen) {
    Screen.HOME -> 0
    Screen.DRAWER, Screen.HIDDEN_DRAWER, Screen.FOCUS -> 1
    Screen.HIDDEN_MANAGER -> 2
}

@Composable
fun LauncherApp(
    viewModel: LauncherViewModel,
    canUseBiometrics: () -> Boolean,
    onRequestBiometric: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showEmptyPageMenu by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = state.screen,
        transitionSpec = {
            if (screenDepth(targetState) < screenDepth(initialState)) {
                (fadeIn() + slideInVertically { height -> -height / 6 }) togetherWith
                    (fadeOut() + slideOutVertically { height -> height })
            } else {
                (fadeIn() + slideInVertically { height -> height }) togetherWith
                    (fadeOut() + slideOutVertically { height -> -height / 6 })
            }
        },
        label = "screen-transition"
    ) { screen ->
        when (screen) {
            Screen.HOME -> HomeScreen(
                state = state,
                onAppTap = viewModel::launchApp,
                onAppLongPress = { app, origin, slot -> viewModel.showContextMenu(app, origin, slot) },
                onDockSlotPick = { slot -> viewModel.openDrawerForDockPick(slot) },
                onEmptyPageLongPress = { showEmptyPageMenu = true },
                onFocusShortcut = viewModel::focusShortcut,
                onOpenFocus = viewModel::openFocus,
                focusRemaining = viewModel.focusRemainingSeconds,
                onOpenDrawer = viewModel::openDrawer,
                onOpenHiddenDrawer = viewModel::openHiddenDrawer,
                onMoveAppToAdjacentPage = viewModel::moveToAdjacentPage,
                onDropOnDock = { app, slot ->
                    if (!viewModel.dropOnDock(app, slot)) {
                        Toast.makeText(
                            context,
                            "Dock pieno (massimo ${HomeLayoutRepository.DOCK_SIZE} app).",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onDropOnHome = viewModel::addToHome,
                onPageSizeChanged = viewModel::setPageSize
            )

            Screen.DRAWER, Screen.HIDDEN_DRAWER -> DrawerScreen(
                state = state,
                // Dal valore dell'enum, non da un campo dello stato: è ciò che rende le due
                // versioni due voci separate per AnimatedContent, e quindi due composizioni.
                hiddenDrawer = screen == Screen.HIDDEN_DRAWER,
                canUseBiometrics = canUseBiometrics,
                onRequestBiometric = onRequestBiometric,
                onQueryChange = viewModel::onQueryChange,
                onAppClick = viewModel::onDrawerAppClick,
                onAppLongPress = { app -> viewModel.showContextMenu(app, MenuOrigin.DRAWER) },
                onHiddenAppClick = viewModel::launchApp,
                onHiddenAppLongPress = { app -> viewModel.showContextMenu(app, MenuOrigin.HIDDEN_DRAWER) },
                onPinSubmit = viewModel::verifyPin,
                onClearUnlockError = viewModel::clearUnlockError,
                onOpenSettings = viewModel::openHiddenSettings,
                onRelock = viewModel::relockVault,
                onClose = viewModel::backToHome
            )

            Screen.FOCUS -> FocusScreen(
                state = state,
                focusRemaining = viewModel.focusRemainingSeconds,
                onSetDuration = viewModel::setFocusDuration,
                onToggleApp = viewModel::toggleFocusApp,
                onStart = viewModel::startFocus,
                onStop = viewModel::stopFocus,
                onResetStats = viewModel::resetFocusStats,
                onDone = viewModel::backToHome
            )

            Screen.HIDDEN_MANAGER -> HiddenManagerScreen(
                state = state,
                onToggleHidden = viewModel::toggleHidden,
                onSetPin = viewModel::setVaultPin,
                onDisableLock = viewModel::disableVaultLock,
                onDone = viewModel::backToHiddenDrawer
            )
        }
    }

    if (state.focusPickerVisible) {
        FocusDurationPrompt(
            options = FocusRepository.SHORTCUT_MINUTES,
            onPick = viewModel::pickFocusDuration,
            onOpenSettings = viewModel::openFocusFromPicker,
            onDismiss = viewModel::dismissFocusPicker
        )
    }

    state.frictionApp?.let { app ->
        FrictionPrompt(
            app = app,
            streakSeconds = state.frictionStreakSeconds,
            recordSeconds = state.focusRecordSeconds,
            onOpenAnyway = { viewModel.launchAnyway(app) },
            onDismiss = viewModel::dismissFriction
        )
    }

    state.contextMenu?.let { menu ->
        AppContextMenu(
            title = menu.app.label,
            actions = buildContextMenuActions(menu, state, viewModel) { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
            onDismiss = viewModel::dismissContextMenu
        )
    }

    if (showEmptyPageMenu) {
        AppContextMenu(
            title = "Home",
            actions = listOf(
                MenuAction("Aggiungi app") {
                    showEmptyPageMenu = false
                    viewModel.openDrawerForHomePick()
                },
                MenuAction("Cambia sfondo") {
                    showEmptyPageMenu = false
                    if (!viewModel.openWallpaperPicker()) {
                        Toast.makeText(
                            context,
                            "Nessun selettore di sfondo disponibile su questo sistema.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                MenuAction(
                    if (state.focusActive) "Concentrazione (in corso)" else "Concentrazione"
                ) {
                    showEmptyPageMenu = false
                    viewModel.openFocus()
                }
            ),
            onDismiss = { showEmptyPageMenu = false }
        )
    }

    // È una finestra a sé (vedi UnlockPauseOverlay), quindi copre tutto senza doversi
    // preoccupare di dove sta nella pila di questa composizione. Niente AnimatedVisibility
    // attorno: l'apertura è la macchia stessa che si allarga, e l'uscita arriva quando il
    // cerchio ha già scoperto tutto lo schermo — una dissolvenza in più si vedrebbe come un
    // secondo sfarfallio dopo che l'animazione è finita.
    if (state.unlockPauseActive) {
        UnlockPauseOverlay(durationMillis = state.unlockPauseMillis)
    }
}

private fun buildContextMenuActions(
    menu: ContextMenuState,
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    showToast: (String) -> Unit
): List<MenuAction> {
    val app: AppInfo = menu.app
    val actions = mutableListOf<MenuAction>()

    actions += MenuAction("Apri") {
        viewModel.launchApp(app)
        viewModel.dismissContextMenu()
    }

    when (menu.origin) {
        MenuOrigin.HOME -> {
            actions += MenuAction("Rimuovi dalla home") { viewModel.removeFromHome(app) }
            actions += MenuAction("Aggiungi al dock") {
                val added = viewModel.addToDock(app)
                if (!added) {
                    showToast("Dock pieno (massimo ${HomeLayoutRepository.DOCK_SIZE} app). Rimuovine una dal dock per aggiungerne un'altra.")
                }
            }

            val currentPageIndex = state.homePages.indexOfFirst { page ->
                page.any { it.componentName == app.componentName }
            }
            if (currentPageIndex > 0) {
                actions += MenuAction("Sposta a pagina precedente") { viewModel.moveToAdjacentPage(app, -1) }
            }
            // Solo se una pagina successiva esiste davvero: il modello dei dati non ha buchi,
            // quindi non si può spostare un'icona su una pagina che non c'è ancora.
            if (currentPageIndex in 0 until state.homePages.size - 1) {
                actions += MenuAction("Sposta a pagina successiva") { viewModel.moveToAdjacentPage(app, +1) }
            }
            actions += MenuAction("Nascondi app") { viewModel.hideApp(app) }
        }
        MenuOrigin.DOCK -> {
            actions += MenuAction("Rimuovi dal dock") { viewModel.removeFromHome(app) }
            actions += MenuAction("Sostituisci") {
                viewModel.dismissContextMenu()
                viewModel.openDrawerForDockPick(menu.dockSlot)
            }
            actions += MenuAction("Nascondi app") { viewModel.hideApp(app) }
        }
        MenuOrigin.DRAWER -> {
            actions += MenuAction("Aggiungi alla home") { viewModel.addToHome(app) }
            // Prima l'unica strada per il dock era passare dalla home: aggiungerla lì, tenerla
            // premuta di nuovo, "Aggiungi al dock". Due passaggi per un'azione sola.
            actions += MenuAction("Aggiungi al dock") {
                if (!viewModel.addToDock(app)) {
                    showToast("Dock pieno (massimo ${HomeLayoutRepository.DOCK_SIZE} app). Rimuovine una dal dock per aggiungerne un'altra.")
                }
            }
            actions += MenuAction("Nascondi app") { viewModel.hideApp(app) }
        }
        MenuOrigin.HIDDEN_DRAWER -> {
            actions += MenuAction("Mostra app") { viewModel.toggleHidden(app) }
            // Prima l'unica strada per bloccare un'app nascosta durante la Concentrazione era
            // mostrarla, andare nella schermata Concentrazione a selezionarla, e nasconderla di
            // nuovo. La schermata Concentrazione esclude le app nascoste dal suo elenco per la
            // regola sulla privacy (il loro nome non deve comparire in una schermata che si
            // apre senza sblocco), ma qui — che è già dietro lo sblocco — l'azione può stare
            // senza violare niente: il nome dell'app non esce mai da questa schermata protetta.
            val blocked = app.packageName in state.focusPackages
            actions += MenuAction(
                if (blocked) "Non bloccare durante la Concentrazione" else "Blocca durante la Concentrazione"
            ) { viewModel.toggleFocusApp(app) }
        }
    }

    actions += MenuAction("Info app") { viewModel.openAppInfo(app) }
    actions += MenuAction("Disinstalla", destructive = true) {
        if (!viewModel.requestUninstall(app)) {
            showToast("Il sistema non ha aperto la disinstallazione. Apro le info dell'app: il pulsante \"Disinstalla\" è lì.")
        }
    }
    return actions
}

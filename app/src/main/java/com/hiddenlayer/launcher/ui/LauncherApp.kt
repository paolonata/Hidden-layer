package com.hiddenlayer.launcher.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.hiddenlayer.launcher.ContextMenuState
import com.hiddenlayer.launcher.LauncherViewModel
import com.hiddenlayer.launcher.MenuOrigin
import com.hiddenlayer.launcher.Screen
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.ui.screens.DrawerScreen
import com.hiddenlayer.launcher.ui.screens.HiddenManagerScreen
import com.hiddenlayer.launcher.ui.screens.HomeScreen
import com.hiddenlayer.launcher.ui.screens.PinPromptScreen
import com.hiddenlayer.launcher.ui.screens.PinSetupScreen

@Composable
fun LauncherApp(
    viewModel: LauncherViewModel,
    canUseBiometrics: () -> Boolean,
    onRequestBiometric: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showEmptyPageMenu by remember { mutableStateOf(false) }

    when (state.screen) {
        Screen.HOME -> HomeScreen(
            state = state,
            onAppTap = viewModel::launchApp,
            onAppLongPress = { app, origin, slot -> viewModel.showContextMenu(app, origin, slot) },
            onDockSlotLongPress = { slot -> viewModel.openDrawerForDockPick(slot) },
            onEmptyPageLongPress = { showEmptyPageMenu = true },
            onOpenDrawer = viewModel::openDrawer
        )

        Screen.DRAWER -> DrawerScreen(
            state = state,
            onQueryChange = viewModel::onQueryChange,
            onAppClick = viewModel::onDrawerAppClick,
            onAppLongPress = { app -> viewModel.showContextMenu(app, MenuOrigin.DRAWER) },
            onOpenHiddenManager = viewModel::requestHiddenSection,
            onClose = viewModel::backToHome
        )

        Screen.PIN_SETUP -> PinSetupScreen(
            onPinConfirmed = viewModel::setPin,
            onCancel = viewModel::backToHome
        )

        Screen.PIN_PROMPT -> PinPromptScreen(
            error = state.pinError,
            canUseBiometrics = canUseBiometrics(),
            onPinSubmit = viewModel::verifyPin,
            onBiometricRequest = onRequestBiometric,
            onCancel = viewModel::backToHome
        )

        Screen.HIDDEN_MANAGER -> HiddenManagerScreen(
            state = state,
            onAppClick = viewModel::launchApp,
            onToggleHidden = viewModel::toggleHidden,
            onDone = viewModel::backToHome
        )
    }

    state.contextMenu?.let { menu ->
        AppContextMenu(
            title = menu.app.label,
            actions = buildContextMenuActions(menu, viewModel),
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
                    context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
                },
                MenuAction("App nascoste") {
                    showEmptyPageMenu = false
                    viewModel.requestHiddenSection()
                }
            ),
            onDismiss = { showEmptyPageMenu = false }
        )
    }
}

private fun buildContextMenuActions(menu: ContextMenuState, viewModel: LauncherViewModel): List<MenuAction> {
    val app: AppInfo = menu.app
    val actions = mutableListOf<MenuAction>()

    actions += MenuAction("Apri") {
        viewModel.launchApp(app)
        viewModel.dismissContextMenu()
    }

    when (menu.origin) {
        MenuOrigin.HOME -> {
            actions += MenuAction("Rimuovi dalla home") { viewModel.removeFromHome(app) }
            actions += MenuAction("Aggiungi al dock") { viewModel.addToDock(app) }
        }
        MenuOrigin.DOCK -> {
            actions += MenuAction("Rimuovi dal dock") { viewModel.removeFromHome(app) }
            actions += MenuAction("Sostituisci") {
                viewModel.dismissContextMenu()
                viewModel.openDrawerForDockPick(menu.dockSlot)
            }
        }
        MenuOrigin.DRAWER -> {
            actions += MenuAction("Aggiungi alla home") { viewModel.addToHome(app) }
        }
    }

    actions += MenuAction("Info app") { viewModel.openAppInfo(app) }
    actions += MenuAction("Nascondi app") { viewModel.hideApp(app) }
    actions += MenuAction("Disinstalla", destructive = true) { viewModel.requestUninstall(app) }
    return actions
}

package com.hiddenlayer.launcher.ui

import android.content.Intent
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
import com.hiddenlayer.launcher.data.HomeLayoutRepository
import com.hiddenlayer.launcher.ui.screens.DrawerScreen
import com.hiddenlayer.launcher.ui.screens.HiddenManagerScreen
import com.hiddenlayer.launcher.ui.screens.HomeScreen

@Composable
fun LauncherApp(viewModel: LauncherViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showEmptyPageMenu by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = state.screen,
        transitionSpec = {
            if (targetState == Screen.HOME) {
                // Closing an overlay: it slides back down, home settles back in underneath.
                (fadeIn() + slideInVertically { height -> -height / 6 }) togetherWith
                    (fadeOut() + slideOutVertically { height -> height })
            } else {
                // Opening drawer/hidden-apps: it slides up over the home screen.
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
                onDockSlotLongPress = { slot -> viewModel.openDrawerForDockPick(slot) },
                onEmptyPageLongPress = { showEmptyPageMenu = true },
                onOpenDrawer = viewModel::openDrawer
            )

            Screen.DRAWER -> DrawerScreen(
                state = state,
                onQueryChange = viewModel::onQueryChange,
                onAppClick = viewModel::onDrawerAppClick,
                onAppLongPress = { app -> viewModel.showContextMenu(app, MenuOrigin.DRAWER) },
                onOpenHiddenManager = viewModel::openHiddenManager,
                onClose = viewModel::backToHome
            )

            Screen.HIDDEN_MANAGER -> HiddenManagerScreen(
                state = state,
                onAppClick = viewModel::launchApp,
                onToggleHidden = viewModel::toggleHidden,
                onDone = viewModel::backToHome
            )
        }
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
                    context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
                },
                MenuAction("App nascoste") {
                    showEmptyPageMenu = false
                    viewModel.openHiddenManager()
                }
            ),
            onDismiss = { showEmptyPageMenu = false }
        )
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
            actions += MenuAction("Sposta a pagina successiva") { viewModel.moveToAdjacentPage(app, +1) }
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

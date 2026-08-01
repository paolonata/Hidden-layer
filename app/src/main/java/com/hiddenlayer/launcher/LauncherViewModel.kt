package com.hiddenlayer.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.data.AppRepository
import com.hiddenlayer.launcher.data.HiddenAppsRepository
import com.hiddenlayer.launcher.data.HomeLayoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val hiddenAppsRepository = HiddenAppsRepository(application)
    private val homeLayoutRepository = HomeLayoutRepository(application)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    init {
        refreshApps()
    }

    /** Re-reads installed apps, drops uninstalled/hidden components from the layout, and
     * seeds a sensible default layout (dock + home, in alphabetical order) on first run. */
    fun refreshApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { appRepository.loadLaunchableApps() }
            val hidden = hiddenAppsRepository.getHiddenPackages()
            val validComponents = apps
                .filter { it.packageName !in hidden }
                .map { it.componentName }
                .toSet()

            homeLayoutRepository.removeInvalid(validComponents)

            var home = homeLayoutRepository.getHomeItems()
            var dock = homeLayoutRepository.getDock()

            if (home.isEmpty() && dock.isEmpty() && apps.isNotEmpty()) {
                val visible = apps.filter { it.packageName !in hidden }
                dock = visible.take(HomeLayoutRepository.DOCK_SIZE).map { it.componentName }
                home = visible.drop(HomeLayoutRepository.DOCK_SIZE).map { it.componentName }
                homeLayoutRepository.setDock(dock)
                homeLayoutRepository.setHomeItems(home)
            }

            _uiState.value = _uiState.value.copy(
                allApps = apps,
                hiddenPackages = hidden,
                homeComponents = home,
                dockComponents = dock,
                loaded = true
            )
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun launchApp(app: AppInfo) {
        appRepository.launch(app.componentName)
    }

    fun openDrawer() {
        _uiState.value = _uiState.value.copy(screen = Screen.DRAWER, drawerMode = DrawerMode.BROWSE, query = "")
    }

    fun openDrawerForHomePick() {
        _uiState.value = _uiState.value.copy(screen = Screen.DRAWER, drawerMode = DrawerMode.PICK_FOR_HOME, query = "")
    }

    fun openDrawerForDockPick(slot: Int) {
        _uiState.value = _uiState.value.copy(
            screen = Screen.DRAWER,
            drawerMode = DrawerMode.PICK_FOR_DOCK,
            pendingDockSlot = slot,
            query = ""
        )
    }

    fun onDrawerAppClick(app: AppInfo) {
        when (_uiState.value.drawerMode) {
            DrawerMode.BROWSE -> {
                launchApp(app)
                backToHome()
            }
            DrawerMode.PICK_FOR_HOME -> {
                homeLayoutRepository.addToHome(app.componentName)
                syncLayout()
                backToHome()
            }
            DrawerMode.PICK_FOR_DOCK -> {
                homeLayoutRepository.setDockSlot(_uiState.value.pendingDockSlot, app.componentName)
                syncLayout()
                backToHome()
            }
        }
    }

    fun backToHome() {
        _uiState.value = _uiState.value.copy(
            screen = Screen.HOME,
            drawerMode = DrawerMode.BROWSE,
            pendingDockSlot = -1,
            query = "",
            contextMenu = null
        )
    }

    /** Back from the nested "manage hidden apps" settings screen to the hidden drawer
     * it was opened from (rather than all the way home). */
    fun backToHiddenDrawer() {
        _uiState.value = _uiState.value.copy(
            screen = Screen.HIDDEN_DRAWER,
            query = "",
            contextMenu = null
        )
    }

    private fun syncLayout() {
        _uiState.value = _uiState.value.copy(
            homeComponents = homeLayoutRepository.getHomeItems(),
            dockComponents = homeLayoutRepository.getDock()
        )
    }

    fun showContextMenu(app: AppInfo, origin: MenuOrigin, dockSlot: Int = -1) {
        _uiState.value = _uiState.value.copy(contextMenu = ContextMenuState(app, origin, dockSlot))
    }

    fun dismissContextMenu() {
        _uiState.value = _uiState.value.copy(contextMenu = null)
    }

    fun removeFromHome(app: AppInfo) {
        homeLayoutRepository.removeFromHome(app.componentName)
        syncLayout()
        dismissContextMenu()
    }

    /** Returns false when the dock is already full (see HomeLayoutRepository.DOCK_SIZE),
     * so the caller can tell the user why nothing happened instead of failing silently. */
    fun addToDock(app: AppInfo): Boolean {
        val added = homeLayoutRepository.addToDock(app.componentName)
        syncLayout()
        dismissContextMenu()
        return added
    }

    fun addToHome(app: AppInfo) {
        homeLayoutRepository.addToHome(app.componentName)
        syncLayout()
        dismissContextMenu()
    }

    /** Moves an app to the previous/next home page (direction -1/+1). Moving past the
     * last page simply creates a new one. */
    fun moveToAdjacentPage(app: AppInfo, direction: Int) {
        val pages = _uiState.value.homePages
        val currentPageIndex = pages.indexOfFirst { page -> page.any { it.componentName == app.componentName } }
        if (currentPageIndex == -1) return
        val targetPageIndex = (currentPageIndex + direction).coerceAtLeast(0)
        homeLayoutRepository.moveToPage(app.componentName, targetPageIndex)
        syncLayout()
        dismissContextMenu()
    }

    fun hideApp(app: AppInfo) {
        hiddenAppsRepository.setHidden(app.packageName, true)
        homeLayoutRepository.removeFromHome(app.componentName)
        _uiState.value = _uiState.value.copy(
            hiddenPackages = hiddenAppsRepository.getHiddenPackages(),
            homeComponents = homeLayoutRepository.getHomeItems(),
            dockComponents = homeLayoutRepository.getDock()
        )
        dismissContextMenu()
    }

    fun openAppInfo(app: AppInfo) {
        appRepository.openAppInfo(app.packageName)
        dismissContextMenu()
    }

    fun requestUninstall(app: AppInfo) {
        appRepository.requestUninstall(app.packageName)
        dismissContextMenu()
    }

    /** Opens the incognito-style drawer that shows only hidden apps — this is the normal
     * way to actually open one, not the toggle list. */
    fun openHiddenDrawer() {
        _uiState.value = _uiState.value.copy(screen = Screen.HIDDEN_DRAWER, query = "")
    }

    /** Opens the toggle list to choose which apps are hidden — reached from within the
     * hidden drawer (like a settings screen), not a direct way to launch anything. */
    fun openHiddenSettings() {
        _uiState.value = _uiState.value.copy(screen = Screen.HIDDEN_MANAGER)
    }

    fun toggleHidden(app: AppInfo) {
        val nowHidden = app.packageName !in _uiState.value.hiddenPackages
        hiddenAppsRepository.setHidden(app.packageName, nowHidden)
        if (nowHidden) homeLayoutRepository.removeFromHome(app.componentName)
        _uiState.value = _uiState.value.copy(
            hiddenPackages = hiddenAppsRepository.getHiddenPackages(),
            homeComponents = homeLayoutRepository.getHomeItems(),
            dockComponents = homeLayoutRepository.getDock()
        )
    }
}

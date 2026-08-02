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
                // Seed only the always-visible dock row; the hidden second row starts empty.
                val visible = apps.filter { it.packageName !in hidden }
                dock = visible.take(HomeLayoutRepository.DOCK_COLUMNS).map { it.componentName }
                home = visible.drop(HomeLayoutRepository.DOCK_COLUMNS).map { it.componentName }
                homeLayoutRepository.setDock(dock)
                homeLayoutRepository.setHomeItems(home)
            }

            _uiState.value = _uiState.value.copy(
                allApps = apps,
                hiddenPackages = hidden,
                homeComponents = home,
                dockComponents = dock,
                unlockRequired = hiddenAppsRepository.isUnlockRequired(),
                loaded = true
            )
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    /** Reported by HomeScreen once it has measured how many icon rows actually fit on the
     * screen, so a page holds as many as it physically can instead of a fixed guess. */
    fun setPageSize(pageSize: Int) {
        if (pageSize > 0 && pageSize != _uiState.value.pageSize) {
            _uiState.value = _uiState.value.copy(pageSize = pageSize)
        }
    }

    fun launchApp(app: AppInfo) {
        appRepository.launch(app.componentName)
    }

    fun openDrawer() {
        _uiState.value = _uiState.value.copy(
            screen = Screen.DRAWER,
            drawerMode = DrawerMode.BROWSE,
            drawerStartPage = 0,
            query = ""
        )
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

    /** Back from the nested "manage hidden apps" settings screen to the drawer page it was
     * opened from (the hidden one), rather than all the way home. */
    fun backToHiddenDrawer() {
        _uiState.value = _uiState.value.copy(
            screen = Screen.DRAWER,
            drawerMode = DrawerMode.BROWSE,
            drawerStartPage = 1,
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

    /** Dropping a dragged home icon onto the dock. Returns false when the dock is full so
     * the caller can say so instead of the drop silently doing nothing. */
    fun dropOnDock(app: AppInfo, slot: Int): Boolean {
        val placed = homeLayoutRepository.insertIntoDock(app.componentName, slot)
        syncLayout()
        return placed
    }

    fun addToHome(app: AppInfo) {
        homeLayoutRepository.addToHome(app.componentName)
        syncLayout()
        dismissContextMenu()
    }

    /** Moves an app to the previous/next home page (direction -1/+1). Moving past the
     * last page simply creates a new one. */
    fun moveToAdjacentPage(app: AppInfo, direction: Int) {
        val currentState = _uiState.value
        val pages = currentState.homePages
        val currentPageIndex = pages.indexOfFirst { page -> page.any { it.componentName == app.componentName } }
        if (currentPageIndex == -1) return
        val targetPageIndex = (currentPageIndex + direction).coerceAtLeast(0)
        homeLayoutRepository.moveToPage(app.componentName, targetPageIndex, currentState.pageSize)
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

    fun onUnlockSucceeded() {
        _uiState.value = _uiState.value.copy(vaultUnlocked = true, unlockError = false)
    }

    fun verifyPin(pin: String) {
        if (hiddenAppsRepository.verifyPin(pin)) {
            onUnlockSucceeded()
        } else {
            _uiState.value = _uiState.value.copy(unlockError = true)
        }
    }

    /** Called when the launcher stops (app opened, screen off, task switched): the hidden
     * page re-locks itself, so it is never left unlocked behind your back. */
    fun lockVault() {
        val current = _uiState.value
        _uiState.value = current.copy(
            vaultUnlocked = false,
            unlockError = false,
            screen = if (current.screen == Screen.HIDDEN_MANAGER) Screen.HOME else current.screen,
            drawerStartPage = 0
        )
    }

    /** Opens the toggle list to choose which apps are hidden — reached from the gear on the
     * hidden page, not a direct way to launch anything. */
    fun openHiddenSettings() {
        _uiState.value = _uiState.value.copy(screen = Screen.HIDDEN_MANAGER)
    }

    fun setVaultPin(pin: String) {
        hiddenAppsRepository.setPin(pin)
        _uiState.value = _uiState.value.copy(unlockRequired = true)
    }

    fun disableVaultLock() {
        hiddenAppsRepository.disableUnlock()
        _uiState.value = _uiState.value.copy(unlockRequired = false)
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

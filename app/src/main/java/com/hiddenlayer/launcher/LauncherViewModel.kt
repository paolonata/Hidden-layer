package com.hiddenlayer.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.data.AppRepository
import com.hiddenlayer.launcher.data.FocusRepository
import com.hiddenlayer.launcher.data.FocusStatsRepository
import com.hiddenlayer.launcher.data.HiddenAppsRepository
import com.hiddenlayer.launcher.data.HomeLayoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Quanto resta accesa la pill di conferma dopo l'avvio di una sessione. */
private const val FOCUS_TOAST_MILLIS = 5_000L

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val hiddenAppsRepository = HiddenAppsRepository(application)
    private val homeLayoutRepository = HomeLayoutRepository(application)
    private val focusRepository = FocusRepository(application)
    private val focusStatsRepository = FocusStatsRepository(application)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    /**
      * I secondi che mancano alla fine della sessione, tenuti fuori da uiState di proposito.
      *
      * Dentro uiState il countdown emetterebbe un nuovo stato al secondo, e siccome
      * LauncherApp lo raccoglie alla radice ogni tick ricomporrebbe tutta l'app: nel cassetto
      * vorrebbe dire rifiltrare l'elenco completo delle app installate e rieseguire la
      * griglia, una volta al secondo, per tutta la sessione. Qui lo raccoglie solo chi lo
      * mostra davvero — la pill e la schermata Concentrazione.
      */
    private val _focusRemainingSeconds = MutableStateFlow(0)
    val focusRemainingSeconds: StateFlow<Int> = _focusRemainingSeconds.asStateFlow()

    private var focusTicker: Job? = null
    private var focusToastJob: Job? = null

    init {
        refreshApps()
        restoreFocusSession()
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
            var dock = homeLayoutRepository.getDockSlots()

            if (home.isEmpty() && dock.all { it == null } && apps.isNotEmpty()) {
                // Seed only the always-visible dock row; the fold-out row starts empty.
                val visible = apps.filter { it.packageName !in hidden }
                dock = List(HomeLayoutRepository.DOCK_SIZE) { index ->
                    if (index < HomeLayoutRepository.DOCK_COLUMNS) {
                        visible.getOrNull(index)?.componentName
                    } else {
                        null
                    }
                }
                home = visible.drop(HomeLayoutRepository.DOCK_COLUMNS).map { it.componentName }
                homeLayoutRepository.setDockSlots(dock)
                homeLayoutRepository.setHomeItems(home)
            }

            _uiState.value = _uiState.value.copy(
                allApps = apps,
                hiddenPackages = hidden,
                homeComponents = home,
                dockComponents = dock,
                unlockRequired = hiddenAppsRepository.isUnlockRequired(),
                focusPackages = focusRepository.getDistractingPackages(),
                focusDurationMinutes = focusRepository.getDurationMinutes(),
                focusRecordSeconds = focusStatsRepository.getRecordSeconds(),
                focusBreaks = focusStatsRepository.getBreaks(),
                focusSessionCount = focusStatsRepository.getSessionCount(),
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

    /** Every launch goes through here, so a muted app can be intercepted wherever it is
     * tapped from — home, dock or drawer — rather than only in one of them. */
    fun launchApp(app: AppInfo) {
        if (_uiState.value.isMuted(app)) {
            _uiState.value = _uiState.value.copy(
                frictionApp = app,
                // Fotografata adesso: il popup poi resta fermo su questo numero.
                frictionStreakSeconds = focusStatsRepository.currentStreakSeconds(System.currentTimeMillis())
            )
        } else {
            appRepository.launch(app.componentName)
        }
    }

    /** Chosen deliberately from the confirmation prompt: bypasses the check. */
    fun launchAnyway(app: AppInfo) {
        // Il cedimento chiude il tratto di resistenza e fa ripartire il conto. Le app
        // nascoste restano fuori: il loro nome comparirebbe nella classifica dentro le
        // impostazioni della Concentrazione, che non sono protette.
        if (app.packageName !in _uiState.value.hiddenPackages) {
            focusStatsRepository.onBreak(app.packageName, System.currentTimeMillis())
        }
        _uiState.value = _uiState.value.copy(frictionApp = null)
        syncFocusStats()
        appRepository.launch(app.componentName)
    }

    private fun syncFocusStats() {
        _uiState.value = _uiState.value.copy(
            focusRecordSeconds = focusStatsRepository.getRecordSeconds(),
            focusBreaks = focusStatsRepository.getBreaks(),
            focusSessionCount = focusStatsRepository.getSessionCount()
        )
    }

    fun resetFocusStats() {
        focusStatsRepository.reset()
        syncFocusStats()
    }

    fun dismissFriction() {
        _uiState.value = _uiState.value.copy(frictionApp = null)
    }

    // --- Focus sessions -----------------------------------------------------------------

    fun openFocus() {
        _uiState.value = _uiState.value.copy(screen = Screen.FOCUS)
    }

    fun setFocusDuration(minutes: Int) {
        val clamped = minutes.coerceIn(FocusRepository.MIN_MINUTES, FocusRepository.MAX_MINUTES)
        focusRepository.setDurationMinutes(clamped)
        _uiState.value = _uiState.value.copy(focusDurationMinutes = clamped)
    }

    fun toggleFocusApp(app: AppInfo) {
        val muted = app.packageName !in _uiState.value.focusPackages
        focusRepository.setDistracting(app.packageName, muted)
        _uiState.value = _uiState.value.copy(focusPackages = focusRepository.getDistractingPackages())
    }

    /**
     * Il doppio tap sulla home: chiede solo per quanto, poi parte. Avviare una sessione deve
     * costare un gesto e una scelta, non quattro passaggi di menu — è proprio quando ne hai
     * bisogno che hai meno voglia di cercarla.
     *
     * Non è un interruttore: a sessione in corso porta alla schermata Concentrazione invece
     * di terminarla, così un doppio tap involontario non può buttare via il lavoro fatto.
     * Stessa cosa se non hai ancora scelto nessuna app da mettere in grigio, perché una
     * sessione a mani vuote non farebbe nulla.
     */
    fun focusShortcut() {
        val current = _uiState.value
        if (current.focusActive || current.focusPackages.isEmpty()) {
            openFocus()
        } else {
            _uiState.value = current.copy(focusPickerVisible = true)
        }
    }

    /** Una delle tre durate del doppio tap. Viene anche salvata come durata corrente, così la
     * schermata Concentrazione resta allineata a quello che hai appena scelto. */
    fun pickFocusDuration(minutes: Int) {
        _uiState.value = _uiState.value.copy(focusPickerVisible = false)
        setFocusDuration(minutes)
        startFocus()
    }

    fun dismissFocusPicker() {
        _uiState.value = _uiState.value.copy(focusPickerVisible = false)
    }

    /** "Altra durata…" dal popup: le tre scorciatoie coprono i casi normali, il resto si
     * regola dove ci sono i minuti al dettaglio. */
    fun openFocusFromPicker() {
        _uiState.value = _uiState.value.copy(focusPickerVisible = false, screen = Screen.FOCUS)
    }

    fun startFocus() {
        val endsAt = System.currentTimeMillis() + _uiState.value.focusDurationMinutes * 60_000L
        focusRepository.setSessionEndsAt(endsAt)
        focusStatsRepository.onSessionStarted(System.currentTimeMillis())
        startTicker(endsAt)
        showFocusToast()
        syncFocusStats()
    }

    fun stopFocus() {
        focusRepository.setSessionEndsAt(0L)
        focusTicker?.cancel()
        endSession()
    }

    /** Chiusura di una sessione, per scadenza o per scelta: un solo aggiornamento di stato. */
    private fun endSession(endedAtMillis: Long = System.currentTimeMillis()) {
        focusStatsRepository.onSessionEnded(endedAtMillis)
        focusTicker = null
        focusToastJob?.cancel()
        focusToastJob = null
        _focusRemainingSeconds.value = 0
        _uiState.value = _uiState.value.copy(
            focusActive = false,
            focusToastVisible = false,
            frictionApp = null,
            focusRecordSeconds = focusStatsRepository.getRecordSeconds(),
            focusBreaks = focusStatsRepository.getBreaks(),
            focusSessionCount = focusStatsRepository.getSessionCount()
        )
    }

    /** La pill col countdown appare all'avvio e si spegne da sola: conferma che la sessione è
     * partita e per quanto, non sta fissa in home. */
    private fun showFocusToast() {
        focusToastJob?.cancel()
        _uiState.value = _uiState.value.copy(focusToastVisible = true)
        focusToastJob = viewModelScope.launch {
            delay(FOCUS_TOAST_MILLIS)
            _uiState.value = _uiState.value.copy(focusToastVisible = false)
        }
    }

    /** A session is a wall-clock deadline, so it keeps running across restarts instead of
     * being cancelled by the launcher being killed. */
    private fun restoreFocusSession() {
        val endsAt = focusRepository.getSessionEndsAt()
        if (endsAt > System.currentTimeMillis()) {
            startTicker(endsAt)
        } else {
            // Scaduta mentre il launcher era chiuso: va chiusa comunque, col suo orario di
            // fine. Altrimenti proprio le sessioni portate a termine senza mai cedere — le
            // migliori — non arriverebbero mai al record.
            if (endsAt > 0L) focusStatsRepository.onSessionEnded(endsAt)
            focusRepository.setSessionEndsAt(0L)
        }
    }

    private fun startTicker(endsAt: Long) {
        focusTicker?.cancel()
        // Subito, non al primo tick: la pill di conferma nasce già col tempo giusto.
        _focusRemainingSeconds.value = ((endsAt - System.currentTimeMillis()) / 1000L)
            .toInt()
            .coerceAtLeast(0)
        _uiState.value = _uiState.value.copy(focusActive = true)

        focusTicker = viewModelScope.launch {
            while (true) {
                val remaining = ((endsAt - System.currentTimeMillis()) / 1000L).toInt()
                if (remaining <= 0) {
                    focusRepository.setSessionEndsAt(0L)
                    endSession()
                    break
                }
                // Solo questo flow: uiState resta fermo per tutta la sessione.
                _focusRemainingSeconds.value = remaining
                delay(1000L)
            }
        }
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
            dockComponents = homeLayoutRepository.getDockSlots()
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
        val placed = homeLayoutRepository.placeInDock(app.componentName, slot)
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
            dockComponents = homeLayoutRepository.getDockSlots()
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
            dockComponents = homeLayoutRepository.getDockSlots()
        )
    }
}

package com.hiddenlayer.launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.data.AppRepository
import com.hiddenlayer.launcher.data.DimRepository
import com.hiddenlayer.launcher.data.DumbRepository
import com.hiddenlayer.launcher.data.FocusRepository
import com.hiddenlayer.launcher.data.FocusStatsRepository
import com.hiddenlayer.launcher.data.HiddenAppsRepository
import com.hiddenlayer.launcher.data.HomeLayoutRepository
import com.hiddenlayer.launcher.dim.ScreenDimService
import com.hiddenlayer.launcher.dim.SystemDim
import com.hiddenlayer.launcher.dumb.SystemGrayscale
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

/** Oltre questo, uno sblocco non è più "appena successo" e la home non si chiude. Serve al caso
 * in cui sblocchi dentro un'altra app: tornando alla home molto dopo, il blocco sarebbe
 * slegato dal gesto che doveva interrompere. */
private const val HOME_LOCK_GRACE_MILLIS = 8_000L

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val hiddenAppsRepository = HiddenAppsRepository(application)
    private val homeLayoutRepository = HomeLayoutRepository(application)
    private val focusRepository = FocusRepository(application)
    private val focusStatsRepository = FocusStatsRepository(application)
    private val dimRepository = DimRepository(application)
    private val dumbRepository = DumbRepository(application)

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

    /** Aspetta la scadenza della sessione DUMB. Non è un countdown: non c'è niente da mostrare
     * ogni secondo, la schermata dice solo fino a che ora. Un solo `delay` fino alla fine. */
    private var dumbTimer: Job? = null

    /**
     * Quando è arrivato l'ultimo sblocco vero, o 0 se è già stato consumato.
     *
     * **I due eventi arrivano in ordine imprevedibile, e il primo tentativo dava per scontato
     * il contrario.** Sbloccando, il launcher fa `onResume` *dietro* la schermata di blocco —
     * appena lo schermo si accende — e `ACTION_USER_PRESENT` arriva solo dopo, a blocco
     * tolto: controllando il flag dentro `onResume` non era ancora alzato, e restava buono
     * fino al `onResume` successivo. Il respiro non compariva sbloccando e saltava fuori più
     * tardi a caso, per esempio premendo il tasto home dalla schermata Concentrazione.
     *
     * Ora il respiro parte da **qualunque dei due arrivi per ultimo**, e il timestamp serve a
     * scartare uno sblocco troppo vecchio: senza, un flag rimasto alzato mentre eri dentro
     * un'app farebbe scattare il respiro al rientro, molto dopo lo sblocco.
     */
    private var unlockedAtElapsed = 0L
    private var launcherResumed = false

    // ACTION_USER_PRESENT non si può dichiarare nel manifest (non è mai stato consegnato lì,
    // nemmeno prima delle restrizioni sui broadcast impliciti di Android 8): va registrato a
    // runtime, e siccome il launcher è quasi sempre vivo — è la home — resta valido per tutta
    // la vita del processo, non solo mentre l'activity è in primo piano.
    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            unlockedAtElapsed = SystemClock.elapsedRealtime()
            maybeLockHome()
        }
    }

    init {
        refreshApps()
        restoreFocusSession()
        restoreDim()
        restoreDumbSession()
        ContextCompat.registerReceiver(
            application,
            userPresentReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(userPresentReceiver)
    }

    fun onLauncherResumed() {
        launcherResumed = true
        // Il `delay` che chiude la sessione DUMB non scorre mentre il telefono dorme: se la
        // scadenza è passata a schermo spento, va constatata adesso invece di aspettare che
        // il timer si svegli con ore di ritardo.
        syncDumbExpiry()
        maybeLockHome()
    }

    // ----- Luminosità extra -----

    private fun restoreDim() {
        _uiState.value = _uiState.value.copy(
            dimEnabled = dimRepository.isEnabled(),
            dimLevel = dimRepository.getLevel(),
            dimSystemAvailable = SystemDim.isAvailable(getApplication<Application>())
        )
    }

    fun setDimEnabled(enabled: Boolean) {
        dimRepository.setEnabled(enabled)
        _uiState.value = _uiState.value.copy(dimEnabled = enabled)
        applyDim(enabled && canDim())
    }

    fun setDimLevel(value: Float) {
        dimRepository.setLevel(value)
        _uiState.value = _uiState.value.copy(dimLevel = dimRepository.getLevel())
        if (_uiState.value.dimEnabled && canDim()) applyDim(true)
    }

    /** Il permesso di overlay serve solo alla strada del velo: con la riduzione di sistema
     * disponibile, l'attenuazione funziona anche senza. */
    private fun canDim(): Boolean =
        _uiState.value.dimSystemAvailable || _uiState.value.dimOverlayAllowed

    /** Richiamato a ogni rientro nel launcher: il permesso si concede da una schermata di
     * sistema, quindi può essere cambiato mentre eravamo fuori. È anche il punto in cui il
     * velo riparte da solo se il risparmio energetico di MIUI ha ucciso il servizio. */
    fun onOverlayPermissionChanged(canDrawOverlays: Boolean) {
        _uiState.value = _uiState.value.copy(
            dimOverlayAllowed = canDrawOverlays,
            // Anche questa va riletta a ogni rientro: `WRITE_SECURE_SETTINGS` si concede da
            // computer mentre il launcher è aperto, e senza rileggerla resterebbe sul velo
            // fino al riavvio del processo.
            dimSystemAvailable = SystemDim.isAvailable(getApplication<Application>())
        )
        if (_uiState.value.dimEnabled && canDim()) applyDim(true)
    }

    /** False quando il sistema non espone la schermata del permesso: chi chiama lo dice
     * all'utente invece di lasciare il pulsante senza effetto. */
    fun openOverlayPermissionSettings(): Boolean = appRepository.openOverlayPermissionSettings()

    fun openDim() {
        _uiState.value = _uiState.value.copy(screen = Screen.DIM)
    }

    /**
     * Due strade per la stessa cosa, e si prende la migliore che c'è.
     *
     * Se il sistema espone "Riduci luminosità" e il permesso è concesso, si usa quella:
     * agisce sul display, quindi **scurisce anche la barra di stato e quella di navigazione**,
     * che un velo non può coprire per costruzione (vedi `SystemDim`). Altrimenti si torna al
     * velo di sempre.
     *
     * Quando si accende una delle due, l'altra va spenta esplicitamente: passando da un
     * telefono senza permesso a uno con permesso — o riavviando dopo un `pm grant` — si
     * finirebbe altrimenti con velo e riduzione accesi insieme, cioè uno schermo scurito il
     * doppio.
     */
    private fun applyDim(show: Boolean) {
        val context = getApplication<Application>()
        val useSystem = SystemDim.isAvailable(context)
        if (show && useSystem) {
            ScreenDimService.stop(context)
            SystemDim.enable(context, dimRepository.getLevel())
        } else if (show) {
            SystemDim.disable(context)
            ScreenDimService.start(context)
        } else {
            SystemDim.disable(context)
            ScreenDimService.stop(context)
        }
    }

    fun onLauncherPaused() {
        launcherResumed = false
    }

    /**
     * Chiude la home se sono vere tutte insieme: sblocco recente, launcher in primo piano,
     * sessione di Concentrazione attiva, home non già chiusa.
     *
     * Chiamata da entrambi i lati (il receiver e `onResume`) proprio perché l'ordine dei due
     * eventi non è garantito — vince chi arriva per ultimo, una volta sola.
     *
     * **Non si riapre da sola.** Prima era un'attesa di qualche secondo, e non funzionava: un
     * ritardo passivo non interrompe l'impulso, lo rimanda — guardi lo schermo pensando
     * all'app che volevi aprire e poi la apri lo stesso. Ora bisogna chiedere il telefono
     * esplicitamente (`unlockHome`), che è una decisione invece che un'attesa. La via d'uscita
     * c'è sempre, di proposito: l'utente vuole poterlo usare quando serve davvero, non essere
     * bloccato fuori.
     *
     * Fuori da una sessione non succede niente: il punto era "prendo in mano il telefono e mi
     * muovo solo per il gusto di farlo", che è esattamente il momento in cui la Concentrazione
     * è già lo strumento giusto — non un freno acceso sempre, che scatterebbe anche quando
     * sbloccare ha uno scopo preciso.
     */
    private fun maybeLockHome() {
        if (!launcherResumed || unlockedAtElapsed == 0L) return
        if (SystemClock.elapsedRealtime() - unlockedAtElapsed > HOME_LOCK_GRACE_MILLIS) {
            unlockedAtElapsed = 0L
            return
        }
        unlockedAtElapsed = 0L
        // In DUMB non serve: quella schermata è già la risposta a "l'ho preso in mano senza
        // motivo", e sovrapporle un secondo blocco vorrebbe dire due conferme per fare una
        // telefonata.
        if (_uiState.value.dumbActive) return
        if (!_uiState.value.focusActive || _uiState.value.homeLockActive) return

        _uiState.value = _uiState.value.copy(homeLockActive = true)
    }

    /**
     * "Mi serve il telefono": riapre la home e segna la richiesta.
     *
     * Il conteggio non serve a impedire niente — è la stessa idea dello storico della
     * Concentrazione: rendere visibile la ripetizione, che è il vero sintomo. Una volta può
     * avere un motivo, la quinta molto meno, e vederlo scritto costa più di un'attesa.
     */
    fun unlockHome() {
        if (!_uiState.value.homeLockActive) return
        val requests = focusRepository.getSessionPhoneRequests() + 1
        focusRepository.setSessionPhoneRequests(requests)
        _uiState.value = _uiState.value.copy(
            homeLockActive = false,
            homeLockRequests = requests
        )
    }

    /** Re-reads installed apps, drops uninstalled/hidden components from the layout, and
     * seeds a sensible default layout (dock + home, in alphabetical order) on first run. */
    fun refreshApps() {
        viewModelScope.launch {
            // Anche getHiddenPackages() sta su IO: la prima chiamata apre l'archivio
            // cifrato, che parla col Keystore, e questo gira a ogni onResume.
            val (apps, hidden) = withContext(Dispatchers.IO) {
                appRepository.loadLaunchableApps() to hiddenAppsRepository.getHiddenPackages()
            }
            val validComponents = apps
                .filter { it.packageName !in hidden }
                .map { it.componentName }
                .toSet()

            // Potare in base a un elenco vuoto cancellerebbe l'intera disposizione, e
            // queryIntentActivities può tornare vuota per motivi passeggeri (aggiornamento di
            // un'app in corso, PackageManager non ancora pronto dopo un riavvio).
            if (apps.isNotEmpty()) homeLayoutRepository.removeInvalid(validComponents)

            var home = homeLayoutRepository.getHomeItems()
            var dock = homeLayoutRepository.getDockSlots()

            // La semina automatica avviene una volta sola nella vita dell'installazione.
            // Prima era condizionata a "home e dock vuoti", che è la stessa cosa solo al
            // primo avvio: dopo, svuotare la home a mano — o una potatura andata male — la
            // faceva riscattare, ripopolando tutto in ordine alfabetico.
            if (!homeLayoutRepository.isSeeded() && apps.isNotEmpty()) {
                if (home.isEmpty() && dock.all { it == null }) {
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
                // Marcato anche quando una disposizione c'era già: aggiornando da una
                // versione senza questo flag, il layout esistente non va riscritto.
                homeLayoutRepository.markSeeded()
            }

            if (apps.isNotEmpty()) seedDumbSlots(apps, hidden)

            _uiState.value = _uiState.value.copy(
                allApps = apps,
                hiddenPackages = hidden,
                homeComponents = home,
                dockComponents = dock,
                dumbSlots = dumbRepository.getSlots().orEmpty(),
                dumbDurationMinutes = dumbRepository.getDurationMinutes(),
                dumbEarlyExits = dumbRepository.getEarlyExits(),
                dumbGrayscaleAvailable = SystemGrayscale.isAvailable(getApplication<Application>()),
                unlockRequired = hiddenAppsRepository.isUnlockRequired(),
                vaultUnavailable = hiddenAppsRepository.isUnavailable(),
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
        // In DUMB le uniche app raggiungibili sono le cinque che hai scelto proprio per essere
        // raggiungibili: la conferma della Concentrazione qui sarebbe un ostacolo messo davanti
        // a una decisione già presa.
        if (_uiState.value.dumbActive) {
            appRepository.launch(app.componentName)
            return
        }
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
            focusSessionCount = focusStatsRepository.getSessionCount(),
            // Il tratto di resistenza riparte a ogni cedimento: qui viaggia insieme al resto
            // dello storico, che è esattamente quando cambia.
            focusStreakStartMillis = focusStatsRepository.getStreakStartMillis()
        )
    }

    fun resetFocusStats() {
        focusStatsRepository.reset(
            sessionActive = _uiState.value.focusActive,
            nowMillis = System.currentTimeMillis()
        )
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
        // Il conteggio vale *dentro* una sessione, non da una sessione all'altra: senza questo
        // azzeramento la seconda sessione della giornata partirebbe già col conto della prima,
        // e il numero non direbbe più niente su quanto stai cedendo adesso.
        focusRepository.setSessionPhoneRequests(0)
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
            focusEndsAt = 0L,
            focusStreakStartMillis = 0L,
            focusToastVisible = false,
            frictionApp = null,
            // Se la sessione scade mentre la home è chiusa, il blocco cade con lei: altrimenti
            // resteresti davanti a una schermata che chiede il telefono per una sessione che
            // non esiste più, e l'unica uscita sarebbe chiederlo.
            homeLockActive = false,
            homeLockRequests = 0,
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
        _uiState.value = _uiState.value.copy(
            focusActive = true,
            focusEndsAt = endsAt,
            focusStreakStartMillis = focusStatsRepository.getStreakStartMillis()
        )

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

    // --- Modalità DUMB ------------------------------------------------------------------
    //
    // La Concentrazione mette in grigio le app che *tu* hai segnato e chiede conferma prima di
    // aprirle: è un freno, e il telefono resta un telefono. DUMB è l'altra cosa: per il tempo
    // che scegli il launcher smette di essere un launcher e diventa un elenco di cinque nomi.
    // Niente icone, niente cassetto, niente ricerca, niente sfondo. Non è che le altre app
    // siano "vietate" — semplicemente da qui non c'è più niente da toccare per arrivarci.

    /**
     * Le cinque posizioni come si presentano la **prima volta**: telefono e messaggi
     * predefiniti di sistema, poi WhatsApp, Amazon Music e la fotocamera.
     *
     * I due predefiniti si ripescano dall'elenco già caricato a partire dai package che il
     * sistema dichiara (nessun permesso, nessuna voce in `<queries>`). Se il dialer
     * predefinito non ha un'icona nel launcher — capita con certe app di sistema — la
     * posizione resta vuota invece di riempirsi con qualcosa a caso.
     */
    private fun defaultDumbSlots(apps: List<AppInfo>, hidden: Set<String>): List<ComponentName?> {
        val visible = apps.filter { it.packageName !in hidden }
        fun byPackage(pkg: String?): ComponentName? =
            pkg?.let { p -> visible.firstOrNull { it.packageName == p }?.componentName }

        // La fotocamera cambia package su ogni ROM (com.android.camera, com.miui.camera,
        // com.google.android.GoogleCamera…), quindi si cerca per sottostringa.
        val camera = visible
            .firstOrNull { it.packageName.contains(DumbRepository.CAMERA_HINT, ignoreCase = true) }
            ?.componentName

        val seeds = listOf(
            byPackage(appRepository.defaultDialerPackage()),
            byPackage(appRepository.defaultSmsPackage())
        ) + DumbRepository.DEFAULT_PACKAGES.map { byPackage(it) } + listOf(camera)

        return List(DumbRepository.SLOT_COUNT) { seeds.getOrNull(it) }
    }

    /**
     * Semina le posizioni **solo la prima volta**, e la differenza è tutta in
     * `getSlots() == null`: dopo, delle posizioni vuote vogliono dire che le hai svuotate di
     * proposito, e riseminarle ti rimetterebbe in mano proprio le app da cui volevi stare
     * lontano.
     *
     * Chi aggiorna dalla versione in cui telefono e messaggi erano fissi ritrova le sue tre
     * scelte nelle ultime tre posizioni, invece di ripartire dal default.
     */
    private fun seedDumbSlots(apps: List<AppInfo>, hidden: Set<String>) {
        if (dumbRepository.getSlots() != null) return
        val legacy = dumbRepository.legacyChosen()
        val slots = if (legacy != null) {
            val defaults = defaultDumbSlots(apps, hidden)
            listOf(defaults[0], defaults[1]) + List(DumbRepository.SLOT_COUNT - 2) { legacy.getOrNull(it) }
        } else {
            defaultDumbSlots(apps, hidden)
        }
        dumbRepository.setSlots(slots)
    }

    /** "Riparti dai predefiniti" dalle impostazioni DUMB: rimette le cinque posizioni com'erano
     * al primo avvio. Esiste perché ora sono tutte modificabili, e una configurazione che si
     * può disfare deve potersi anche rifare senza reinstallare l'app. */
    fun resetDumbSlots() {
        val current = _uiState.value
        val slots = defaultDumbSlots(current.allApps, current.hiddenPackages)
        dumbRepository.setSlots(slots)
        _uiState.value = current.copy(dumbSlots = slots)
    }

    fun openDumbSettings() {
        _uiState.value = _uiState.value.copy(screen = Screen.DUMB_SETTINGS)
    }

    fun setDumbDuration(minutes: Int) {
        dumbRepository.setDurationMinutes(minutes)
        _uiState.value = _uiState.value.copy(dumbDurationMinutes = dumbRepository.getDurationMinutes())
    }

    /** Apre il cassetto per riempire una delle tre posizioni libere. Il cassetto in modalità
     * scelta mostra solo le app visibili, quindi una nascosta non può finire qui — e non
     * comparirebbe comunque in una schermata non protetta. */
    fun openDrawerForDumbPick(slot: Int) {
        _uiState.value = _uiState.value.copy(
            screen = Screen.DRAWER,
            drawerMode = DrawerMode.PICK_FOR_DUMB,
            pendingDumbSlot = slot,
            query = ""
        )
    }

    private fun setDumbSlot(slot: Int, component: ComponentName?) {
        val slots = MutableList<ComponentName?>(DumbRepository.SLOT_COUNT) { index ->
            _uiState.value.dumbSlots.getOrNull(index)
        }
        if (slot !in slots.indices) return
        slots[slot] = component
        // La stessa app in due posizioni sarebbe solo una riga sprecata su cinque.
        if (component != null) {
            slots.forEachIndexed { index, existing ->
                if (index != slot && existing == component) slots[index] = null
            }
        }
        dumbRepository.setSlots(slots)
        _uiState.value = _uiState.value.copy(dumbSlots = slots)
    }

    fun clearDumbSlot(slot: Int) = setDumbSlot(slot, null)

    /**
     * Avvia la sessione. La scadenza è un orario assoluto salvato su disco: MIUI chiude
     * volentieri il launcher, e con un contatore in memoria per uscire dalla modalità
     * basterebbe aspettare che il sistema faccia pulizia.
     */
    fun startDumb() {
        val endsAt = System.currentTimeMillis() + _uiState.value.dumbDurationMinutes * 60_000L
        dumbRepository.setEndsAt(endsAt)
        startDumbTimer(endsAt)
    }

    private fun startDumbTimer(endsAt: Long) {
        dumbTimer?.cancel()
        SystemGrayscale.enable(getApplication<Application>())
        _uiState.value = _uiState.value.copy(
            dumbActive = true,
            dumbEndsAt = endsAt,
            // Entrando si chiude tutto quello che potrebbe restare aperto sopra: un menu
            // contestuale o una richiesta di conferma sopravvissuti mostrerebbero il nome di
            // un'app che in DUMB non deve esistere.
            contextMenu = null,
            frictionApp = null,
            focusPickerVisible = false,
            dumbExitPromptVisible = false
        )
        dumbTimer = viewModelScope.launch {
            while (true) {
                val remaining = endsAt - System.currentTimeMillis()
                if (remaining <= 0) {
                    endDumb()
                    break
                }
                delay(remaining.coerceAtMost(60_000L))
            }
        }
    }

    /** Scadenza naturale: la sessione finisce e basta, non viene contata da nessuna parte. */
    private fun endDumb() {
        dumbTimer?.cancel()
        dumbTimer = null
        dumbRepository.setEndsAt(0L)
        SystemGrayscale.disable(getApplication<Application>())
        _uiState.value = _uiState.value.copy(
            dumbActive = false,
            dumbEndsAt = 0L,
            dumbExitPromptVisible = false,
            screen = Screen.HOME
        )
    }

    /** Al ritorno in primo piano: il `delay` non scorre a telefono addormentato. */
    private fun syncDumbExpiry() {
        if (!_uiState.value.dumbActive) return
        if (dumbRepository.getEndsAt() <= System.currentTimeMillis()) endDumb()
    }

    private fun restoreDumbSession() {
        val endsAt = dumbRepository.getEndsAt()
        if (endsAt > System.currentTimeMillis()) {
            startDumbTimer(endsAt)
        } else {
            dumbRepository.setEndsAt(0L)
            // Scaduta mentre il launcher era chiuso: il grigio di sistema è rimasto acceso,
            // perché a spegnerlo è questo processo. Va tolto anche qui, o resterebbe finché
            // non entri in DUMB un'altra volta.
            SystemGrayscale.disable(getApplication<Application>())
        }
    }

    fun requestDumbExit() {
        _uiState.value = _uiState.value.copy(dumbExitPromptVisible = true)
    }

    fun dismissDumbExit() {
        _uiState.value = _uiState.value.copy(dumbExitPromptVisible = false)
    }

    /**
     * L'uscita anticipata c'è, ed è voluta: una modalità da cui non si esce viene disinstallata
     * al primo imprevisto. Ma costa una conferma e **viene contata**, per sempre — è la stessa
     * idea dello storico della Concentrazione: non impedire, rendere visibile la ripetizione.
     */
    fun confirmDumbExit() {
        dumbRepository.setEarlyExits(dumbRepository.getEarlyExits() + 1)
        _uiState.value = _uiState.value.copy(dumbEarlyExits = dumbRepository.getEarlyExits())
        endDumb()
    }

    fun openDrawer() {
        _uiState.value = _uiState.value.copy(
            screen = Screen.DRAWER,
            drawerMode = DrawerMode.BROWSE,
            query = ""
        )
    }

    /** La scorciatoia dalla home (swipe su a due dita): porta dritti alle app nascoste, o alla
     * richiesta di sblocco se è attiva. Non passa dal cassetto normale — è tutto il punto. */
    fun openHiddenDrawer() {
        _uiState.value = _uiState.value.copy(
            screen = Screen.HIDDEN_DRAWER,
            drawerMode = DrawerMode.BROWSE,
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
                // Se l'app è in grigio, launchApp non ha aperto niente: ha alzato la
                // richiesta di conferma. Chiudere il cassetto adesso la lascerebbe sopra la
                // home, e "Resta sul pezzo" ti farebbe perdere anche la ricerca digitata.
                if (_uiState.value.frictionApp == null) backToHome()
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
            DrawerMode.PICK_FOR_DUMB -> {
                setDumbSlot(_uiState.value.pendingDumbSlot, app.componentName)
                // Si torna alle impostazioni DUMB, non alla home: le posizioni si riempiono
                // quasi sempre più di una alla volta.
                _uiState.value = _uiState.value.copy(
                    screen = Screen.DUMB_SETTINGS,
                    drawerMode = DrawerMode.BROWSE,
                    pendingDumbSlot = -1,
                    query = "",
                    contextMenu = null
                )
            }
        }
    }

    /** La chiusura del cassetto. Annullando la scelta di un'app per DUMB si torna alle sue
     * impostazioni, non alla home: da lì eri partito, e ributtarti in home vorrebbe dire
     * rifare tutto il giro per riempire la posizione successiva. */
    fun closeDrawer() {
        if (_uiState.value.drawerMode == DrawerMode.PICK_FOR_DUMB) {
            _uiState.value = _uiState.value.copy(
                screen = Screen.DUMB_SETTINGS,
                drawerMode = DrawerMode.BROWSE,
                pendingDumbSlot = -1,
                query = "",
                contextMenu = null
            )
        } else {
            backToHome()
        }
    }

    fun backToHome() {
        _uiState.value = _uiState.value.copy(
            screen = Screen.HOME,
            drawerMode = DrawerMode.BROWSE,
            pendingDockSlot = -1,
            pendingDumbSlot = -1,
            query = "",
            contextMenu = null
        )
    }

    /** Back from the nested "manage hidden apps" settings screen to the hidden drawer it was
     * opened from, rather than all the way home. */
    fun backToHiddenDrawer() {
        _uiState.value = _uiState.value.copy(
            screen = Screen.HIDDEN_DRAWER,
            drawerMode = DrawerMode.BROWSE,
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
        val targetPageIndex = currentPageIndex + direction
        // Fuori dalle pagine esistenti non si fa niente. Prima verso sinistra dalla prima
        // pagina l'indice veniva portato a 0 e l'icona finiva in testa alla stessa pagina —
        // cioè un riordino dentro la pagina, che questo launcher non fa; e verso destra
        // dall'ultima l'inserimento ricadeva in fondo alla stessa pagina, quindi il gesto
        // sembrava semplicemente ignorato.
        if (targetPageIndex < 0 || targetPageIndex >= pages.size) {
            dismissContextMenu()
            return
        }
        homeLayoutRepository.moveToPage(app.componentName, targetPageIndex, currentState.pageSize)
        syncLayout()
        dismissContextMenu()
    }

    fun hideApp(app: AppInfo) {
        applyHidden(app, hidden = true)
        dismissContextMenu()
    }

    /**
     * Nascondere e mostrare passano di qui perché la parte delicata è la stessa.
     *
     * Si ragiona per **package**, non per componente: un'app con più voci nel launcher (comune
     * fra quelle di sistema Xiaomi) lascerebbe in home le icone che non hai toccato, ancora
     * apribili fino al primo rientro nel launcher.
     *
     * E si toglie anche dalle app della Concentrazione: quell'elenco sta in preferenze **non
     * cifrate**, quindi il nome del package di un'app nascosta non deve restarci.
     */
    private fun applyHidden(app: AppInfo, hidden: Boolean) {
        hiddenAppsRepository.setHidden(app.packageName, hidden)
        if (hidden) {
            _uiState.value.allApps
                .filter { it.packageName == app.packageName }
                .forEach { homeLayoutRepository.removeFromHome(it.componentName) }
            focusRepository.setDistracting(app.packageName, false)
        }
        _uiState.value = _uiState.value.copy(
            hiddenPackages = hiddenAppsRepository.getHiddenPackages(),
            homeComponents = homeLayoutRepository.getHomeItems(),
            dockComponents = homeLayoutRepository.getDockSlots(),
            focusPackages = focusRepository.getDistractingPackages()
        )
    }

    fun openWallpaperPicker(): Boolean = appRepository.openWallpaperPicker()

    fun openAppInfo(app: AppInfo) {
        appRepository.openAppInfo(app.packageName)
        dismissContextMenu()
    }

    /** False quando il sistema non ha aperto la disinstallazione e si è ripiegato sulle info
     * dell'app: chi chiama lo dice all'utente invece di lasciare il menu senza effetto. */
    fun requestUninstall(app: AppInfo): Boolean {
        val started = appRepository.requestUninstall(app.packageName)
        dismissContextMenu()
        return started
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

    /** Chiamata mentre riscrivi: "PIN errato" e il bordo rosso restavano accesi fino allo
     * sblocco, quindi il secondo tentativo partiva già segnato come sbagliato. */
    fun clearUnlockError() {
        if (_uiState.value.unlockError) {
            _uiState.value = _uiState.value.copy(unlockError = false)
        }
    }

    /** Called when the launcher stops (app opened, screen off, task switched): the hidden
     * page re-locks itself, so it is never left unlocked behind your back. */
    fun lockVault() {
        val current = _uiState.value
        // Si torna sempre alla home, anche dal cassetto normale: riaccendendo lo schermo su una
        // schermata aperta prima di andarsene si perde il senso di dove si è, e per le nascoste
        // sarebbe anche un'anteprima gratis di cosa c'era dentro.
        // DIM è esclusa come FOCUS, e qui non è solo coerenza: da quella schermata si apre la
        // schermata di sistema del permesso di overlay, il che fa passare il launcher da
        // onStop. Mandandolo alla home, al ritorno ti ritroveresti altrove e non vedresti
        // l'avviso sparire — cioè proprio la conferma che il permesso è stato concesso.
        val leavingVault = current.screen != Screen.HOME &&
            current.screen != Screen.FOCUS &&
            current.screen != Screen.DIM
        _uiState.value = current.copy(
            vaultUnlocked = false,
            unlockError = false,
            screen = if (leavingVault) Screen.HOME else current.screen,
            // Menu contestuale e conferma di apertura portano scritto il nome dell'app: se
            // sopravvivessero al blocco, riaccendendo lo schermo il nome di un'app nascosta
            // resterebbe lì sopra un cassetto ormai chiuso.
            contextMenu = null,
            frictionApp = null
        )
    }

    /** Uscendo dalle app nascoste lo sblocco decade **subito**, senza aspettare che il
     * launcher vada in pausa: altrimenti bastava che qualcuno prendesse il telefono nei
     * secondi dopo — schermo ancora acceso, launcher ancora in primo piano — e rifacesse il
     * gesto per entrare senza impronta né PIN. Cambia solo il lucchetto: la schermata la
     * gestisce chi chiama. */
    fun relockVault() {
        val current = _uiState.value
        if (current.vaultUnlocked || current.unlockError) {
            _uiState.value = current.copy(vaultUnlocked = false, unlockError = false)
        }
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
        applyHidden(app, hidden = app.packageName !in _uiState.value.hiddenPackages)
    }
}

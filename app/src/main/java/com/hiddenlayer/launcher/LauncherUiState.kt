package com.hiddenlayer.launcher

import android.content.ComponentName
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.data.DimRepository
import com.hiddenlayer.launcher.data.DumbRepository
import com.hiddenlayer.launcher.data.FocusBreak
import com.hiddenlayer.launcher.data.FocusRepository
import com.hiddenlayer.launcher.data.HomeLayoutRepository

/** The hidden apps are no longer a screen of their own: they're the second page of the
 * drawer, reached by swiping left, so the transition follows the finger. */
/**
 * Il cassetto delle app nascoste è una **schermata a sé**, non un modo del cassetto normale.
 *
 * Erano lo stesso `DRAWER` con un campo a dire quale delle due mostrare, e il campo veniva
 * letto una volta all'ingresso. Ma `AnimatedContent`, alla radice, tiene la schermata uscente
 * composta finché la sua animazione non finisce: chiudere il cassetto e riaprirlo subito col
 * gesto a due dita ricadeva su quella composizione ancora viva, il `remember` non veniva
 * rivalutato e si riapriva il cassetto normale. Due valori distinti dell'enum sono due voci
 * distinte per `AnimatedContent`, quindi la schermata giusta viene composta da zero.
 */
enum class Screen { HOME, DRAWER, HIDDEN_DRAWER, HIDDEN_MANAGER, FOCUS, DIM, DUMB_SETTINGS }

enum class DrawerMode { BROWSE, PICK_FOR_HOME, PICK_FOR_DOCK, PICK_FOR_DUMB }

enum class MenuOrigin { HOME, DOCK, DRAWER, HIDDEN_DRAWER }

data class ContextMenuState(
    val app: AppInfo,
    val origin: MenuOrigin,
    val dockSlot: Int = -1
)

data class LauncherUiState(
    val allApps: List<AppInfo> = emptyList(),
    val hiddenPackages: Set<String> = emptySet(),
    val homeComponents: List<ComponentName> = emptyList(),
    /** Fixed-size dock grid: null where a slot is empty, so the second row can hold an
     * app while the first row still has gaps. */
    val dockComponents: List<ComponentName?> = emptyList(),
    val screen: Screen = Screen.HOME,
    val drawerMode: DrawerMode = DrawerMode.BROWSE,
    val pendingDockSlot: Int = -1,
    val pendingDumbSlot: Int = -1,
    val query: String = "",
    val contextMenu: ContextMenuState? = null,
    /** How many icons fit on one home page, measured from the real screen height by
     * HomeScreen (rows that fit x columns) rather than hardcoded. */
    val pageSize: Int = HomeLayoutRepository.DEFAULT_PAGE_SIZE,
    /** Whether reaching the hidden page asks for biometrics/PIN first (off until switched on). */
    val unlockRequired: Boolean = false,
    /** L'archivio cifrato non si è potuto aprire. L'elenco delle nascoste risulta vuoto, e
     * va detto: altrimenti sembra che non ci sia mai stato niente. */
    val vaultUnavailable: Boolean = false,
    val unlockError: Boolean = false,
    /** Cleared every time the launcher is left, so the hidden page re-locks itself. */
    val vaultUnlocked: Boolean = false,
    /** La home è chiusa dopo uno sblocco vero (non un ritorno da un'app), durante la
     * Concentrazione: non si riapre da sola, serve chiedere il telefono. Cambia due volte per
     * attivazione, come `focusToastVisible`, quindi non fa ricomporre niente di continuo. */
    val homeLockActive: Boolean = false,
    /** Quante volte hai chiesto il telefono in questa sessione. Mostrato sulla schermata di
     * blocco dalla seconda in poi: è la ripetizione il sintomo, non la singola volta. */
    val homeLockRequests: Int = 0,
    // --- Luminosità extra ---
    /** Cambia solo quando la accendi o la spegni: può stare qui senza costare niente. */
    val dimEnabled: Boolean = false,
    val dimLevel: Float = DimRepository.DEFAULT_LEVEL,
    /** Se il permesso "Visualizza sopra altre app" è concesso. Senza, il velo non può esistere
     * e va detto, invece di lasciar credere che sia rotto. Riletto a ogni rientro nel launcher,
     * perché si concede da una schermata di sistema. */
    val dimOverlayAllowed: Boolean = false,
    /** Se il sistema può attenuare **da solo** ("Riduci luminosità" di Android 12+, con
     * `WRITE_SECURE_SETTINGS` concesso). Quando è vero si usa quella invece del velo: agisce
     * sul display, quindi scurisce anche la barra di stato e quella di navigazione, che un
     * overlay non può coprire. Cambia solo quando si concede il permesso. */
    val dimSystemAvailable: Boolean = false,
    // --- Modalità DUMB ---
    /** Se una sessione DUMB è in corso. Quando è true il launcher non compone nient'altro:
     * niente cassetto, niente dock, niente gesti. Non è "nascosto", è proprio assente. */
    val dumbActive: Boolean = false,
    /** Orario di fine, epoch millis. Mostrato come "fino alle HH:mm", non come conto alla
     * rovescia: guardare i minuti scendere è già un modo di stare al telefono. */
    val dumbEndsAt: Long = 0L,
    val dumbDurationMinutes: Int = DumbRepository.DEFAULT_DURATION_MINUTES,
    /** Le cinque posizioni, in ordine; `null` dove è vuota. **Tutte modificabili**: le prime
     * due sono seminate col telefono e i messaggi predefiniti di sistema, ma da lì in poi le
     * decidi tu — vedi `DumbRepository`. */
    val dumbSlots: List<ComponentName?> = emptyList(),
    /** Quante volte hai chiuso una sessione prima della scadenza, da sempre. */
    val dumbEarlyExits: Int = 0,
    /** L'uscita anticipata chiede conferma. */
    val dumbExitPromptVisible: Boolean = false,
    /** Se il grigio si può estendere a tutto il telefono (permesso di sistema concesso via
     * ADB) o resta dentro il launcher. Serve a dirlo nelle impostazioni: senza, sembrerebbe
     * semplicemente che non funzioni. */
    val dumbGrayscaleAvailable: Boolean = false,
    // --- Focus sessions ---
    val focusPackages: Set<String> = emptySet(),
    val focusDurationMinutes: Int = FocusRepository.DEFAULT_DURATION_MINUTES,
    /** Se una sessione è in corso. I secondi che mancano NON stanno qui: vivono in un flow
     * a parte (LauncherViewModel.focusRemainingSeconds), perché un campo che cambia ogni
     * secondo dentro questo stato farebbe ricomporre l'intera app a ogni tick — cassetto
     * compreso. Questo invece cambia due volte per sessione. */
    val focusActive: Boolean = false,
    /** Quando finisce la sessione, epoch millis (0 se non ce n'è una). La schermata della
     * sessione in corso lo mostra come "fino alle 21:06": un orario assoluto si legge una
     * volta e si sa, mentre il countdown da solo invita a ricontrollare. Cambia due volte per
     * sessione, quindi può stare qui. */
    val focusEndsAt: Long = 0L,
    /** Da quando dura il tratto di resistenza in corso, epoch millis. È l'**inizio**, non la
     * durata: la durata ticchetterebbe, questo cambia solo quando cedi. */
    val focusStreakStartMillis: Long = 0L,
    /** Il doppio tap sulla home ha chiesto per quanto tempo. */
    val focusPickerVisible: Boolean = false,
    /** La pill col countdown è una conferma, non un elemento fisso della home: resta accesa
     * qualche secondo dopo l'avvio e poi si spegne da sola. */
    val focusToastVisible: Boolean = false,
    /** The app whose launch is being second-guessed by the "are you sure?" prompt. */
    val frictionApp: AppInfo? = null,
    /** Da quanto stavi resistendo quando è comparsa quella richiesta di conferma. È una
     * fotografia presa all'apertura del popup, non un contatore che scorre: il popup dura
     * pochi secondi e un campo che ticchetta qui dentro ricomporrebbe tutta l'app. */
    val frictionStreakSeconds: Int = 0,
    // --- Storico della Concentrazione (cambia solo quando cedi o quando una sessione
    // inizia o finisce, quindi può stare qui senza costare niente) ---
    val focusRecordSeconds: Int = 0,
    val focusBreaks: List<FocusBreak> = emptyList(),
    val focusSessionCount: Int = 0,
    val loaded: Boolean = false
) {
    /** True for an app that is muted by the running session: greyed out everywhere, and
     * asks for confirmation before it will open. */
    fun isMuted(app: AppInfo): Boolean = focusActive && app.packageName in focusPackages

    /** Le app raggiungibili in DUMB, nell'ordine delle posizioni. Le nascoste sono escluse
     * per costruzione — il cassetto in modalità scelta non le mostra — e il filtro qui è la
     * rete di sicurezza: la regola vale anche in DUMB, nessuna schermata non protetta le
     * mostra. */
    val dumbApps: List<AppInfo>
        get() {
            val byComponent = allApps.associateBy { it.componentName }
            return dumbSlots
                .filterNotNull()
                .distinct()
                .mapNotNull { byComponent[it] }
                .filter { it.packageName !in hiddenPackages }
        }

    val homeApps: List<AppInfo>
        get() {
            val byComponent = allApps.associateBy { it.componentName }
            return homeComponents.mapNotNull { byComponent[it] }
        }

    val dockSlots: List<AppInfo?>
        get() {
            val byComponent = allApps.associateBy { it.componentName }
            return List(HomeLayoutRepository.DOCK_SIZE) { index ->
                dockComponents.getOrNull(index)?.let { byComponent[it] }
            }
        }

    val homePages: List<List<AppInfo>>
        get() = homeApps.chunked(pageSize.coerceAtLeast(1)).ifEmpty { listOf(emptyList()) }

    val visibleApps: List<AppInfo>
        get() = allApps
            .filter { it.packageName !in hiddenPackages }
            .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }

    val hiddenApps: List<AppInfo>
        get() = allApps.filter { it.packageName in hiddenPackages }

    /** What the incognito-style hidden drawer shows: hidden apps, filtered by the same
     * search field the regular drawer uses (only one of the two is ever on screen). */
    val hiddenVisibleApps: List<AppInfo>
        get() = hiddenApps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
}

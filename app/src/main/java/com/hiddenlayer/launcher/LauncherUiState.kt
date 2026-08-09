package com.hiddenlayer.launcher

import android.content.ComponentName
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.data.FocusBreak
import com.hiddenlayer.launcher.data.FocusRepository
import com.hiddenlayer.launcher.data.HomeLayoutRepository
import com.hiddenlayer.launcher.data.NightModeRepository

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
enum class Screen { HOME, DRAWER, HIDDEN_DRAWER, HIDDEN_MANAGER, FOCUS, NIGHT_MODE }

enum class DrawerMode { BROWSE, PICK_FOR_HOME, PICK_FOR_DOCK }

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
    // --- Modalità rossa (astrofotografia) ---
    /** Cambia due volte per sessione notturna, quindi può stare qui dentro senza problemi
     * (vedi la regola su ciò che non deve ticchettare in `uiState`). */
    val nightModeEnabled: Boolean = false,
    val nightRedIntensity: Float = NightModeRepository.DEFAULT_RED,
    val nightDimLevel: Float = NightModeRepository.DEFAULT_DIM,
    /** Se il permesso "Visualizza sopra altre app" è concesso. Senza, il rosso vale solo
     * dentro il launcher, e va detto invece di lasciar credere che sia rotto. Aggiornato al
     * rientro nel launcher, perché si concede da una schermata di sistema. */
    val nightOverlayAllowed: Boolean = false,
    // --- Focus sessions ---
    val focusPackages: Set<String> = emptySet(),
    val focusDurationMinutes: Int = FocusRepository.DEFAULT_DURATION_MINUTES,
    /** Se una sessione è in corso. I secondi che mancano NON stanno qui: vivono in un flow
     * a parte (LauncherViewModel.focusRemainingSeconds), perché un campo che cambia ogni
     * secondo dentro questo stato farebbe ricomporre l'intera app a ogni tick — cassetto
     * compreso. Questo invece cambia due volte per sessione. */
    val focusActive: Boolean = false,
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

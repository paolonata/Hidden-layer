package com.hiddenlayer.launcher

import android.content.ComponentName
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.data.FocusBreak
import com.hiddenlayer.launcher.data.FocusRepository
import com.hiddenlayer.launcher.data.HomeLayoutRepository

/** The hidden apps are no longer a screen of their own: they're the second page of the
 * drawer, reached by swiping left, so the transition follows the finger. */
enum class Screen { HOME, DRAWER, HIDDEN_MANAGER, FOCUS }

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
    /** Which drawer page to open on: 1 when coming back from the hidden-apps settings. */
    val drawerStartPage: Int = 0,
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

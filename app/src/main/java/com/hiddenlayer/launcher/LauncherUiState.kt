package com.hiddenlayer.launcher

import android.content.ComponentName
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.data.HomeLayoutRepository

enum class Screen { HOME, DRAWER, VAULT_UNLOCK, HIDDEN_DRAWER, HIDDEN_MANAGER }

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
    val dockComponents: List<ComponentName> = emptyList(),
    val screen: Screen = Screen.HOME,
    val drawerMode: DrawerMode = DrawerMode.BROWSE,
    val pendingDockSlot: Int = -1,
    val query: String = "",
    val contextMenu: ContextMenuState? = null,
    /** How many icons fit on one home page, measured from the real screen height by
     * HomeScreen (rows that fit x columns) rather than hardcoded. */
    val pageSize: Int = HomeLayoutRepository.DEFAULT_PAGE_SIZE,
    /** Whether opening the vault asks for biometrics/PIN first (off until switched on). */
    val unlockRequired: Boolean = false,
    val unlockError: Boolean = false,
    val loaded: Boolean = false
) {
    val homeApps: List<AppInfo>
        get() {
            val byComponent = allApps.associateBy { it.componentName }
            return homeComponents.mapNotNull { byComponent[it] }
        }

    val dockApps: List<AppInfo>
        get() {
            val byComponent = allApps.associateBy { it.componentName }
            return dockComponents.mapNotNull { byComponent[it] }
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

package com.hiddenlayer.launcher

import android.content.ComponentName
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.data.HomeLayoutRepository

enum class Screen { HOME, DRAWER, PIN_SETUP, PIN_PROMPT, HIDDEN_MANAGER }

enum class DrawerMode { BROWSE, PICK_FOR_HOME, PICK_FOR_DOCK }

enum class MenuOrigin { HOME, DOCK, DRAWER }

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
    val pinError: Boolean = false,
    val contextMenu: ContextMenuState? = null,
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
        get() = homeApps.chunked(HomeLayoutRepository.PAGE_SIZE).ifEmpty { listOf(emptyList()) }

    val visibleApps: List<AppInfo>
        get() = allApps
            .filter { it.packageName !in hiddenPackages }
            .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }

    val hiddenApps: List<AppInfo>
        get() = allApps.filter { it.packageName in hiddenPackages }
}

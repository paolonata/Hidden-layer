package com.hiddenlayer.launcher.data

import android.content.ComponentName
import android.content.Context

/**
 * Persists which apps sit on the home screen(s) and in the dock, as ordered
 * lists of ComponentName. Home apps overflow onto additional pages once a
 * page is full (see LauncherUiState.homePages); there is no per-slot grid,
 * so items are always packed left-to-right / top-to-bottom in insertion order.
 */
class HomeLayoutRepository(context: Context) {

    private val prefs = context.getSharedPreferences("home_layout", Context.MODE_PRIVATE)

    fun getHomeItems(): List<ComponentName> = decode(prefs.getString(KEY_HOME, null))

    fun getDock(): List<ComponentName> = decode(prefs.getString(KEY_DOCK, null))

    fun setHomeItems(items: List<ComponentName>) {
        prefs.edit().putString(KEY_HOME, encode(items)).apply()
    }

    fun setDock(items: List<ComponentName>) {
        prefs.edit().putString(KEY_DOCK, encode(items.take(DOCK_SIZE))).apply()
    }

    // An app lives in exactly one place: the dock or the home pages, never both. The dock
    // is already visible from every page, so leaving a copy on the grid as well is just a
    // duplicate taking up a slot.

    fun addToHome(component: ComponentName) {
        setDock(getDock().filterNot { it == component })
        val items = getHomeItems()
        if (component !in items) setHomeItems(items + component)
    }

    fun addToDock(component: ComponentName): Boolean {
        val dock = getDock()
        if (component in dock) return true
        if (dock.size >= DOCK_SIZE) return false
        setDock(dock + component)
        setHomeItems(getHomeItems().filterNot { it == component })
        return true
    }

    /** Drops an app into the dock at the position it was released over, closing ranks
     * rather than leaving a hole (the stored dock is a plain ordered list, so a gap in the
     * middle can't be represented anyway). Dropping past the last icon appends; dragging an
     * app that is already docked just reorders it. Returns false when the dock is full. */
    fun insertIntoDock(component: ComponentName, preferredSlot: Int): Boolean {
        val dock = getDock().filterNot { it == component }.toMutableList()
        if (dock.size >= DOCK_SIZE) return false
        dock.add(preferredSlot.coerceIn(0, dock.size), component)
        setDock(dock)
        setHomeItems(getHomeItems().filterNot { it == component })
        return true
    }

    fun setDockSlot(slot: Int, component: ComponentName) {
        val dock = getDock().toMutableList()
        if (slot < dock.size) {
            dock[slot] = component
        } else {
            dock.add(component)
        }
        setDock(dock.distinct())
        setHomeItems(getHomeItems().filterNot { it == component })
    }

    fun removeFromHome(component: ComponentName) {
        setHomeItems(getHomeItems().filterNot { it == component })
        setDock(getDock().filterNot { it == component })
    }

    /** Moves an app to the start of the given home page, shifting everything else along.
     * [pageSize] is however many icons currently fit on one page (measured by the UI, see
     * LauncherUiState.pageSize), so this lands on the same page the user actually sees.
     * Targeting a page past the current last one is fine: it simply lands at the end of
     * the list, which overflows into a freshly created page. */
    fun moveToPage(component: ComponentName, targetPageIndex: Int, pageSize: Int) {
        val items = getHomeItems().toMutableList()
        val currentIndex = items.indexOf(component)
        if (currentIndex == -1) return
        items.removeAt(currentIndex)
        val insertIndex = (targetPageIndex * pageSize).coerceIn(0, items.size)
        items.add(insertIndex, component)
        setHomeItems(items)
    }

    /** Drops uninstalled/hidden apps from the layout on every refresh — and, since the dock
     * wins over the grid, also repairs layouts saved before that rule existed by clearing
     * any leftover home copy of an app that sits in the dock. */
    fun removeInvalid(validComponents: Set<ComponentName>) {
        val dock = getDock().filter { it in validComponents }
        setDock(dock)
        setHomeItems(getHomeItems().filter { it in validComponents && it !in dock })
    }

    private fun encode(items: List<ComponentName>): String =
        items.joinToString(";") { it.flattenToString() }

    private fun decode(raw: String?): List<ComponentName> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(";")
            .filter { it.isNotEmpty() }
            .mapNotNull { ComponentName.unflattenFromString(it) }
    }

    companion object {
        /** Only a fallback for the first frame: the real page size is measured from the
         * actual screen height so a page holds as many rows as physically fit. */
        const val DEFAULT_PAGE_SIZE = 20

        /** The dock is two rows of five: the bottom one is always on screen, the one above
         * it stays tucked away until you swipe up on the dock itself. */
        const val DOCK_COLUMNS = 5
        const val DOCK_ROWS = 2
        const val DOCK_SIZE = DOCK_COLUMNS * DOCK_ROWS
        private const val KEY_HOME = "home_items"
        private const val KEY_DOCK = "dock_items"
    }
}

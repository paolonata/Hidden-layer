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

    fun addToHome(component: ComponentName) {
        val items = getHomeItems()
        if (component !in items) setHomeItems(items + component)
    }

    fun addToDock(component: ComponentName): Boolean {
        val dock = getDock()
        if (component in dock) return true
        if (dock.size >= DOCK_SIZE) return false
        setDock(dock + component)
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
    }

    fun removeFromHome(component: ComponentName) {
        setHomeItems(getHomeItems().filterNot { it == component })
        setDock(getDock().filterNot { it == component })
    }

    /** Moves an app to the start of the given home page, shifting everything else along.
     * Targeting a page past the current last one is fine: it simply lands at the end of
     * the list, which overflows into a freshly created page. */
    fun moveToPage(component: ComponentName, targetPageIndex: Int) {
        val items = getHomeItems().toMutableList()
        val currentIndex = items.indexOf(component)
        if (currentIndex == -1) return
        items.removeAt(currentIndex)
        val insertIndex = (targetPageIndex * PAGE_SIZE).coerceIn(0, items.size)
        items.add(insertIndex, component)
        setHomeItems(items)
    }

    fun removeInvalid(validComponents: Set<ComponentName>) {
        setHomeItems(getHomeItems().filter { it in validComponents })
        setDock(getDock().filter { it in validComponents })
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
        const val PAGE_SIZE = 20
        const val DOCK_SIZE = 5
        private const val KEY_HOME = "home_items"
        private const val KEY_DOCK = "dock_items"
    }
}

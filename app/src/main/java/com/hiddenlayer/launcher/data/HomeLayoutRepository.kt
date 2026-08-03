package com.hiddenlayer.launcher.data

import android.content.ComponentName
import android.content.Context

/**
 * Persists which apps sit on the home screen(s) and in the dock.
 *
 * Home items are a plain ordered list that overflows onto further pages once a page is full
 * (see LauncherUiState.homePages), so they're always packed left-to-right with no gaps.
 *
 * The dock is different: it's a fixed grid of [DOCK_ROWS] x [DOCK_COLUMNS] slots, stored as
 * exactly that many positions with empty ones written as blanks. It has to be able to hold a
 * gap — otherwise dropping an app onto an upper row while the bottom row is half empty would
 * silently slide it back down into the bottom row, which is precisely what it used to do.
 */
class HomeLayoutRepository(context: Context) {

    private val prefs = context.getSharedPreferences("home_layout", Context.MODE_PRIVATE)

    fun getHomeItems(): List<ComponentName> = decode(prefs.getString(KEY_HOME, null))

    fun setHomeItems(items: List<ComponentName>) {
        prefs.edit().putString(KEY_HOME, encode(items)).apply()
    }

    /** The dock as [DOCK_SIZE] positions, null where a slot is empty. Also reads the old
     * gap-less format written by earlier versions: those simply land in slots 0..n-1. */
    fun getDockSlots(): List<ComponentName?> {
        val raw = prefs.getString(KEY_DOCK, null)
        if (raw.isNullOrEmpty()) return List(DOCK_SIZE) { null }
        val parts = raw.split(";")
        return List(DOCK_SIZE) { index ->
            parts.getOrNull(index)
                ?.takeIf { it.isNotEmpty() }
                ?.let { ComponentName.unflattenFromString(it) }
        }
    }

    fun setDockSlots(slots: List<ComponentName?>) {
        val normalized = List(DOCK_SIZE) { slots.getOrNull(it) }
        prefs.edit()
            .putString(KEY_DOCK, normalized.joinToString(";") { it?.flattenToString() ?: "" })
            .apply()
    }

    private fun dockedComponents(): Set<ComponentName> = getDockSlots().filterNotNull().toSet()

    // An app lives in exactly one place: the dock or the home pages, never both. The dock
    // is already visible from every page, so leaving a copy on the grid as well is just a
    // duplicate taking up a slot.

    fun addToHome(component: ComponentName) {
        clearFromDock(component)
        val items = getHomeItems()
        if (component !in items) setHomeItems(items + component)
    }

    /** First free slot. Returns false when every slot is taken. */
    fun addToDock(component: ComponentName): Boolean {
        val slots = getDockSlots()
        if (component in slots) return true
        val free = slots.indexOfFirst { it == null }
        if (free == -1) return false
        return placeInDock(component, free)
    }

    /**
     * Drops an app into the slot it was released over. If that exact slot is taken by
     * another app the nearest free slot wins, so a drop never silently swallows an icon
     * that was already there. Returns false only when the dock has no room at all.
     */
    fun placeInDock(component: ComponentName, preferredSlot: Int): Boolean {
        val slots = getDockSlots().toMutableList()

        // Dragging an app that is already docked is a move, so vacate its old slot first.
        val existing = slots.indexOf(component)
        if (existing >= 0) slots[existing] = null

        val target = when {
            preferredSlot in 0 until DOCK_SIZE && slots[preferredSlot] == null -> preferredSlot
            else -> nearestFreeSlot(slots, preferredSlot)
        }

        if (target == -1) {
            if (existing >= 0) slots[existing] = component
            setDockSlots(slots)
            return false
        }

        slots[target] = component
        setDockSlots(slots)
        setHomeItems(getHomeItems().filterNot { it == component })
        return true
    }

    /** Replaces whatever occupies [slot] — used by "Sostituisci" on a docked icon. */
    fun setDockSlot(slot: Int, component: ComponentName) {
        if (slot !in 0 until DOCK_SIZE) return
        val slots = getDockSlots().toMutableList()
        val existing = slots.indexOf(component)
        if (existing >= 0) slots[existing] = null
        slots[slot] = component
        setDockSlots(slots)
        setHomeItems(getHomeItems().filterNot { it == component })
    }

    fun removeFromHome(component: ComponentName) {
        setHomeItems(getHomeItems().filterNot { it == component })
        clearFromDock(component)
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
     * any leftover home copy of a docked app. */
    fun removeInvalid(validComponents: Set<ComponentName>) {
        setDockSlots(getDockSlots().map { it?.takeIf { c -> c in validComponents } })
        val docked = dockedComponents()
        setHomeItems(getHomeItems().filter { it in validComponents && it !in docked })
    }

    /** Se la disposizione iniziale è già stata generata. Senza questo flag bastava
     * svuotare home e dock — a mano, o dopo una potatura andata male — perché al riavvio
     * successivo il launcher ripopolasse la home con tutte le app in ordine alfabetico,
     * cancellando la disposizione scelta dall'utente. */
    fun isSeeded(): Boolean = prefs.getBoolean(KEY_SEEDED, false)

    fun markSeeded() {
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    private fun clearFromDock(component: ComponentName) {
        setDockSlots(getDockSlots().map { if (it == component) null else it })
    }

    /** Searches outwards from the preferred position so a drop lands as close as possible
     * to where the finger actually let go. */
    private fun nearestFreeSlot(slots: List<ComponentName?>, preferredSlot: Int): Int {
        val anchor = preferredSlot.coerceIn(0, DOCK_SIZE - 1)
        for (distance in 0 until DOCK_SIZE) {
            val after = anchor + distance
            if (after < DOCK_SIZE && slots[after] == null) return after
            val before = anchor - distance
            if (before >= 0 && slots[before] == null) return before
        }
        return -1
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

        /** The dock is three rows of five: the bottom one is always on screen, the two
         * above it stay tucked away until you swipe up on the dock itself. Both numbers are
         * free to change — the layout and the drop targeting are derived from them. */
        const val DOCK_COLUMNS = 5
        const val DOCK_ROWS = 3
        const val DOCK_SIZE = DOCK_COLUMNS * DOCK_ROWS
        private const val KEY_HOME = "home_items"
        private const val KEY_DOCK = "dock_items"
        private const val KEY_SEEDED = "layout_seeded"
    }
}

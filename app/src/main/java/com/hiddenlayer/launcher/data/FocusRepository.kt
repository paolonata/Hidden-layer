package com.hiddenlayer.launcher.data

import android.content.Context

/**
 * Focus sessions: which apps are the distracting ones, how long a session lasts, and when
 * the current one ends.
 *
 * The end time is stored as a wall-clock timestamp rather than a countdown, so a session
 * survives the launcher being killed in the background (which MIUI does readily) and can't
 * be reset just by reopening the app.
 */
class FocusRepository(context: Context) {

    private val prefs = context.getSharedPreferences("focus", Context.MODE_PRIVATE)

    fun getDistractingPackages(): Set<String> =
        prefs.getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()

    fun setDistracting(packageName: String, distracting: Boolean) {
        val current = getDistractingPackages().toMutableSet()
        if (distracting) current.add(packageName) else current.remove(packageName)
        prefs.edit().putStringSet(KEY_PACKAGES, current).apply()
    }

    fun getDurationMinutes(): Int = prefs.getInt(KEY_DURATION, DEFAULT_DURATION_MINUTES)

    fun setDurationMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DURATION, minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)).apply()
    }

    /** Epoch millis when the running session ends, or 0 when none is running. */
    fun getSessionEndsAt(): Long = prefs.getLong(KEY_ENDS_AT, 0L)

    fun setSessionEndsAt(endsAt: Long) {
        prefs.edit().putLong(KEY_ENDS_AT, endsAt).apply()
    }

    /**
     * Quante volte hai sbloccato il telefono da quando è iniziata la sessione in corso: è ciò
     * che fa allungare il respiro a ogni sblocco successivo.
     *
     * Sta nelle preferenze e non in memoria perché una sessione dura ore e MIUI chiude
     * volentieri il launcher in background: tenendolo in RAM, il conteggio si azzererebbe da
     * solo proprio nelle sessioni lunghe, che sono quelle in cui serve.
     */
    fun getSessionUnlockCount(): Int = prefs.getInt(KEY_UNLOCKS, 0)

    fun setSessionUnlockCount(count: Int) {
        prefs.edit().putInt(KEY_UNLOCKS, count).apply()
    }

    companion object {
        const val DEFAULT_DURATION_MINUTES = 25
        const val MIN_MINUTES = 5
        const val MAX_MINUTES = 180
        const val STEP_MINUTES = 5
        /** Include tutte e tre le durate del doppio tap, così quella scelta al volo resta
         * a un tocco anche da qui. */
        val PRESET_MINUTES = listOf(15, 30, 45, 60, 120)

        /** Le tre durate del doppio tap. Poche e nette apposta: la scelta deve durare meno
         * di un secondo, tutto il resto si regola dalla schermata Concentrazione. */
        val SHORTCUT_MINUTES = listOf(30, 60, 120)

        private const val KEY_PACKAGES = "distracting_packages"
        private const val KEY_DURATION = "duration_minutes"
        private const val KEY_ENDS_AT = "session_ends_at"
        private const val KEY_UNLOCKS = "session_unlocks"
    }
}

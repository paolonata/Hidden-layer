package com.hiddenlayer.launcher.data

import android.content.ComponentName
import android.content.Context

/**
 * La modalità DUMB: quali app restano raggiungibili, per quanto, e quante volte hai ceduto.
 *
 * Le due posizioni fisse — telefono e messaggi — **non stanno qui**: vengono risolte a runtime
 * dal dialer e dall'app SMS predefiniti di sistema (vedi `LauncherViewModel`). Salvarle
 * sarebbe fragile: cambi app di messaggistica predefinita e ti ritroveresti una modalità di
 * autodisciplina che non ti fa più chiamare nessuno.
 *
 * La scadenza è un orario assoluto, non un conto alla rovescia: una sessione dura ore e MIUI
 * chiude volentieri il launcher: un contatore in memoria si azzererebbe da solo, e uscire
 * dalla modalità basterebbe aspettare che il sistema faccia pulizia.
 */
class DumbRepository(context: Context) {

    private val prefs = context.getSharedPreferences("dumb_mode", Context.MODE_PRIVATE)

    /** Epoch millis di fine, 0 se nessuna sessione è in corso. */
    fun getEndsAt(): Long = prefs.getLong(KEY_ENDS_AT, 0L)

    fun setEndsAt(endsAt: Long) {
        prefs.edit().putLong(KEY_ENDS_AT, endsAt).apply()
    }

    fun getDurationMinutes(): Int = prefs.getInt(KEY_DURATION, DEFAULT_DURATION_MINUTES)

    fun setDurationMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DURATION, minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)).apply()
    }

    /**
     * Le posizioni libere, cioè quelle che scegli tu. Null finché non sono mai state impostate:
     * serve a distinguere "non ho ancora scelto" (e allora si semina il default) da "ho scelto
     * di lasciarne una vuota".
     */
    fun getChosen(): List<ComponentName>? {
        val raw = prefs.getStringSet(KEY_CHOSEN, null) ?: return null
        // Ordine salvato a parte: uno StringSet non lo conserva, e l'elenco in DUMB deve
        // restare sempre nello stesso ordine — spostarsi sotto le dita è esattamente il tipo
        // di sorpresa che questa modalità deve evitare.
        val order = prefs.getString(KEY_CHOSEN_ORDER, "")!!.split("\n").filter { it.isNotBlank() }
        val byKey = raw.mapNotNull { ComponentName.unflattenFromString(it) }.associateBy { it.flattenToString() }
        return order.mapNotNull { byKey[it] }
    }

    fun setChosen(components: List<ComponentName>) {
        prefs.edit()
            .putStringSet(KEY_CHOSEN, components.map { it.flattenToString() }.toSet())
            .putString(KEY_CHOSEN_ORDER, components.joinToString("\n") { it.flattenToString() })
            .apply()
    }

    /** Quante volte hai interrotto una sessione prima della scadenza, da sempre. */
    fun getEarlyExits(): Int = prefs.getInt(KEY_EXITS, 0)

    fun setEarlyExits(count: Int) {
        prefs.edit().putInt(KEY_EXITS, count).apply()
    }

    companion object {
        const val DEFAULT_DURATION_MINUTES = 60
        const val MIN_MINUTES = 15
        const val MAX_MINUTES = 480
        val PRESET_MINUTES = listOf(30, 60, 120, 240)

        /** Quante posizioni scegli tu, oltre a telefono e messaggi che ci sono sempre. */
        const val CHOSEN_SLOTS = 3

        /** I package con cui si semina la prima volta, nell'ordine in cui l'utente li ha
         * chiesti. Se uno non è installato la posizione resta libera. */
        val DEFAULT_PACKAGES = listOf("com.whatsapp", "com.amazon.mp3")

        /** La fotocamera cambia package su ogni ROM, quindi si cerca per sottostringa invece
         * che per nome esatto. */
        const val CAMERA_HINT = "camera"

        private const val KEY_ENDS_AT = "ends_at"
        private const val KEY_DURATION = "duration_minutes"
        private const val KEY_CHOSEN = "chosen"
        private const val KEY_CHOSEN_ORDER = "chosen_order"
        private const val KEY_EXITS = "early_exits"
    }
}

package com.hiddenlayer.launcher.data

import android.content.ComponentName
import android.content.Context

/**
 * La modalità DUMB: quali app restano raggiungibili, per quanto, e quante volte hai ceduto.
 *
 * **Le cinque posizioni sono tutte tue.** Nella prima versione le prime due — telefono e
 * messaggi — non stavano qui: erano risolte a runtime dal dialer e dall'app SMS predefiniti
 * di sistema, e non si potevano cambiare. Il ragionamento era che salvarle sarebbe stato
 * fragile (cambi app di messaggistica predefinita e ti ritrovi una modalità di autodisciplina
 * che non ti fa più scrivere a nessuno), ma il prezzo era peggiore: se il tuo telefono o i
 * tuoi messaggi non sono quelli che il sistema considera predefiniti, non c'era modo di
 * dirlo. Ora i predefiniti di sistema restano il **seme** — la prima volta si trovano già lì
 * dentro — e da lì in poi decidi tu.
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
     * Le cinque posizioni, in ordine; `null` dove è vuota. **Tutto null** se non sono mai
     * state impostate: serve a distinguere "non ho ancora scelto" — e allora si semina il
     * default — da "ho scelto di lasciarle vuote".
     *
     * Salvate come una stringa sola separata da a capo, non come `StringSet`: un set non
     * conserva l'ordine, e l'elenco in DUMB deve restare sempre lo stesso — spostarsi sotto
     * le dita è esattamente il tipo di sorpresa che questa modalità deve evitare. Una riga
     * vuota è una posizione libera, ed è il motivo per cui non basta una lista di nomi.
     */
    fun getSlots(): List<ComponentName?>? {
        val raw = prefs.getString(KEY_SLOTS, null) ?: return null
        val parsed = raw.split("\n").map { ComponentName.unflattenFromString(it) }
        return List(SLOT_COUNT) { parsed.getOrNull(it) }
    }

    fun setSlots(slots: List<ComponentName?>) {
        val text = List(SLOT_COUNT) { slots.getOrNull(it) }
            .joinToString("\n") { it?.flattenToString() ?: "" }
        prefs.edit().putString(KEY_SLOTS, text).apply()
    }

    /**
     * Le tre scelte del modello precedente, se ci sono ancora.
     *
     * Serve solo a non far ritrovare le posizioni vuote a chi aggiorna da una versione in cui
     * telefono e messaggi erano fissi: quelle tre finiscono nelle ultime tre posizioni nuove.
     * Si può togliere quando nessuno aggiorna più da lì.
     */
    fun legacyChosen(): List<ComponentName>? {
        val raw = prefs.getStringSet(KEY_LEGACY_CHOSEN, null) ?: return null
        val order = prefs.getString(KEY_LEGACY_ORDER, "")!!.split("\n").filter { it.isNotBlank() }
        val byKey = raw.mapNotNull { ComponentName.unflattenFromString(it) }
            .associateBy { it.flattenToString() }
        return order.mapNotNull { byKey[it] }
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

        /** Quante app hai a disposizione in DUMB. */
        const val SLOT_COUNT = 5

        /** I package con cui si seminano le ultime tre posizioni la prima volta, nell'ordine
         * in cui l'utente li ha chiesti. Le prime due si seminano invece col dialer e con
         * l'app SMS predefiniti di sistema (vedi `LauncherViewModel`). Se qualcosa non è
         * installato, la posizione resta libera. */
        val DEFAULT_PACKAGES = listOf("com.whatsapp", "com.amazon.mp3")

        /** La fotocamera cambia package su ogni ROM, quindi si cerca per sottostringa invece
         * che per nome esatto. */
        const val CAMERA_HINT = "camera"

        private const val KEY_ENDS_AT = "ends_at"
        private const val KEY_DURATION = "duration_minutes"
        private const val KEY_SLOTS = "slots"
        private const val KEY_EXITS = "early_exits"

        // Il modello precedente: tre posizioni scelte, con l'ordine a parte.
        private const val KEY_LEGACY_CHOSEN = "chosen"
        private const val KEY_LEGACY_ORDER = "chosen_order"
    }
}

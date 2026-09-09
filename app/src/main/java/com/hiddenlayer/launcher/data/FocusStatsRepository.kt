package com.hiddenlayer.launcher.data

import android.content.Context

/** Quante volte hai aperto un'app nonostante il blocco, e quando è successo l'ultima volta. */
data class FocusBreak(
    val packageName: String,
    val count: Int,
    val lastAtMillis: Long
)

/**
 * Lo storico della Concentrazione: quante volte cedi, con quali app, e il tratto più lungo
 * passato sotto blocco senza aprire niente.
 *
 * Due scelte che vale la pena ricordare:
 *
 * - **Il record si azzera a ogni cedimento**, non a ogni sessione: misura "quanto riesco a
 *   resistere di fila", che è la cosa che si può battere. Una sessione finita senza cedimenti
 *   vale per intero.
 * - **Gli orari sono assoluti**, come la scadenza della sessione: il tratto in corso
 *   sopravvive a MIUI che chiude il launcher, che è la norma e non l'eccezione.
 *
 * Le app nascoste non entrano mai qui: il loro nome finirebbe nella classifica dentro le
 * impostazioni della Concentrazione, che non sono protette, e sarebbe una falla nel senso
 * stesso dell'area riservata. Il filtro è sia in scrittura sia in lettura, perché un'app può
 * essere nascosta dopo aver già accumulato aperture.
 */
class FocusStatsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("focus_stats", Context.MODE_PRIVATE)

    fun getBreaks(): List<FocusBreak> =
        (prefs.getStringSet(KEY_BREAKS, emptySet()) ?: emptySet())
            .mapNotNull(::decode)
            .sortedWith(compareByDescending<FocusBreak> { it.count }.thenByDescending { it.lastAtMillis })

    fun getRecordSeconds(): Int = prefs.getInt(KEY_RECORD, 0)

    fun getSessionCount(): Int = prefs.getInt(KEY_SESSIONS, 0)

    /** Da quando dura il tratto di resistenza in corso, o 0 se non ce n'è uno.
     *
     * Esposto come **istante di inizio** e non solo come durata perché la schermata della
     * sessione in corso lo mostra mentre scorre: un campo con la durata dentro `uiState`
     * ticchetterebbe una volta al secondo e farebbe ricomporre tutta l'app (vedi il commento
     * su `focusRemainingSeconds`), mentre un istante fisso cambia solo quando cedi. La
     * sottrazione la fa chi disegna, che sta già ricomponendo per conto suo. */
    fun getStreakStartMillis(): Long = prefs.getLong(KEY_STREAK_START, 0L)

    /** Da quanto stai resistendo adesso; 0 se non c'è nessuna sessione in corso. */
    fun currentStreakSeconds(nowMillis: Long): Int {
        val start = prefs.getLong(KEY_STREAK_START, 0L)
        if (start <= 0L || nowMillis <= start) return 0
        return ((nowMillis - start) / 1000L).toInt()
    }

    fun onSessionStarted(nowMillis: Long) {
        prefs.edit()
            .putLong(KEY_STREAK_START, nowMillis)
            .putInt(KEY_SESSIONS, getSessionCount() + 1)
            .apply()
    }

    /** Hai aperto un'app bloccata: il tratto si chiude, il record si aggiorna se è il caso, e
     * il conto riparte da adesso — la sessione continua. */
    fun onBreak(packageName: String, nowMillis: Long) {
        val updated = getBreaks()
            .associateBy { it.packageName }
            .toMutableMap()
        val existing = updated[packageName]
        updated[packageName] = FocusBreak(
            packageName = packageName,
            count = (existing?.count ?: 0) + 1,
            lastAtMillis = nowMillis
        )

        prefs.edit()
            .putStringSet(KEY_BREAKS, updated.values.map(::encode).toSet())
            .putInt(KEY_RECORD, maxOf(getRecordSeconds(), currentStreakSeconds(nowMillis)))
            .putLong(KEY_STREAK_START, nowMillis)
            .apply()
    }

    /**
     * Fine della sessione, per scadenza o per scelta. `endedAtMillis` è quando è finita
     * davvero, non quando ce ne siamo accorti: una sessione che scade mentre il launcher è
     * chiuso viene chiusa al rientro usando il suo orario di fine, altrimenti proprio le
     * sessioni portate a termine senza mai cedere — cioè i record migliori — non verrebbero
     * mai contate.
     */
    fun onSessionEnded(endedAtMillis: Long) {
        prefs.edit()
            .putInt(KEY_RECORD, maxOf(getRecordSeconds(), currentStreakSeconds(endedAtMillis)))
            .putLong(KEY_STREAK_START, 0L)
            .apply()
    }

    /** Azzera tutto. Se una sessione è in corso il tratto di resistenza riparte da adesso
     * invece di sparire: `clear()` cancellava anche l'orario di inizio, e per tutto il resto
     * della sessione il popup avrebbe detto "stai resistendo da meno di un minuto" e il
     * record non si sarebbe più aggiornato. */
    fun reset(sessionActive: Boolean, nowMillis: Long) {
        prefs.edit().clear().apply()
        if (sessionActive) {
            prefs.edit().putLong(KEY_STREAK_START, nowMillis).apply()
        }
    }

    private fun encode(entry: FocusBreak): String =
        "${entry.packageName}$SEPARATOR${entry.count}$SEPARATOR${entry.lastAtMillis}"

    private fun decode(raw: String): FocusBreak? {
        val parts = raw.split(SEPARATOR)
        if (parts.size != 3) return null
        val count = parts[1].toIntOrNull() ?: return null
        val lastAt = parts[2].toLongOrNull() ?: return null
        return FocusBreak(parts[0], count, lastAt)
    }

    private companion object {
        // I nomi dei package non contengono la barra verticale, quindi basta come separatore.
        const val SEPARATOR = "|"
        const val KEY_BREAKS = "breaks"
        const val KEY_RECORD = "record_seconds"
        const val KEY_SESSIONS = "session_count"
        const val KEY_STREAK_START = "streak_started_at"
    }
}

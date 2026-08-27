package com.hiddenlayer.launcher.data

import android.content.Context

/**
 * L'attenuazione extra dello schermo: se è accesa e quanto.
 *
 * Sopravvive al riavvio del launcher perché è una cosa che si lascia accesa per ore — di notte,
 * a letto — e MIUI chiude volentieri i processi in background: tenendo lo stato solo in memoria
 * si spegnerebbe da sola proprio nei casi in cui serve.
 */
class DimRepository(context: Context) {

    private val prefs = context.getSharedPreferences("screen_dim", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Quanto nero sopra tutto il resto, 0..[MAX_LEVEL]. */
    fun getLevel(): Float = prefs.getFloat(KEY_LEVEL, DEFAULT_LEVEL).coerceIn(MIN_LEVEL, MAX_LEVEL)

    fun setLevel(value: Float) {
        prefs.edit().putFloat(KEY_LEVEL, value.coerceIn(MIN_LEVEL, MAX_LEVEL)).apply()
    }

    companion object {
        const val DEFAULT_LEVEL = 0.4f

        const val MIN_LEVEL = 0f

        /**
         * Il massimo non arriva a 1 apposta.
         *
         * Il velo non riceve i tocchi, quindi non si può spegnere toccandolo: se fosse
         * completamente opaco lo schermo diventerebbe nero e non ci sarebbe più modo di
         * vedere dove premere per riaccenderlo. Deve restare sempre abbastanza leggibile da
         * poterlo togliere.
         */
        const val MAX_LEVEL = 0.9f

        private const val KEY_ENABLED = "enabled"
        private const val KEY_LEVEL = "level"
    }
}

package com.hiddenlayer.launcher.data

import android.content.Context

/**
 * La modalità rossa da astrofotografia: quanto rosso e quanto buio, e se è accesa.
 *
 * Le due manopole sono separate apposta. Il rosso serve a togliere il blu, che è quello che
 * rompe l'adattamento al buio; l'attenuazione serve ad abbassare la luce totale, che conta
 * quanto il colore — uno schermo rosso a piena luminosità ti brucia comunque la vista
 * adattata, e al telescopio dà fastidio anche agli altri. Servono entrambe e in dosi diverse
 * a seconda di quanto è buio il posto, quindi non si accorpano in un unico cursore.
 *
 * Sopravvive al riavvio del launcher: una sessione dura ore e MIUI uccide i processi in
 * background volentieri, quindi lo stato "acceso" non può vivere solo in memoria.
 */
class NightModeRepository(context: Context) {

    private val prefs = context.getSharedPreferences("night_mode", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Quanto velo rosso sopra tutto il resto, 0..1. */
    fun getRedIntensity(): Float =
        prefs.getFloat(KEY_RED, DEFAULT_RED).coerceIn(MIN_RED, MAX_RED)

    fun setRedIntensity(value: Float) {
        prefs.edit().putFloat(KEY_RED, value.coerceIn(MIN_RED, MAX_RED)).apply()
    }

    /** Quanto nero sopra tutto il resto, 0..1: porta lo schermo sotto il minimo di sistema. */
    fun getDimLevel(): Float =
        prefs.getFloat(KEY_DIM, DEFAULT_DIM).coerceIn(MIN_DIM, MAX_DIM)

    fun setDimLevel(value: Float) {
        prefs.edit().putFloat(KEY_DIM, value.coerceIn(MIN_DIM, MAX_DIM)).apply()
    }

    companion object {
        /** Alto di proposito: dentro il launcher è a 1.0 che si ottiene il rosso su nero vero,
         * quello delle app con tema rosso nativo. Sotto lo 0.8 il bianco diventa rosa. */
        const val DEFAULT_RED = 0.85f
        const val DEFAULT_DIM = 0.35f

        const val MIN_RED = 0.2f
        const val MAX_RED = 1f

        /**
         * Il cursore "Rosso" significa due cose diverse nei due meccanismi, e solo qui si
         * possono riconciliare.
         *
         * Dentro il launcher è il peso di un multiply: a 1.0 dà il rosso esatto su nero, ed è
         * esattamente quello che si vuole. Sull'overlay di sistema è invece l'opacità di un
         * velo rosso: a 1.0 darebbe uno schermo rosso pieno che copre tutto, inutilizzabile.
         * Quindi fuori dal launcher il cursore viene riscalato fin qui e non oltre.
         */
        const val OVERLAY_RED_CEILING = 0.7f

        // Il massimo non arriva a 1: un velo nero opaco renderebbe lo schermo illeggibile e
        // impossibile da recuperare, visto che l'overlay non riceve tocchi. Deve restare
        // sempre abbastanza visibile da poterlo spegnere.
        const val MIN_DIM = 0f
        const val MAX_DIM = 0.85f

        private const val KEY_ENABLED = "enabled"
        private const val KEY_RED = "red_intensity"
        private const val KEY_DIM = "dim_level"
    }
}

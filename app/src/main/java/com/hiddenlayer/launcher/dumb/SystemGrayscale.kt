package com.hiddenlayer.launcher.dumb

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Mette **tutto il telefono** in scala di grigi, non solo il launcher.
 *
 * È la parte che conta davvero: una schermata DUMB grigia dura quanto ci metti ad aprire
 * WhatsApp, e dentro WhatsApp torna tutto a colori. Il colore è metà del motivo per cui il
 * telefono attira; toglierlo ovunque è ciò che rende la modalità qualcosa di più di una
 * schermata sobria.
 *
 * Si passa dal daltonizzatore di sistema, in modalità monocromatica — la stessa cosa che fa
 * l'accessibilità. Scriverci richiede `WRITE_SECURE_SETTINGS`, che è un permesso di sistema:
 * non si può chiedere con una richiesta a comparsa, si concede una volta sola da computer con
 *
 *     adb shell pm grant com.hiddenlayer.launcher android.permission.WRITE_SECURE_SETTINGS
 *
 * **Senza quel permesso non succede niente e non è un errore**: la modalità DUMB funziona
 * lo stesso, semplicemente il grigio resta dentro il launcher. Per questo qui non si solleva
 * mai: `isAvailable` serve a *dirlo* nelle impostazioni, invece di lasciar credere che sia
 * rotto.
 */
object SystemGrayscale {

    /** Il daltonizzatore in modalità monocromatica: il valore che usa l'accessibilità di
     * sistema per "scala di grigi". */
    private const val MODE_MONOCHROMACY = 0

    /** Il valore che il daltonizzatore aveva prima di entrare in DUMB, per rimetterlo com'era
     * uscendo: se qualcuno lo usa davvero per daltonismo, questa modalità non deve
     * riconfigurargli il telefono. -1 = "non l'abbiamo mai toccato". */
    private const val KEY_PREVIOUS_MODE = "previous_daltonizer"
    private const val KEY_PREVIOUS_ENABLED = "previous_daltonizer_enabled"
    private const val NOT_SAVED = Int.MIN_VALUE

    private const val SETTING_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val SETTING_MODE = "accessibility_display_daltonizer"

    fun isAvailable(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun enable(context: Context) {
        if (!isAvailable(context)) return
        runCatching {
            val prefs = context.getSharedPreferences("dumb_mode", Context.MODE_PRIVATE)
            // Salvato solo se non c'è già: entrando in DUMB due volte di fila senza uscire
            // (riavvio del launcher a sessione in corso) il secondo salvataggio registrerebbe
            // il grigio come "stato precedente", e uscendo resterebbe grigio per sempre.
            if (prefs.getInt(KEY_PREVIOUS_MODE, NOT_SAVED) == NOT_SAVED) {
                prefs.edit()
                    .putInt(KEY_PREVIOUS_MODE, Settings.Secure.getInt(context.contentResolver, SETTING_MODE, MODE_MONOCHROMACY))
                    .putInt(KEY_PREVIOUS_ENABLED, Settings.Secure.getInt(context.contentResolver, SETTING_ENABLED, 0))
                    .apply()
            }
            Settings.Secure.putInt(context.contentResolver, SETTING_MODE, MODE_MONOCHROMACY)
            Settings.Secure.putInt(context.contentResolver, SETTING_ENABLED, 1)
        }
    }

    fun disable(context: Context) {
        if (!isAvailable(context)) return
        runCatching {
            val prefs = context.getSharedPreferences("dumb_mode", Context.MODE_PRIVATE)
            val previousMode = prefs.getInt(KEY_PREVIOUS_MODE, NOT_SAVED)
            val previousEnabled = prefs.getInt(KEY_PREVIOUS_ENABLED, 0)
            if (previousMode != NOT_SAVED) {
                Settings.Secure.putInt(context.contentResolver, SETTING_MODE, previousMode)
            }
            Settings.Secure.putInt(context.contentResolver, SETTING_ENABLED, previousEnabled)
            prefs.edit().remove(KEY_PREVIOUS_MODE).remove(KEY_PREVIOUS_ENABLED).apply()
        }
    }
}

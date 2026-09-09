package com.hiddenlayer.launcher.dim

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.provider.Settings

/**
 * L'attenuazione fatta **dal display**, non da un velo sovrapposto.
 *
 * Serve a risolvere l'unico limite che il velo non può superare: un overlay di un'app normale
 * (`TYPE_APPLICATION_OVERLAY`) sta in un livello di finestre **sotto** la barra di stato e
 * quella di navigazione. Il velo copre tutta l'area dello schermo, ma il sistema disegna le
 * sue barre sopra: l'orologio, la batteria e i tasti restano a piena luminosità. Non è un bug
 * da correggere nel velo, è dove finisce quello che un overlay può fare.
 *
 * Da Android 12 esiste "Riduci luminosità" (*Reduce Bright Colors*), la stessa cosa che
 * l'accessibilità chiama "Luminosità extra": agisce nella pipeline del display, quindi
 * **scurisce tutto, barre di sistema comprese**, e non ha nessuno dei limiti dell'overlay —
 * non sparisce sulle app bancarie, non viene ucciso dal risparmio energetico, e non c'è una
 * finestra da tenere in piedi.
 *
 * Si accende scrivendo in `Settings.Secure`, che richiede `WRITE_SECURE_SETTINGS`: lo stesso
 * permesso di sistema già usato dalla scala di grigi della modalità DUMB, che si concede una
 * volta sola da computer con
 *
 *     adb shell pm grant com.hiddenlayer.launcher android.permission.WRITE_SECURE_SETTINGS
 *
 * Quando non è disponibile — permesso non concesso, Android più vecchio, o ROM che non
 * implementa la funzione — [isAvailable] torna false e si continua col velo di prima. Il velo
 * resta la strada normale, questa è la strada migliore quando c'è.
 */
object SystemDim {

    private const val SETTING_ACTIVATED = "reduce_bright_colors_activated"
    private const val SETTING_LEVEL = "reduce_bright_colors_level"

    /**
     * Vere tutte e tre, o non se ne fa niente:
     *
     * 1. Android 12+, perché prima la funzione non esiste;
     * 2. `WRITE_SECURE_SETTINGS` concesso;
     * 3. **la ROM la implementa davvero.** Questo terzo controllo è quello che sembra
     *    superfluo e non lo è: scrivere in `Settings.Secure` riesce sempre, anche se poi
     *    nessuno legge quella chiave. Senza guardare il flag di configurazione del sistema,
     *    su una ROM che non ha la funzione spegneremmo il velo per accendere qualcosa che non
     *    esiste — e lo schermo resterebbe semplicemente com'era, senza nessun errore da
     *    nessuna parte.
     */
    fun isAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val granted = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return false
        return supportedByRom()
    }

    /** Il flag interno con cui il sistema dichiara di avere "Riduci luminosità". Letto per
     * nome dalle risorse di sistema invece che con la reflection su una API nascosta: se il
     * nome non esiste, `getIdentifier` torna 0 e rispondiamo no. */
    private fun supportedByRom(): Boolean = runCatching {
        val resources = Resources.getSystem()
        val id = resources.getIdentifier("config_reduceBrightColorsAvailable", "bool", "android")
        id != 0 && resources.getBoolean(id)
    }.getOrDefault(false)

    /**
     * [level] è lo stesso 0..1 del velo. Il sistema vuole invece una percentuale di
     * *intensità*: il massimo del velo (0.9) diventa il massimo del sistema (100).
     */
    fun enable(context: Context, level: Float) {
        if (!isAvailable(context)) return
        runCatching {
            val percent = (level / MAX_VEIL_LEVEL * 100f).toInt().coerceIn(0, 100)
            Settings.Secure.putInt(context.contentResolver, SETTING_LEVEL, percent)
            Settings.Secure.putInt(context.contentResolver, SETTING_ACTIVATED, 1)
        }
    }

    /** Spegne. Non tocca il livello: se l'utente riaccende dalle impostazioni di sistema,
     * ritrova l'intensità che aveva scelto lì. */
    fun disable(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        runCatching {
            Settings.Secure.putInt(context.contentResolver, SETTING_ACTIVATED, 0)
        }
    }

    /** Il massimo del cursore del launcher (`DimRepository.MAX_LEVEL`), ripetuto qui per non
     * far dipendere questo file dai repository. */
    private const val MAX_VEIL_LEVEL = 0.9f
}

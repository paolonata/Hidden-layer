package com.hiddenlayer.launcher.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Everything about the vault: which apps are in it, and whether opening it requires an
 * unlock. Nothing leaves the device — it is all stored client-side and encrypted at rest
 * through the Android Keystore (AES256-GCM), so another app can't read the list without
 * root. The PIN itself is never stored, only a salted SHA-256 hash of it.
 */
class HiddenAppsRepository(private val context: Context) {

    /**
     * Costruito alla prima lettura e non nel costruttore, e mai senza rete di sicurezza.
     *
     * `EncryptedSharedPreferences.create()` parla col Keystore hardware e legge un file: è
     * lavoro sincrono di decine di millisecondi, che nel costruttore veniva pagato dentro
     * `onCreate` dell'activity home, prima del primo frame.
     *
     * Ma il motivo vero è un altro: quella chiamata può lanciare, e succede davvero (chiave
     * del Keystore invalidata da un cambio di blocco schermo, archivio corrotto,
     * `AEADBadTagException`). Risalendo dal costruttore del ViewModel avrebbe fatto crashare
     * MainActivity a ogni avvio — e siccome questo È il launcher predefinito, il telefono
     * sarebbe rimasto in un ciclo di crash sulla home, senza una schermata da cui
     * disinstallare. Meglio degradare: nessuna app nascosta, e le impostazioni tornano
     * modificabili, piuttosto che rendere il telefono inutilizzabile.
     */
    private var prefs: SharedPreferences? = null
    private var initialised = false

    private fun prefs(): SharedPreferences? {
        if (!initialised) {
            initialised = true
            prefs = runCatching {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    "hidden_apps_store",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }.onFailure {
                Log.e("HiddenLayer", "Archivio cifrato illeggibile: le app nascoste non sono disponibili", it)
            }.getOrNull()
        }
        return prefs
    }

    /** True se l'archivio cifrato non si è potuto aprire: chi lo mostra all'utente deve
     * spiegare perché l'elenco è vuoto invece di far credere che non ci sia niente. */
    fun isUnavailable(): Boolean = prefs() == null

    fun getHiddenPackages(): Set<String> =
        prefs()?.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()

    fun setHidden(packageName: String, hidden: Boolean) {
        val store = prefs() ?: return
        val current = getHiddenPackages().toMutableSet()
        if (hidden) current.add(packageName) else current.remove(packageName)
        store.edit().putStringSet(KEY_HIDDEN, current).apply()
    }

    fun isHidden(packageName: String): Boolean = packageName in getHiddenPackages()

    // --- Optional unlock ------------------------------------------------------------

    /** Off by default: the vault is reachable straight from the drawer menu until the user
     * decides they want a lock on it. */
    fun isUnlockRequired(): Boolean = prefs()?.getBoolean(KEY_REQUIRE_UNLOCK, false) ?: false

    fun isPinSet(): Boolean = prefs()?.contains(KEY_PIN_HASH) ?: false

    /** Enabling the lock always goes through setPin(), so a lock can never be switched on
     * without a way to get back in (biometrics alone could stop working). */
    fun setPin(pin: String) {
        val store = prefs() ?: return
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        store.edit()
            .putString(KEY_PIN_SALT, salt.joinToString(",") { it.toString() })
            .putString(KEY_PIN_HASH, hash(pin, salt))
            .putBoolean(KEY_REQUIRE_UNLOCK, true)
            .apply()
    }

    fun disableUnlock() {
        val store = prefs() ?: return
        store.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_REQUIRE_UNLOCK, false)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val store = prefs() ?: return false
        val saltString = store.getString(KEY_PIN_SALT, null) ?: return false
        val expected = store.getString(KEY_PIN_HASH, null) ?: return false
        val salt = saltString.split(",").map { it.toInt().toByte() }.toByteArray()
        return hash(pin, salt) == expected
    }

    private fun hash(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(pin.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val KEY_HIDDEN = "hidden_packages"
        const val KEY_REQUIRE_UNLOCK = "require_unlock"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_PIN_HASH = "pin_hash"
    }
}

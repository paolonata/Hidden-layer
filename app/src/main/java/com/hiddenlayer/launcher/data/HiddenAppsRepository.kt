package com.hiddenlayer.launcher.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Package names never leave the device: stored client-side only, encrypted at rest
 * via the Android Keystore (AES256-GCM), same mechanism used for the PIN.
 */
class HiddenAppsRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "hidden_apps_store",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getHiddenPackages(): Set<String> =
        prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()

    fun setHidden(packageName: String, hidden: Boolean) {
        val current = getHiddenPackages().toMutableSet()
        if (hidden) current.add(packageName) else current.remove(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN, current).apply()
    }

    fun isHidden(packageName: String): Boolean = packageName in getHiddenPackages()

    private companion object {
        const val KEY_HIDDEN = "hidden_packages"
    }
}

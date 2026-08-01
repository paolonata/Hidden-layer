package com.hiddenlayer.launcher.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun canUseBiometrics(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /** The PIN screen stays on-screen behind the prompt, so a failure or a dismissal just
     * leaves the user on it — there is nothing to handle on the error path. */
    fun authenticate(activity: FragmentActivity, onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            }
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Sblocca")
                .setSubtitle("Conferma la tua identità per aprire le app nascoste")
                .setNegativeButtonText("Usa PIN")
                .build()
        )
    }
}

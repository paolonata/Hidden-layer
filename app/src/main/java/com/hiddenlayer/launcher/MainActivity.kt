package com.hiddenlayer.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.hiddenlayer.launcher.auth.BiometricHelper
import com.hiddenlayer.launcher.ui.LauncherApp
import com.hiddenlayer.launcher.ui.theme.HiddenLayerTheme

class MainActivity : FragmentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContent {
            HiddenLayerTheme {
                LauncherApp(
                    viewModel = viewModel,
                    canUseBiometrics = { BiometricHelper.canUseBiometrics(this) },
                    onRequestBiometric = {
                        BiometricHelper.authenticate(this) { viewModel.onUnlockSucceeded() }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()
        // Decide se questo resume segue davvero uno sblocco (non un ritorno alla home da
        // un'altra app) e, se sei in una sessione di Concentrazione, apre il respiro breve
        // prima che la griglia sia toccabile.
        viewModel.onLauncherResumed()
    }

    /** Sblocchi ed entri direttamente in un'altra app: il launcher non è in primo piano, e il
     * respiro non deve partire alle sue spalle per poi essere già finito quando torni. */
    override fun onPause() {
        super.onPause()
        viewModel.onLauncherPaused()
    }

    /** Leaving the launcher for any reason — opening an app, screen off, task switcher —
     * closes the vault, so it is never left open behind your back and coming back always
     * lands on the home screen.
     *
     * Un cambio di configurazione (rotazione, tema di sistema, lingua) passa da qui pur non
     * essendo un'uscita: senza questo controllo bastava girare lo schermo per ritrovarsi
     * davanti alla richiesta di PIN. */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) viewModel.lockVault()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.backToHome()
        }
    }
}

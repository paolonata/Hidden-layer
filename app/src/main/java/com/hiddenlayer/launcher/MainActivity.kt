package com.hiddenlayer.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.hiddenlayer.launcher.auth.BiometricHelper
import com.hiddenlayer.launcher.ui.LauncherApp
import com.hiddenlayer.launcher.ui.theme.HiddenLayerTheme

class MainActivity : FragmentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // **Edge-to-edge: la finestra arriva sotto la barra di stato e sotto quella di
        // navigazione.** Senza questa riga il sistema rimpicciolisce la finestra per stare fra
        // le due barre, e quelle due strisce non appartengono al launcher: sulla home ci si
        // vedeva lo sfondo di sistema al posto della griglia, e in DUMB restavano fuori dal
        // fondo nero — due bande chiare in cima e in fondo a una schermata che deve essere
        // tutta spenta.
        //
        // Da qui in poi **le barre non sono più padding gratuito**: ogni schermata deve
        // chiedersi i propri inset, o il contenuto ci finisce sotto. Il punto fisso è che
        // sfondi e superfici (lo sfondo sfocato, il pannello del dock, il nero di DUMB)
        // riempiono tutto lo schermo, mentre il contenuto toccabile si tiene dentro
        // `statusBarsPadding` / `navigationBarsPadding`.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Il launcher è scuro ovunque: le icone di sistema restano chiare in entrambe le
        // barre. Senza dirlo, su una ROM in tema chiaro diventerebbero nere su nero.
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

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
        // Il permesso di overlay si concede da una schermata di sistema, quindi può essere
        // cambiato mentre eravamo fuori. È anche il punto in cui il velo riparte se il
        // risparmio energetico di MIUI ha ucciso il servizio nel frattempo.
        viewModel.onOverlayPermissionChanged(Settings.canDrawOverlays(this))
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

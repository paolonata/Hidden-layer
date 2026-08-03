package com.hiddenlayer.launcher.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Marks the current screen as secure for as long as it is composed: no screenshots, and no
 * thumbnail of it in the system's recent-apps switcher. Without this, the contents of the
 * vault could show up in a screenshot or leak through the app-switcher preview — which
 * would rather defeat the point of hiding the apps in the first place.
 *
 * Il flag è **contato**, non impostato e tolto da ogni schermata per conto suo. Durante la
 * transizione fra due schermate protette (pagina nascosta → impostazioni e ritorno) le due
 * sono composte insieme per la durata dell'animazione: quella che entra accendeva il flag e
 * poi quella che usciva lo spegneva, lasciando scoperta la schermata rimasta a video.
 * Con il contatore si accende passando da 0 a 1 e si spegne solo tornando a 0.
 */
@Composable
fun SecureScreen() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val window = context.findActivity()?.window
        if (secureRequests++ == 0) {
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        onDispose {
            if (--secureRequests <= 0) {
                secureRequests = 0
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}

/** Quante schermate protette sono composte in questo momento. Sta qui e non in uno stato di
 * Compose perché deve sopravvivere alla ricomposizione e vale per l'intera finestra; ci si
 * accede solo dal thread principale. */
private var secureRequests = 0

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

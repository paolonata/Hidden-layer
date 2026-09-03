package com.hiddenlayer.launcher.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.core.graphics.drawable.toBitmap

class AppRepository(private val context: Context) {

    /** Called from a background dispatcher (see LauncherViewModel.refreshApps): decoding every
     * icon to a Bitmap here means paging/scrolling in the UI never has to do that conversion. */
    fun loadLaunchableApps(): List<AppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        return resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .map { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                AppInfo(
                    packageName = activityInfo.packageName,
                    componentName = ComponentName(activityInfo.packageName, activityInfo.name),
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    icon = resolveInfo.loadIcon(packageManager).toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX)
                )
            }
            .distinctBy { it.componentName.flattenToString() }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Il package del dialer e quello dell'app SMS predefiniti, per la modalità DUMB.
     *
     * Sono **letti dal sistema, non scelti né salvati**: le due posizioni fisse devono seguire
     * l'app che usi davvero. Salvarle vorrebbe dire che cambiando app di messaggistica
     * predefinita ti ritrovi una modalità di autodisciplina che non ti fa più scrivere a
     * nessuno, e l'unico modo di accorgertene è entrarci.
     *
     * Nessuna delle due richiede permessi né una voce in `<queries>`: tornano un nome di
     * package, e la componente da lanciare viene poi ripescata dall'elenco già caricato con
     * MAIN/LAUNCHER. Null su un dispositivo senza telefonia (tablet Wi-Fi), dove la posizione
     * resta semplicemente vuota.
     */
    fun defaultDialerPackage(): String? = runCatching {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        telecom?.defaultDialerPackage
    }.getOrNull()

    fun defaultSmsPackage(): String? = runCatching {
        Telephony.Sms.getDefaultSmsPackage(context)
    }.getOrNull()

    fun launch(component: ComponentName) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            this.component = component
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        start(intent)
    }

    fun openAppInfo(packageName: String): Boolean = start(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )

    /**
     * Chiede al sistema di disinstallare un'app. Torna false se non ci è riuscito: in quel
     * caso ripiega sulla pagina Info app, che il pulsante "Disinstalla" ce l'ha comunque, così
     * l'azione non finisce nel vuoto.
     *
     * Il ripiego serve perché i motivi per cui il sistema può rifiutare non li controlliamo:
     * app di sistema che non sono disinstallabili, ROM che sostituiscono l'installer, criteri
     * di visibilità dei package. Meglio portare l'utente a un passo dal risultato che lasciarlo
     * davanti a un menu che non fa niente.
     */
    fun requestUninstall(packageName: String): Boolean {
        val uninstall = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (start(uninstall)) return true
        openAppInfo(packageName)
        return false
    }

    /** Il selettore di sfondo di sistema. Passa da qui e non da un startActivity scritto
     * nella UI perché era l'ultimo intent rimasto senza rete: se non risolve, il processo
     * home cade e riparte, e sembra che il menu non abbia fatto niente. */
    fun openWallpaperPicker(): Boolean = start(
        Intent(Intent.ACTION_SET_WALLPAPER).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
    )

    /** La schermata di sistema per "Visualizza sopra altre app": è l'unico modo di concedere
     * SYSTEM_ALERT_WINDOW, non esiste una richiesta a comparsa. */
    fun openOverlayPermissionSettings(): Boolean = start(
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )

    /** startActivity senza far cadere il launcher: un'eccezione qui è un crash del processo
     * home, che il sistema riavvia subito — da fuori sembra che il menu non abbia fatto
     * niente, ed è esattamente il modo in cui questo bug si era presentato. */
    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    private companion object {
        const val ICON_SIZE_PX = 128
    }
}

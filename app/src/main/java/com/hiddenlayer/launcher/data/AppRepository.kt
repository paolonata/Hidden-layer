package com.hiddenlayer.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.graphics.drawable.toBitmap

class AppRepository(private val context: Context) {

    /**
     * Called from a background dispatcher (see LauncherViewModel.refreshApps): decoding every
     * icon to a Bitmap here means paging/scrolling in the UI never has to do that conversion.
     *
     * `previous` è l'elenco già in memoria, e le app che ci sono ancora vengono riusate
     * pari pari. Serve perché questo metodo gira a ogni onResume, cioè ogni volta che torni
     * al launcher: ridecodificare da capo significherebbe qualche megabyte di bitmap nuovi da
     * allocare (e altrettanti da raccogliere) a ogni chiusura di un'app, e un elenco nuovo di
     * zecca che invalida ogni memo della UI. Riusando gli oggetti, quando non è cambiato
     * niente il nuovo stato risulta uguale al precedente e non viene nemmeno emesso.
     *
     * Il compromesso: l'icona o il nome cambiati da un aggiornamento dell'app si vedono al
     * riavvio del launcher, non subito.
     */
    fun loadLaunchableApps(previous: List<AppInfo> = emptyList()): List<AppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val known = previous.associateBy { it.componentName }

        return resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .map { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                val component = ComponentName(activityInfo.packageName, activityInfo.name)
                known[component] ?: AppInfo(
                    packageName = activityInfo.packageName,
                    componentName = component,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    icon = resolveInfo.loadIcon(packageManager).toBitmap(width = ICON_SIZE_PX, height = ICON_SIZE_PX)
                )
            }
            .distinctBy { it.componentName.flattenToString() }
            .sortedBy { it.label.lowercase() }
    }

    fun launch(component: ComponentName) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            this.component = component
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun requestUninstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private companion object {
        const val ICON_SIZE_PX = 128
    }
}

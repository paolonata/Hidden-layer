package com.hiddenlayer.launcher.ui

/** mm:ss, per il countdown che scorre: pill e schermata Concentrazione lo scrivono uguale. */
fun formatFocusRemaining(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/** Durate da leggere, non da cronometrare: "1h 47m", "23 min". Usata per il record e per il
 * tratto di resistenza in corso, dove i secondi non aggiungono niente. */
fun formatFocusDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    return when {
        minutes < 1 -> "meno di un minuto"
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

/** "oggi", "ieri", "3 giorni fa": per l'ultima volta che hai ceduto con una certa app basta
 * l'ordine di grandezza, la data esatta non direbbe niente di più. */
fun formatTimeAgo(millis: Long, nowMillis: Long): String {
    val days = ((nowMillis - millis) / 86_400_000L).toInt()
    return when {
        days <= 0 -> "oggi"
        days == 1 -> "ieri"
        days < 7 -> "$days giorni fa"
        days < 14 -> "una settimana fa"
        days < 60 -> "${days / 7} settimane fa"
        else -> "${days / 30} mesi fa"
    }
}

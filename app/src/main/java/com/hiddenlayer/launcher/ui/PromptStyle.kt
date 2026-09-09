package com.hiddenlayer.launcher.ui

import androidx.compose.ui.graphics.Color
import com.hiddenlayer.launcher.ui.theme.HlBackground
import com.hiddenlayer.launcher.ui.theme.HlPaper
import com.hiddenlayer.launcher.ui.theme.HlPaper42
import com.hiddenlayer.launcher.ui.theme.HlPaper55
import com.hiddenlayer.launcher.ui.theme.HlScrim
import com.hiddenlayer.launcher.ui.theme.HlSheetShape
import com.hiddenlayer.launcher.ui.theme.HlSurface
import com.hiddenlayer.launcher.ui.theme.HlPillShape

/**
 * L'aspetto condiviso dei tre popup (scelta della durata, conferma di apertura, uscita da
 * DUMB). Stanno qui e non duplicati nei tre file perché sono lo stesso foglio: separati,
 * prima o poi divergono.
 *
 * **Sono diventati fogli ancorati in basso**, non carte centrate. Una carta al centro dello
 * schermo è una finestra di sistema: interrompe, chiede attenzione e sta dove non arriva il
 * pollice. Un foglio in basso si legge dall'alto in basso come il resto dell'interfaccia, e
 * il pulsante finisce dove la mano è già.
 *
 * La superficie resta **scura e piena**, non un velo traslucido: il velo funzionava sulla
 * home ma non sopra il cassetto, che è una griglia di icone colorate. Opaco e non "quasi":
 * con un filo di trasparenza il contrasto torna a dipendere da cosa c'è dietro, ed è già
 * stato segnalato due volte.
 */
val PromptCardShape = HlSheetShape
val PromptPillShape = HlPillShape

/** Il velo dietro il foglio: quasi opaco, così quello che c'è sotto smette di competere. */
val PromptScrim = HlScrim

val PromptSurface = HlSurface

/** Nessun bordo: la carta si stacca dal velo per luminosità, non per contorno. Il bordo era
 * un residuo di quando il velo era chiaro. */
val PromptBorder = Color.Transparent

/** Il pulsante che vuoi che si prema: carta piena, testo scuro. */
val PromptAccent = HlPaper
val PromptAccentContent = HlBackground

/** L'azione secondaria non è più un pulsante: è testo. Se una scelta va scoraggiata, il modo
 * onesto è darle meno peso, non toglierla. */
val PromptSecondaryContent = HlPaper42
val PromptDisabledContent = HlPaper42

val PromptTitle = HlPaper
val PromptBody = HlPaper55
val PromptCaption = HlPaper42

package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * L'aspetto condiviso dei due popup (scelta della durata e conferma di apertura). Stanno qui
 * e non duplicati nei due file perché sono la stessa carta: separati, prima o poi divergono.
 *
 * La superficie è **scura e piena**, non un velo traslucido. Il velo funzionava sulla home,
 * dove sotto c'è lo sfondo; sopra il cassetto, che è una griglia di icone colorate, il testo
 * bianco ci finiva dentro e non si leggeva più. Un fondo pieno tiene lo stesso carattere —
 * angoli morbidi, bordo appena accennato, niente superficie squadrata di sistema — e il
 * contrasto non dipende più da cosa c'è dietro né da quanto è colorato.
 *
 * Opaco e non "quasi": con un filo di trasparenza il contrasto torna a dipendere dallo
 * sfondo, e questo problema è già stato segnalato due volte.
 */
val PromptCardShape = RoundedCornerShape(28.dp)
val PromptPillShape = RoundedCornerShape(percent = 50)

/** Il velo dietro la carta: abbastanza scuro da staccare il popup da una griglia di icone. */
val PromptScrim = Color.Black.copy(alpha = 0.72f)

val PromptSurface = Color(0xFF16171A)
val PromptBorder = Color.White.copy(alpha = 0.16f)

/** Il pulsante che vuoi che si prema: bianco pieno, testo scuro. */
val PromptAccent = Color.White.copy(alpha = 0.94f)
val PromptAccentContent = Color(0xFF17181B)

/** Il pulsante secondario. Il contenuto disabilitato resta ben visibile: deve leggersi che
 * l'attesa sta scorrendo, non sembrare un pulsante spento. */
val PromptSecondary = Color.White.copy(alpha = 0.16f)
val PromptSecondaryBorder = Color.White.copy(alpha = 0.30f)
val PromptSecondaryContent = Color.White.copy(alpha = 0.90f)
val PromptDisabledContent = Color.White.copy(alpha = 0.55f)

val PromptTitle = Color.White
val PromptBody = Color.White.copy(alpha = 0.88f)
val PromptCaption = Color.White.copy(alpha = 0.68f)

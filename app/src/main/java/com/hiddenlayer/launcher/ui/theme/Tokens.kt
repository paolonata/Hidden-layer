package com.hiddenlayer.launcher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * La palette del launcher: **un fondo nero caldo e un solo accento**.
 *
 * Il criterio non è estetico, è funzionale al motivo per cui questo launcher esiste. Il colore
 * saturo è ciò che fa girare la testa verso lo schermo: un rosso di avviso, un verde di
 * conferma, una pill smerigliata che luccica. Qui non ce n'è nessuno — le informazioni si
 * distinguono per **peso e spazio**, non per tinta. Se serve dire che manca un permesso, lo si
 * scrive in maiuscoletto invece di colorarlo di rosso: l'informazione resta, il richiamo no.
 *
 * Il nero è caldo (`#14120F`) e non neutro: su OLED al buio un nero freddo vira all'azzurro e
 * si legge come "schermo acceso", che è esattamente l'impressione da evitare.
 */
val HlBackground = Color(0xFF14120F)

/** Il fondo delle app nascoste. Più cupo del resto, e **piatto**: quella schermata deve
 * leggersi come un altro posto, non come la home un po' più scura. */
val HlBackgroundDeep = Color(0xFF0E0D0B)

/** Il fondo della modalità DUMB, il più cupo di tutti. */
val HlBackgroundDumb = Color(0xFF0B0B0A)

/** Carte, fogli e popup. */
val HlSurface = Color(0xFF1C1913)

/** L'unico accento: testo pieno e pulsante primario. Bianco sporco, non bianco puro — al
 * buio il bianco pieno su nero abbaglia e lascia la scia. */
val HlPaper = Color(0xFFEDEAE4)
val HlPaper70 = HlPaper.copy(alpha = 0.70f)
/** Didascalie e micro-etichette. È il **minimo leggibile**: sotto questa soglia il contrasto
 * scende sotto 4.5:1 sui fondi qui sopra. Non scendere più giù per "alleggerire". */
val HlPaper55 = HlPaper.copy(alpha = 0.55f)
val HlPaper42 = HlPaper.copy(alpha = 0.42f)
val HlPaper30 = HlPaper.copy(alpha = 0.30f)

/** I filetti da 1dp che hanno preso il posto dei bordi e delle carte: separano senza
 * disegnare un contenitore. */
val HlHairline = HlPaper.copy(alpha = 0.12f)

/** Il fondo di una tile icona e quello di uno slot del dock. */
val HlTileBg = HlPaper.copy(alpha = 0.09f)
val HlDockTileBg = HlPaper.copy(alpha = 0.11f)

/** Il velo dietro i fogli. Quasi opaco: la leggibilità di un popup non deve dipendere da cosa
 * c'è dietro — problema già segnalato due volte. */
val HlScrim = Color(0xDB0A0908)

// --- Forme ---
val HlCardShape = RoundedCornerShape(20.dp)
/** Il foglio ancorato in basso: angoli più morbidi della carta, perché è più grande. */
val HlSheetShape = RoundedCornerShape(26.dp)
val HlTileShape = RoundedCornerShape(13.dp)
val HlPillShape = RoundedCornerShape(percent = 50)

// --- Misure ricorrenti ---
/** Margine di schermata nello stile "Carta". */
val HlScreenMargin = 26.dp
/** Margine di schermata nello stile "Indice" (sessione in corso, home chiusa, DUMB): più
 * largo, perché lì il vuoto è il contenuto. */
val HlWideMargin = 34.dp
/** Altezza minima toccabile per le azioni diventate testuali ("Chiudi", "Esci", "Termina
 * ora"): il testo si è alleggerito, l'area di tocco no. */
val HlTouchTarget = 44.dp

// --- Tipografia ricorrente ---
/** Le etichette di sezione: maiuscolo, piccolo, molto spaziato. Sono l'unico "ornamento"
 * rimasto, e servono a dare struttura senza disegnare intestazioni. */
val HlLabelSize = 11.sp
val HlLabelTracking = 2.2.sp

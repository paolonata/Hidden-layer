package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiddenlayer.launcher.ui.theme.HlPaper
import com.hiddenlayer.launcher.ui.theme.HlPaper42
import com.hiddenlayer.launcher.ui.theme.HlTouchTarget

data class MenuAction(val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

/**
 * Il menu che esce tenendo premuta un'icona.
 *
 * Era un `AlertDialog` di Material: superficie squadrata al centro dello schermo, titolo in
 * corpo grande e "ANNULLA" in maiuscoletto — la finestra di sistema che tutto il resto del
 * launcher ha smesso di usare. Ora è lo stesso foglio in basso dei popup, il che ha anche un
 * vantaggio pratico: un menu che esce da sotto ha le voci a portata di pollice, e questo è
 * l'unico elemento dell'interfaccia che si apre con la mano già sullo schermo.
 *
 * L'azione distruttiva non è rossa — qui non c'è nessun colore saturo — ma sta **in fondo e
 * staccata**, che è l'altro modo di dire la stessa cosa.
 */
@Composable
fun AppContextMenu(title: String, actions: List<MenuAction>, onDismiss: () -> Unit) {
    PromptSheet(onDismiss = onDismiss) {
        SectionLabel(title)
        Spacer(Modifier.height(14.dp))

        actions.forEach { action ->
            if (action.destructive) {
                Spacer(Modifier.height(6.dp))
                Hairline()
                Spacer(Modifier.height(6.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = HlTouchTarget)
                    .clickable(onClick = action.onClick),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = action.label,
                    color = if (action.destructive) HlPaper42 else HlPaper,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Hairline()
        TextAction(text = "Annulla", onClick = onDismiss, color = HlPaper42, fontSize = 14.sp)
    }
}

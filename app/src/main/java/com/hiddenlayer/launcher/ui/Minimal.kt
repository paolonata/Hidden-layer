package com.hiddenlayer.launcher.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiddenlayer.launcher.ui.theme.HlBackground
import com.hiddenlayer.launcher.ui.theme.HlHairline
import com.hiddenlayer.launcher.ui.theme.HlLabelSize
import com.hiddenlayer.launcher.ui.theme.HlLabelTracking
import com.hiddenlayer.launcher.ui.theme.HlPaper
import com.hiddenlayer.launcher.ui.theme.HlPaper42
import com.hiddenlayer.launcher.ui.theme.HlPaper55
import com.hiddenlayer.launcher.ui.theme.HlTouchTarget

/**
 * I pochi pezzi che si ripetono in tutte le schermate, tenuti in un posto solo.
 *
 * Sono deliberatamente **poveri**: un'etichetta, un filetto, un'azione scritta, un
 * interruttore. Il linguaggio di questo launcher non ha carte con ombre, pulsanti colorati e
 * icone di sistema — quelle erano proprio le cose che facevano sembrare la home un posto
 * dove c'è qualcosa da fare. Quando serve una gerarchia, la fanno il peso del testo e lo
 * spazio attorno.
 */

/** L'etichetta di sezione: maiuscolo piccolo e molto spaziato. Ha preso il posto sia dei
 * titoli delle `TopAppBar` sia delle intestazioni di sezione. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = HlPaper55
) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = HlLabelSize,
        fontWeight = FontWeight.Medium,
        letterSpacing = HlLabelTracking,
        modifier = modifier
    )
}

/**
 * Un'azione scritta invece che disegnata: "Chiudi", "Impostazioni", "Termina ora", "Esci".
 *
 * Il testo pesa poco, ma **l'area di tocco no**: il padding porta l'altezza cliccabile ad
 * almeno [HlTouchTarget]. È il punto in cui questo tipo di interfaccia si rompe di solito —
 * si alleggerisce la grafica e si finisce con bersagli da 16dp che non si prendono.
 */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = HlPaper55,
    fontSize: TextUnit = 13.sp,
    underline: Boolean = false
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = HlTouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            textDecoration = if (underline) TextDecoration.Underline else null
        )
    }
}

/** Il filetto da 1px che ha preso il posto di bordi e riquadri. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    HorizontalDivider(color = HlHairline, modifier = modifier)
}

/** Numeri e conteggi: monospazio, così le cifre stanno in colonna e una classifica si legge
 * come una tabella invece che come una frase. */
@Composable
fun MonoValue(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = HlPaper,
    fontSize: TextUnit = 15.sp,
    letterSpacing: TextUnit = 0.sp
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        letterSpacing = letterSpacing,
        modifier = modifier
    )
}

/**
 * L'intestazione di ogni schermata a pieno schermo: etichetta a sinistra, un'azione a destra.
 *
 * Ha sostituito la `TopAppBar` di Material, che portava con sé un'altezza fissa da 64dp, una
 * X in un cerchio di ripple e un titolo in corpo grande — tre cose che qui non servono a
 * nessuno. La `X` di `Icons.Default.Close` è diventata la parola "Chiudi": è più piccola
 * eppure più facile da capire, e non è un'icona in più da guardare.
 */
@Composable
fun ScreenHeader(
    label: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    labelColor: Color = HlPaper55
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SectionLabel(label, color = labelColor)
        if (actionText != null && onAction != null) {
            TextAction(actionText, onAction)
        }
    }
}

/**
 * L'interruttore, ridisegnato: una traccia e un pallino, nessun'ombra e nessun colore.
 *
 * Lo `Switch` di Material porta con sé il verde del tema, il ripple e un'icona dentro il
 * pallino. Su una schermata che elenca venti app diventava venti macchie colorate — cioè
 * venti cose che tirano l'occhio in una lista che si scorre.
 */
@Composable
fun MinimalSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val knobOffset by animateDpAsState(if (checked) 20.dp else 2.dp, label = "switch-knob")
    val knobAlpha by animateFloatAsState(if (checked) 1f else 0.30f, label = "switch-alpha")

    Box(
        modifier = modifier
            // L'area di tocco resta piena anche se il disegno è alto 24dp.
            .defaultMinSize(minWidth = HlTouchTarget, minHeight = HlTouchTarget)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) HlPaper else Color.Transparent)
                .border(
                    width = if (checked) 0.dp else 1.dp,
                    color = if (checked) Color.Transparent else HlPaper.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(start = knobOffset)
                    .size(18.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (checked) HlBackground else HlPaper)
                    .alpha(knobAlpha)
            )
        }
    }
}

/**
 * Il contorno tratteggiato di uno slot vuoto del dock.
 *
 * Ha preso il posto di `Icons.Default.Add` dentro un cerchio: un'icona piena si legge come
 * "qui c'è qualcosa", mentre un tratteggio si legge per quello che è — un posto libero. Il
 * `+` resta al centro, appena visibile, perché è l'unico modo di dire che si può toccare.
 *
 * Compose non ha un bordo tratteggiato: va disegnato a mano con un `PathEffect`, e il tratto
 * viene rientrato di mezza larghezza perché `drawRoundRect` centra la linea sul bordo e
 * altrimenti metà finirebbe fuori dalla forma.
 */
fun Modifier.dashedBorder(
    color: Color,
    shape: RoundedCornerShape,
    width: Dp = 1.dp,
    dash: Dp = 3.dp,
    gap: Dp = 3.dp
): Modifier = this.drawBehind {
    val strokePx = width.toPx()
    val radiusPx = shape.topStart.toPx(size, this)
    val inset = strokePx / 2
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - strokePx, size.height - strokePx),
        cornerRadius = CornerRadius(radiusPx, radiusPx),
        style = Stroke(
            width = strokePx,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))
        )
    )
}

/** Il pulsante primario, l'unico elemento pieno di tutta l'interfaccia: carta su nero. Ce n'è
 * al massimo uno per schermata, ed è quello che vuoi che si prema. */
@Composable
fun PrimaryPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (enabled) HlPaper else HlPaper42)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = HlBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

/** Il pulsante secondario: stessa forma, solo contorno. Usato dove l'azione è possibile ma
 * non è quella che si vuole incoraggiare — "Mi serve il telefono", per esempio. */
@Composable
fun OutlinePill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, HlPaper.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = HlPaper, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

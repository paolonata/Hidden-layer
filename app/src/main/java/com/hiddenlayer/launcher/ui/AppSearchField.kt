package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiddenlayer.launcher.ui.theme.HlPaper

private val FIELD_HEIGHT = 40.dp

/**
 * La ricerca: **una riga con un filetto sotto**, non più una pill smerigliata.
 *
 * Il vetro smerigliato (fondo bianco al 15% più bordo al 22%) era la cosa più luminosa del
 * cassetto: una capsula chiara in cima a una griglia di icone, che l'occhio prendeva per
 * prima ogni volta che si apriva. Un campo di testo non ha bisogno di un contenitore per
 * essere riconoscibile — bastano l'icona di apertura e la linea che lo sottolinea.
 *
 * Resta su `BasicTextField` e non su quello di Material: quello porta un contenitore alto
 * 56dp con etichetta fluttuante e stato di errore, cioè esattamente il tipo di scatola che
 * qui è stata tolta.
 */
@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector = Icons.Default.Search
) {
    Column(modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = HlPaper,
                fontSize = 17.sp,
                fontWeight = FontWeight.Light
            ),
            cursorBrush = SolidColor(HlPaper),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FIELD_HEIGHT)
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = HlPaper.copy(alpha = 0.40f),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(12.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = HlPaper.copy(alpha = 0.40f),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                        innerTextField()
                    }

                    // La X per svuotare è l'unica icona rimasta oltre a quella di apertura, e
                    // compare solo mentre c'è qualcosa da cancellare.
                    if (value.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancella",
                            tint = HlPaper.copy(alpha = 0.45f),
                            modifier = Modifier
                                .size(17.dp)
                                .clickable { onValueChange("") }
                        )
                    }
                }
            }
        )
        Hairline()
    }
}

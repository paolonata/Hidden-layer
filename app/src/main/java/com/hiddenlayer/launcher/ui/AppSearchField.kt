package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FIELD_HEIGHT = 46.dp
private val FIELD_SHAPE = RoundedCornerShape(percent = 50)

/**
 * Rounded frosted-glass search pill, built on BasicTextField rather than Material's
 * TextField so the height, shape and padding aren't fixed by the boxy filled/outlined
 * defaults — those bring a tall container and a heavy border that fight the translucent
 * look of the drawer.
 */
@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector = Icons.Default.Search
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.15.sp
        ),
        cursorBrush = SolidColor(Color.White),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FIELD_HEIGHT)
                    .clip(FIELD_SHAPE)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), FIELD_SHAPE)
                    .padding(horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(19.dp)
                )
                Spacer(Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.15.sp
                        )
                    }
                    innerTextField()
                }

                if (value.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancella",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier
                            .size(19.dp)
                            .clickable { onValueChange("") }
                    )
                }
            }
        }
    )
}

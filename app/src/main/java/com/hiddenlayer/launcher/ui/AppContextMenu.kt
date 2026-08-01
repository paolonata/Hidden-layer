package com.hiddenlayer.launcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

data class MenuAction(val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

@Composable
fun AppContextMenu(title: String, actions: List<MenuAction>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                actions.forEach { action ->
                    TextButton(onClick = action.onClick) {
                        Text(
                            action.label,
                            color = if (action.destructive) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

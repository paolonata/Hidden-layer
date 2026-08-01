package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hiddenlayer.launcher.LauncherUiState
import com.hiddenlayer.launcher.data.AppInfo
import com.hiddenlayer.launcher.ui.AppIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenManagerScreen(
    state: LauncherUiState,
    onAppClick: (AppInfo) -> Unit,
    onToggleHidden: (AppInfo) -> Unit,
    onDone: () -> Unit
) {
    BackHandler(onBack = onDone)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App nascoste") },
                navigationIcon = { TextButton(onClick = onDone) { Text("Chiudi") } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                Text(
                    "Tocca un'app nascosta per aprirla. Usa l'interruttore per nasconderla o mostrarla nel cassetto e nella home.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            items(state.allApps, key = { it.componentName.flattenToString() }) { app ->
                val hidden = app.packageName in state.hiddenPackages
                ListItem(
                    headlineContent = { Text(app.label) },
                    leadingContent = { AppIcon(app = app, size = 36.dp) },
                    trailingContent = {
                        Switch(checked = hidden, onCheckedChange = { onToggleHidden(app) })
                    },
                    modifier = Modifier.clickable(enabled = hidden) { onAppClick(app) }
                )
                HorizontalDivider()
            }
        }
    }
}

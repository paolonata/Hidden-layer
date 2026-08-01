package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinPromptScreen(
    error: Boolean,
    canUseBiometrics: Boolean,
    onPinSubmit: (String) -> Unit,
    onBiometricRequest: () -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)

    var pin by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (canUseBiometrics) onBiometricRequest()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Sblocca app nascoste") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6) pin = it.filter(Char::isDigit) },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = error
            )
            if (error) {
                Spacer(Modifier.height(8.dp))
                Text("PIN errato", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Row {
                TextButton(onClick = onCancel) { Text("Annulla") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onPinSubmit(pin) }) { Text("Sblocca") }
                if (canUseBiometrics) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onBiometricRequest) { Text("Impronta") }
                }
            }
        }
    }
}

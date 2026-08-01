package com.hiddenlayer.launcher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hiddenlayer.launcher.ui.BlurredWallpaperBackground
import com.hiddenlayer.launcher.ui.SecureScreen

/**
 * Shown before the vault when the user has switched the lock on. Biometrics fire straight
 * away (the common case is "touch the sensor and you're in"); the PIN field underneath is
 * always there as the fallback, which is why enabling the lock forces setting a PIN.
 */
@Composable
fun VaultUnlockScreen(
    error: Boolean,
    canUseBiometrics: Boolean,
    onBiometricRequest: () -> Unit,
    onPinSubmit: (String) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)
    SecureScreen()

    var pin by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (canUseBiometrics) onBiometricRequest()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BlurredWallpaperBackground(scrimAlpha = 0.6f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8) pin = it.filter(Char::isDigit) },
                label = { Text("PIN", color = Color.White.copy(alpha = 0.8f)) },
                singleLine = true,
                isError = error,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.7f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                    cursorColor = Color.White
                )
            )

            if (error) {
                Spacer(Modifier.height(8.dp))
                Text("PIN errato", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCancel) { Text("Annulla", color = Color.White) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onPinSubmit(pin) }) { Text("Sblocca") }
                if (canUseBiometrics) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onBiometricRequest) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("Impronta", color = Color.White)
                    }
                }
            }
        }
    }
}

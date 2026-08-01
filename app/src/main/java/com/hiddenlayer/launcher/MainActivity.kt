package com.hiddenlayer.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.hiddenlayer.launcher.auth.BiometricHelper
import com.hiddenlayer.launcher.ui.LauncherApp
import com.hiddenlayer.launcher.ui.theme.HiddenLayerTheme

class MainActivity : FragmentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContent {
            HiddenLayerTheme {
                LauncherApp(
                    viewModel = viewModel,
                    canUseBiometrics = { BiometricHelper.canUseBiometrics(this) },
                    onRequestBiometric = {
                        BiometricHelper.authenticate(
                            activity = this,
                            onSuccess = { viewModel.onBiometricSuccess() },
                            onError = { /* the PIN field on screen stays available as fallback */ }
                        )
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshApps()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.backToHome()
        }
    }
}

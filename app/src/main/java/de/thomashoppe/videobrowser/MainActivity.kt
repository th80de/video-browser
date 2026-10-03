package de.thomashoppe.videobrowser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import de.thomashoppe.videobrowser.auth.GoogleAuth
import de.thomashoppe.videobrowser.ui.VideoBrowserApp

class MainActivity : ComponentActivity() {
    private val viewModel: VideoBrowserViewModel by viewModels { VideoBrowserViewModel.factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("appearance", MODE_PRIVATE)
        setContent {
            val systemDark = isSystemInDarkTheme()
            var themeMode by remember {
                mutableStateOf(AppThemeMode.fromPreference(preferences.getString("theme_mode", null)))
            }
            val darkMode = when (themeMode) {
                AppThemeMode.System -> systemDark
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }
            MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
                val email by viewModel.signedInEmail.collectAsStateWithLifecycle()
                val signInLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        runCatching { GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java) }
                            .onSuccess { viewModel.accountChanged() }
                    }
                }
                VideoBrowserApp(
                    viewModel = viewModel,
                    email = email,
                    onSignIn = { signInLauncher.launch(GoogleAuth.client(this).signInIntent) },
                    onSignOut = { GoogleAuth.client(this).signOut().addOnCompleteListener { viewModel.accountChanged() } },
                    themeMode = themeMode,
                    onThemeModeChanged = { mode ->
                        themeMode = mode
                        preferences.edit().putString("theme_mode", mode.preferenceValue).apply()
                    },
                )
            }
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import com.example.auth.AuthManager
import com.example.data.repository.LocalHistoryRepository
import com.example.ui.screens.MainSearchScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeMode
import com.example.util.configureAppCheck
import com.example.viewmodel.AssistantViewModel
import com.example.viewmodel.SearchViewModel

class MainActivity : ComponentActivity() {

    private val searchViewModel: SearchViewModel by viewModels()
    private val assistantViewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Step 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        // Step 2: Attempt background silent auto-sign-in for authorized Google accounts
        val credentialManager = CredentialManager.create(this)
        AuthManager.attemptAutoSignIn(
            context = this,
            credentialManager = credentialManager,
            scope = lifecycleScope,
            onAuthSuccess = { /* Account signed in silently */ },
            onUnauthenticated = { /* Guest / interactive mode */ }
        )

        val localHistoryRepository = LocalHistoryRepository(applicationContext)
        val initialMode = try {
            ThemeMode.valueOf(localHistoryRepository.getSavedThemeMode())
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }

        setContent {
            var themeMode by remember { mutableStateOf(initialMode) }

            MyApplicationTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainSearchScreen(
                        searchViewModel = searchViewModel,
                        assistantViewModel = assistantViewModel,
                        currentThemeMode = themeMode,
                        onThemeModeChange = { newMode ->
                            themeMode = newMode
                            localHistoryRepository.saveThemeMode(newMode.name)
                        }
                    )
                }
            }
        }
    }
}

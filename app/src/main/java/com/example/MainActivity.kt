package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flutterforge.core.network.OAuthEventBus
import com.flutterforge.core.ui.theme.FlutterForgeTheme
import com.flutterforge.ui.navigation.FlutterForgeNavGraph
import com.flutterforge.ui.navigation.RootViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var oauthEventBus: OAuthEventBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)

        setContent {
            val rootViewModel: RootViewModel = hiltViewModel()
            val preferences by rootViewModel.preferences.collectAsStateWithLifecycle()

            FlutterForgeTheme(
                darkTheme = preferences.darkTheme,
                dynamicColor = preferences.dynamicColor
            ) {
                FlutterForgeNavGraph(
                    unauthorizedEvents = rootViewModel.unauthorizedEvents
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "pegasus" && uri.host == "oauth-callback") {
            val code = uri.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                oauthEventBus.emitCode(code)
            }
        }
    }
}

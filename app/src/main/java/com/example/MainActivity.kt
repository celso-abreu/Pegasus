package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flutterforge.core.ui.theme.FlutterForgeTheme
import com.flutterforge.ui.navigation.FlutterForgeNavGraph
import com.flutterforge.ui.navigation.RootViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
}

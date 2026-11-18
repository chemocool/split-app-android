package com.chemocool.split

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chemocool.split.data.ThemeChoice
import com.chemocool.split.ui.AppViewModel
import com.chemocool.split.ui.SplitApp
import com.chemocool.split.ui.theme.SplitTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val dark = when (settings.theme) {
                ThemeChoice.LIGHT -> false
                ThemeChoice.DARK -> true
                ThemeChoice.SYSTEM -> isSystemInDarkTheme()
            }
            LaunchedEffect(dark) {
                val bars = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            SplitTheme(darkTheme = dark) {
                SplitApp(viewModel, this)
            }
        }
    }
}

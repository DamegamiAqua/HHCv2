package com.example.cofre

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.cofre.ui.AppNav
import com.example.cofre.ui.SettingsViewModel
import com.example.cofre.data.SoundEngine
import androidx.compose.runtime.CompositionLocalProvider
import com.example.cofre.ui.LocalSoundEngine
import com.example.cofre.ui.ChestViewModel
import com.example.cofre.ui.WeeksViewModel
import com.example.cofre.ui.theme.CofreTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ChestViewModel by viewModels {
        ChestViewModel.factory((application as ChestApp).repository)
    }
    private val weeksViewModel: WeeksViewModel by viewModels {
        WeeksViewModel.factory((application as ChestApp).weekRepository, (application as ChestApp).mediaStore)
    }
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.factory((application as ChestApp).settingsStore, (application as ChestApp).mediaStore, (application as ChestApp).backupManager)
    }
    private val soundEngine by lazy { SoundEngine(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { CofreTheme { CompositionLocalProvider(LocalSoundEngine provides soundEngine) { AppNav(viewModel, weeksViewModel, settingsViewModel, (application as ChestApp).mediaStore) } } }
    }

    override fun onDestroy() {
        soundEngine.release()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        weeksViewModel.refreshToday() // detecta el cambio de lunes al volver a la app
    }
}

package com.example.cofre.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.cofre.data.SoundEngine

val LocalSoundEngine = staticCompositionLocalOf<SoundEngine> { error("SoundEngine no proporcionado") }

package com.example.cofre.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

data class AppSettings(
    val onboardingDone: Boolean = false,
    val soundsEnabled: Boolean = true,
    val effectsVolume: Float = 0.75f,
    val profilePhoto: String? = null,
    val chestBackground: String? = null,
    val chestScale: Float = 1f,
    val chestOffsetX: Float = 0f,
    val chestOffsetY: Float = 0f,
    val chestOpacity: Float = 1f,
    val weekBackground: String? = null,
    val weekScale: Float = 1f,
    val weekOffsetX: Float = 0f,
    val weekOffsetY: Float = 0f,
    val weekOpacity: Float = 1f,
    val otherBackground: String? = null,
    val otherScale: Float = 1f,
    val otherOffsetX: Float = 0f,
    val otherOffsetY: Float = 0f,
    val otherOpacity: Float = 1f,
)

/** Preferencias visuales/sonoras; no contienen reglas financieras. */
class SettingsStore(private val context: Context) {
    private object K {
        val onboarding = booleanPreferencesKey("onboarding_done")
        val sounds = booleanPreferencesKey("sounds_enabled")
        val volume = floatPreferencesKey("effects_volume")
        val profile = stringPreferencesKey("profile_photo")
        val chestBg = stringPreferencesKey("chest_background")
        val chestScale = floatPreferencesKey("chest_scale")
        val chestX = floatPreferencesKey("chest_offset_x")
        val chestY = floatPreferencesKey("chest_offset_y")
        val chestOpacity = floatPreferencesKey("chest_opacity")
        val weekBg = stringPreferencesKey("week_background")
        val weekScale = floatPreferencesKey("week_scale")
        val weekX = floatPreferencesKey("week_offset_x")
        val weekY = floatPreferencesKey("week_offset_y")
        val weekOpacity = floatPreferencesKey("week_opacity")
        val otherBg = stringPreferencesKey("other_background")
        val otherScale = floatPreferencesKey("other_scale")
        val otherX = floatPreferencesKey("other_offset_x")
        val otherY = floatPreferencesKey("other_offset_y")
        val otherOpacity = floatPreferencesKey("other_opacity")
    }

    val onboardingDone: Flow<Boolean> = context.settingsDataStore.data.map { it[K.onboarding] ?: false }
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            onboardingDone = p[K.onboarding] ?: false,
            soundsEnabled = p[K.sounds] ?: true,
            effectsVolume = (p[K.volume] ?: 0.75f).coerceIn(0f, 1f),
            profilePhoto = p[K.profile],
            chestBackground = p[K.chestBg], chestScale = p[K.chestScale] ?: 1f, chestOffsetX = p[K.chestX] ?: 0f, chestOffsetY = p[K.chestY] ?: 0f, chestOpacity = (p[K.chestOpacity] ?: 1f).coerceIn(0f, 1f),
            weekBackground = p[K.weekBg], weekScale = p[K.weekScale] ?: 1f, weekOffsetX = p[K.weekX] ?: 0f, weekOffsetY = p[K.weekY] ?: 0f, weekOpacity = (p[K.weekOpacity] ?: 1f).coerceIn(0f, 1f),
            otherBackground = p[K.otherBg], otherScale = p[K.otherScale] ?: 1f, otherOffsetX = p[K.otherX] ?: 0f, otherOffsetY = p[K.otherY] ?: 0f, otherOpacity = (p[K.otherOpacity] ?: 1f).coerceIn(0f, 1f),
        )
    }

    suspend fun setOnboardingDone() { context.settingsDataStore.edit { it[K.onboarding] = true } }
    suspend fun setSoundsEnabled(value: Boolean) { context.settingsDataStore.edit { it[K.sounds] = value } }
    suspend fun setEffectsVolume(value: Float) { context.settingsDataStore.edit { it[K.volume] = value.coerceIn(0f, 1f) } }
    suspend fun setProfilePhoto(ref: String?) { context.settingsDataStore.edit { if (ref == null) it.remove(K.profile) else it[K.profile] = ref } }

    suspend fun setBackground(section: String, ref: String?) = context.settingsDataStore.edit {
        val key = when (section) { "chest" -> K.chestBg; "week" -> K.weekBg; else -> K.otherBg }
        if (ref == null) it.remove(key) else it[key] = ref
    }
    suspend fun restore(s: AppSettings) = context.settingsDataStore.edit { p ->
        p[K.onboarding] = s.onboardingDone
        p[K.sounds] = s.soundsEnabled
        p[K.volume] = s.effectsVolume.coerceIn(0f, 1f)
        if (s.profilePhoto == null) p.remove(K.profile) else p[K.profile] = s.profilePhoto
        fun putBg(bg: String?, scale: Float, x: Float, y: Float, opacity: Float, bk: androidx.datastore.preferences.core.Preferences.Key<String>, sk: androidx.datastore.preferences.core.Preferences.Key<Float>, xk: androidx.datastore.preferences.core.Preferences.Key<Float>, yk: androidx.datastore.preferences.core.Preferences.Key<Float>, opacityKey: androidx.datastore.preferences.core.Preferences.Key<Float>) {
            if (bg == null) p.remove(bk) else p[bk] = bg
            p[sk] = scale.coerceIn(1f, 2.5f); p[xk] = x.coerceIn(-300f, 300f); p[yk] = y.coerceIn(-300f, 300f); p[opacityKey] = opacity.coerceIn(0f, 1f)
        }
        putBg(s.chestBackground, s.chestScale, s.chestOffsetX, s.chestOffsetY, s.chestOpacity, K.chestBg, K.chestScale, K.chestX, K.chestY, K.chestOpacity)
        putBg(s.weekBackground, s.weekScale, s.weekOffsetX, s.weekOffsetY, s.weekOpacity, K.weekBg, K.weekScale, K.weekX, K.weekY, K.weekOpacity)
        putBg(s.otherBackground, s.otherScale, s.otherOffsetX, s.otherOffsetY, s.otherOpacity, K.otherBg, K.otherScale, K.otherX, K.otherY, K.otherOpacity)
    }

    suspend fun setBackgroundOpacity(section: String, opacity: Float) = context.settingsDataStore.edit {
        val key = when (section) { "chest" -> K.chestOpacity; "week" -> K.weekOpacity; else -> K.otherOpacity }
        it[key] = opacity.coerceIn(0f, 1f)
    }

    suspend fun setBackgroundTransform(section: String, scale: Float, x: Float, y: Float) = context.settingsDataStore.edit {
        val (sk, xk, yk) = when (section) {
            "chest" -> Triple(K.chestScale, K.chestX, K.chestY)
            "week" -> Triple(K.weekScale, K.weekX, K.weekY)
            else -> Triple(K.otherScale, K.otherX, K.otherY)
        }
        it[sk] = scale.coerceIn(1f, 2.5f); it[xk] = x.coerceIn(-300f, 300f); it[yk] = y.coerceIn(-300f, 300f)
    }
}

package com.example.cofre.ui

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cofre.data.AppSettings
import com.example.cofre.data.BackupManager
import com.example.cofre.data.LocalMediaStore
import com.example.cofre.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.zip.ZipException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val store: SettingsStore,
    private val media: LocalMediaStore,
    private val backup: BackupManager,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setSoundsEnabled(v: Boolean) = viewModelScope.launch { store.setSoundsEnabled(v) }
    fun setVolume(v: Float) = viewModelScope.launch { store.setEffectsVolume(v) }

    fun setProfilePhoto(uri: Uri?, onError: (String?) -> Unit) = viewModelScope.launch {
        try {
            val ref = uri?.let { withContext(Dispatchers.IO) { media.copyFromUri(it, "profile") } }
            val old = settings.value.profilePhoto
            try {
                store.setProfilePhoto(ref)
            } catch (t: Throwable) {
                withContext(Dispatchers.IO) { media.delete(ref) }
                throw t
            }
            if (ref != null) withContext(Dispatchers.IO) { media.delete(old) }
            onError(null)
        } catch (t: Throwable) {
            onError(t.message ?: "No se pudo guardar la foto.")
        }
    }

    fun removeProfilePhoto() = viewModelScope.launch {
        val old = settings.value.profilePhoto
        store.setProfilePhoto(null)
        withContext(Dispatchers.IO) { media.delete(old) }
    }

    fun setBackground(section: String, uri: Uri?, onError: (String?) -> Unit) = viewModelScope.launch {
        try {
            val ref = uri?.let { withContext(Dispatchers.IO) { media.copyFromUri(it, "backgrounds") } }
            val old = when (section) { "chest" -> settings.value.chestBackground; "week" -> settings.value.weekBackground; else -> settings.value.otherBackground }
            try {
                store.setBackground(section, ref)
            } catch (t: Throwable) {
                withContext(Dispatchers.IO) { media.delete(ref) }
                throw t
            }
            if (ref != null && old != ref) withContext(Dispatchers.IO) { media.delete(old) }
            onError(null)
        } catch (t: Throwable) {
            onError(t.message ?: "No se pudo guardar el fondo.")
        }
    }

    fun resetBackground(section: String) = viewModelScope.launch {
        val old = when (section) { "chest" -> settings.value.chestBackground; "week" -> settings.value.weekBackground; else -> settings.value.otherBackground }
        store.setBackground(section, null)
        store.setBackgroundTransform(section, 1f, 0f, 0f)
        store.setBackgroundOpacity(section, 1f)
        withContext(Dispatchers.IO) { media.delete(old) }
    }

    fun setBackgroundTransform(section: String, scale: Float, x: Float, y: Float) = viewModelScope.launch { store.setBackgroundTransform(section, scale, x, y) }
    fun setBackgroundOpacity(section: String, opacity: Float) = viewModelScope.launch { store.setBackgroundOpacity(section, opacity) }
    fun export(uri: Uri, onResult: (String?) -> Unit) = viewModelScope.launch {
        try {
            backup.exportTo(uri)
            onResult(null)
        } catch (t: Throwable) {
            onResult(t.message ?: "No se pudo exportar el backup.")
        }
    }

    fun import(uri: Uri, onResult: (String?) -> Unit) = viewModelScope.launch {
        try {
            backup.importFrom(uri)
            onResult(null)
        } catch (t: Throwable) {
            onResult(importError(t))
        }
    }

    private fun importError(t: Throwable): String {
        val message = t.message.orEmpty()
        return when {
            t is ZipException || message.contains("ZIP", ignoreCase = true) || message.contains("corrupto", ignoreCase = true) -> message.ifBlank { "El archivo de backup está corrupto o no es válido." }
            message.contains("Versión de backup no soportada", ignoreCase = true) -> "La versión de este backup no es compatible con esta aplicación."
            message.contains("Falta backup.json", ignoreCase = true) || message.contains("no es un backup de Cofre Hero", ignoreCase = true) -> "El archivo no contiene una copia de seguridad válida de Cofre Hero."
            else -> message.ifBlank { "No se pudo importar el backup. Los datos actuales no se modificaron si el archivo no superó la validación." }
        }
    }

    companion object {
    fun factory(
        store: SettingsStore,
        media: LocalMediaStore,
        backup: BackupManager
    ) = viewModelFactory {
        initializer {
            SettingsViewModel(store, media, backup)
        }
    }
}
}

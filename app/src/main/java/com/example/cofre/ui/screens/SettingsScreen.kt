package com.example.cofre.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cofre.data.AppSettings
import com.example.cofre.data.LocalMediaStore
import com.example.cofre.ui.SettingsViewModel
import com.example.cofre.ui.theme.*

@Composable
fun SettingsScreen(vm: SettingsViewModel, media: LocalMediaStore, finalUnlocked: Boolean, onSecret: () -> Unit, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<String?>(null) }
    var importConfirm by remember { mutableStateOf<Uri?>(null) }
    val pickProfile = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) vm.setProfilePhoto(uri) { error = it } }
    var backgroundSection by remember { mutableStateOf<String?>(null) }
    val pickBackground = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        backgroundSection?.let { section -> if (uri != null) vm.setBackground(section, uri) { error = it } }
        backgroundSection = null
    }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> uri?.let { vm.export(it) { error = it } } }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) importConfirm = uri }

    Scaffold(containerColor = PxBg) { pad ->
        Box(Modifier.fillMaxSize()) {
            BackgroundLayer(media, s, "other", Modifier.fillMaxSize())
            Column(Modifier.padding(pad).padding(horizontal = 16.dp).fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeader("Ajustes", onBack)
            PixelCard(Modifier.fillMaxWidth()) {
                Text("PERFIL", color = PxGold, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(88.dp)) { LocalImage(media, s.profilePhoto, Modifier.fillMaxSize(), 256, ContentScale.Crop, "Foto de perfil") }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        PixelButton(if (s.profilePhoto == null) "ELEGIR FOTO" else "CAMBIAR FOTO", Modifier.fillMaxWidth(), gold = false) { pickProfile.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        if (s.profilePhoto != null) TextButton({ preview = s.profilePhoto }) { Text("Ver foto", color = PxBlueLight) }
                        if (s.profilePhoto != null) TextButton({ vm.removeProfilePhoto() }) { Text("Eliminar", color = PxDanger) }
                    }
                }
            }
            PixelCard(Modifier.fillMaxWidth()) {
                Text("SONIDOS", color = PxGold, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("SONIDOS ON/OFF", Modifier.weight(1f), color = PxText)
                    Switch(checked = s.soundsEnabled, onCheckedChange = vm::setSoundsEnabled)
                }
                Text("VOLUMEN DE EFECTOS", color = PxDim)
                Slider(value = s.effectsVolume, onValueChange = vm::setVolume, valueRange = 0f..1f)
            }
            BackgroundEditor("FONDO DEL COFRE", "chest", s, media, vm, { backgroundSection = "chest"; pickBackground.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
            BackgroundEditor("FONDO SEMANAL", "week", s, media, vm, { backgroundSection = "week"; pickBackground.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
            BackgroundEditor("OTRAS SECCIONES", "other", s, media, vm, { backgroundSection = "other"; pickBackground.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
            if (finalUnlocked) {
                PixelCard(Modifier.fillMaxWidth()) {
                    Text("RECOMPENSA ESPECIAL", color = PxGold, fontWeight = FontWeight.Bold)
                    Text("Tu progreso desbloqueó un recuerdo especial.", color = PxText, modifier = Modifier.padding(vertical = 6.dp))
                    PixelButton("REPRODUCIR FINAL", Modifier.fillMaxWidth(), gold = true, onClick = onSecret)
                }
            }
            PixelCard(Modifier.fillMaxWidth()) {
                Text("COPIAS DE SEGURIDAD", color = PxGold, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                PixelButton("EXPORTAR COPIA DE SEGURIDAD", Modifier.fillMaxWidth(), gold = true) { createBackup.launch("cofre-hero-backup.zip") }
                Spacer(Modifier.height(8.dp))
                PixelButton("IMPORTAR COPIA DE SEGURIDAD", Modifier.fillMaxWidth(), gold = false) { openBackup.launch(arrayOf("application/zip", "application/octet-stream")) }
            }
            error?.let { Text(it, color = PxDanger, fontWeight = FontWeight.Bold) }
            Text("Todo se guarda localmente en este dispositivo. No hay cuenta, servidor ni sincronización.", color = PxDim, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(24.dp))
            }
        }
    }
    preview?.let { PhotoPreviewDialog(media, it) { preview = null } }
    importConfirm?.let { uri ->
        AlertDialog(onDismissRequest = { importConfirm = null }, title = { Text("¿Importar backup?") }, text = { Text("Se reemplazarán los datos actuales por la copia. Esta acción no se puede deshacer desde la aplicación.") },
            confirmButton = { TextButton({ importConfirm = null; vm.import(uri) { error = it } }) { Text("Importar", color = PxDanger) } },
            dismissButton = { TextButton({ importConfirm = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun BackgroundEditor(title: String, section: String, s: AppSettings, media: LocalMediaStore, vm: SettingsViewModel, onPick: () -> Unit) {
    val ref = when (section) { "chest" -> s.chestBackground; "week" -> s.weekBackground; else -> s.otherBackground }
    val scale = when (section) { "chest" -> s.chestScale; "week" -> s.weekScale; else -> s.otherScale }
    val x = when (section) { "chest" -> s.chestOffsetX; "week" -> s.weekOffsetX; else -> s.otherOffsetX }
    val y = when (section) { "chest" -> s.chestOffsetY; "week" -> s.weekOffsetY; else -> s.otherOffsetY }
    val opacity = when (section) { "chest" -> s.chestOpacity; "week" -> s.weekOpacity; else -> s.otherOpacity }
    PixelCard(Modifier.fillMaxWidth()) {
        Text(title, color = PxGold, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(150.dp)) { BackgroundLayer(media, s, section, Modifier.fillMaxSize()) }
        Spacer(Modifier.height(8.dp))
        PixelButton(if (ref == null) "ELEGIR IMAGEN" else "CAMBIAR IMAGEN", Modifier.fillMaxWidth(), gold = false, onClick = onPick)
        if (ref != null) {
            Text("OPACIDAD · ${(opacity * 100f).roundToInt()}%", color = PxDim)
            Slider(value = opacity, onValueChange = { vm.setBackgroundOpacity(section, it) }, valueRange = 0.08f..1f)
            Text("ESCALA", color = PxDim)
            Slider(value = scale, onValueChange = { vm.setBackgroundTransform(section, it, x, y) }, valueRange = 1f..2.5f)
            Text("POSICIÓN HORIZONTAL", color = PxDim)
            Slider(value = x, onValueChange = { vm.setBackgroundTransform(section, scale, it, y) }, valueRange = -300f..300f)
            Text("POSICIÓN VERTICAL", color = PxDim)
            Slider(value = y, onValueChange = { vm.setBackgroundTransform(section, scale, x, it) }, valueRange = -300f..300f)
            TextButton({ vm.resetBackground(section) }) { Text("RESTAURAR FONDO PREDETERMINADO", color = PxDanger) }
        }
    }
}

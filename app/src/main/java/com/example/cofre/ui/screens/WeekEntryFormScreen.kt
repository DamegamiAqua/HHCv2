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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cofre.core.DateFmt
import com.example.cofre.core.Money
import com.example.cofre.data.LocalMediaStore
import com.example.cofre.ui.theme.*
import com.example.cofre.ui.LocalSoundEngine
import com.example.cofre.data.SoundEvent

enum class WeekEntryKind { EXPENSE, TRANSFER }

@Composable
fun WeekEntryFormScreen(
    kind: WeekEntryKind,
    title: String,
    submitLabel: String,
    isNew: Boolean,
    weekStart: Long,
    maxCents: Long,
    initialCents: Long?,
    initialConcept: String,
    initialAt: Long,
    initialPhotoRef: String?,
    media: LocalMediaStore,
    onBack: () -> Unit,
    onCopyPhoto: (Uri, (String?, String?) -> Unit) -> Unit,
    onSubmit: (cents: Long, concept: String, at: Long, photoRef: String?, onError: (String?) -> Unit) -> Unit,
) {
    var amount by rememberSaveable { mutableStateOf(initialCents?.let { Money.toInput(it) } ?: "") }
    var concept by rememberSaveable { mutableStateOf(initialConcept) }
    var at by rememberSaveable { mutableStateOf(initialAt) }
    var photoRef by rememberSaveable { mutableStateOf(initialPhotoRef) }
    var originalPhotoRef by rememberSaveable { mutableStateOf(initialPhotoRef) }
    var newPhotoRefs by remember { mutableStateOf(emptyList<String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmCents by remember { mutableStateOf<Long?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var showPhoto by remember { mutableStateOf(false) }
    val sound = LocalSoundEngine.current
    var saving by remember { mutableStateOf(false) }
    val transfer = kind == WeekEntryKind.TRANSFER
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onCopyPhoto(uri) { ref, msg ->
            if (ref != null) { photoRef = ref; newPhotoRefs = newPhotoRefs + ref; error = null }
            else error = msg
        }
    }

    fun cleanupPending() { newPhotoRefs.forEach(media::delete) }
    fun finishBack() { cleanupPending(); onBack() }

    fun submit(cents: Long) {
        saving = true
        onSubmit(cents, concept, at, photoRef) { msg ->
            saving = false
            if (msg == null) {
                if (originalPhotoRef != null && originalPhotoRef != photoRef) media.delete(originalPhotoRef)
                newPhotoRefs.filter { it != photoRef }.forEach(media::delete)
                newPhotoRefs = emptyList()
                originalPhotoRef = photoRef
                onBack()
            } else {
                error = msg
                newPhotoRefs.forEach(media::delete)
                newPhotoRefs = emptyList()
            }
        }
    }

    fun onTapSave() {
        val cents = Money.parse(amount)
        when {
            cents == null || cents <= 0 -> error = "Ingresa una cantidad válida mayor a cero."
            cents > maxCents -> error = "Máximo disponible en la semana: ${Money.format(maxCents)}."
            transfer && isNew -> confirmCents = cents
            else -> submit(cents)
        }
    }

    Scaffold(containerColor = PxBg) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 20.dp).fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
            ScreenHeader(title, ::finishBack)
            Text("Disponible para este movimiento: ${Money.format(maxCents)}", color = PxBlueLight, modifier = Modifier.padding(bottom = 8.dp))
            PixelField(amount, { amount = it; error = null }, "Cantidad (MXN)", decimal = true)
            Spacer(Modifier.height(12.dp))
            PixelField(concept, { concept = it }, if (transfer) "Concepto (opcional)" else "Concepto (libre)")
            if (!transfer) {
                Spacer(Modifier.height(12.dp))
                PixelCard(Modifier.fillMaxWidth()) {
                    Text("FOTOGRAFÍA", color = PxGold, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (photoRef != null) {
                        LocalImage(media, photoRef, Modifier.fillMaxWidth().height(150.dp), 512, ContentScale.Fit)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton({ showPhoto = true }) { Text("Ver", color = PxBlueLight) }
                            TextButton({ photoRef = null }) { Text("Eliminar", color = PxDanger) }
                            TextButton({ picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Reemplazar", color = PxGold) }
                        }
                    } else {
                        PixelButton("AÑADIR FOTO", Modifier.fillMaxWidth(), gold = false) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PixelButton(DateFmt.date(at), Modifier.weight(1f), gold = false) { showDate = true }
                PixelButton(DateFmt.time(at), Modifier.weight(1f), gold = false) { showTime = true }
            }
            error?.let { Text(it, color = PxDanger, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(20.dp))
            PixelButton(submitLabel, Modifier.fillMaxWidth(), gold = transfer.not(), enabled = !saving) { onTapSave() }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPhoto && photoRef != null) PhotoPreviewDialog(media, photoRef!!) { showPhoto = false }
    confirmCents?.let { cents ->
        AlertDialog(onDismissRequest = { confirmCents = null }, title = { Text("¿Pasar al cofre?") }, text = { Text("${Money.format(cents)} saldrán de la semana y entrarán al cofre.") },
            confirmButton = { TextButton({ confirmCents = null; sound.play(SoundEvent.CONFIRM); submit(cents) }) { Text("Pasar", color = PxGold) } },
            dismissButton = { TextButton({ confirmCents = null }) { Text("Cancelar") } })
    }
    if (showDate) DatePickDialog(at, { at = it }, { showDate = false }, minEpochDay = weekStart, maxEpochDay = weekStart + 6)
    if (showTime) TimePickDialog(at, { at = it }, { showTime = false })
}

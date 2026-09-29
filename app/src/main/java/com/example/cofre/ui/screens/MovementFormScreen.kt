package com.example.cofre.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cofre.core.DateFmt
import com.example.cofre.core.Money
import com.example.cofre.data.TransactionEntity
import com.example.cofre.ui.theme.*
import com.example.cofre.ui.LocalSoundEngine
import com.example.cofre.data.SoundEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.abs

/**
 * Formulario compartido: aportación, retiro y edición.
 * - Nuevo: fecha/hora automáticas (momento de guardar).
 * - Edición: cantidad, concepto, fecha y hora editables.
 * - Retiro nuevo: pide confirmación y valida contra el saldo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementFormScreen(
    title: String,
    submitLabel: String,
    isWithdrawal: Boolean,
    editing: TransactionEntity?,
    balanceCents: Long,
    onBack: () -> Unit,
    onSubmit: (cents: Long, concept: String, at: Long, onError: (String) -> Unit) -> Unit,
) {
    var amount by rememberSaveable { mutableStateOf(editing?.let { Money.toInput(abs(it.amountCents)) } ?: "") }
    var concept by rememberSaveable { mutableStateOf(editing?.concept ?: "") }
    var at by rememberSaveable { mutableStateOf(editing?.occurredAt ?: System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmCents by remember { mutableStateOf<Long?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val sound = LocalSoundEngine.current
    var saving by remember { mutableStateOf(false) }

    fun submit(cents: Long) {
        saving = true
        val when_ = if (editing != null) at else System.currentTimeMillis()
        onSubmit(cents, concept, when_) { msg -> saving = false; error = msg }
    }

    fun onTapSave() {
        val cents = Money.parse(amount)
        when {
            cents == null || cents <= 0 -> error = "Ingresa una cantidad válida mayor a cero."
            editing == null && isWithdrawal ->
                if (cents > balanceCents) error = "No puedes retirar más que tu saldo (${Money.format(balanceCents)})."
                else confirmCents = cents
            else -> submit(cents)
        }
    }

    Scaffold(containerColor = PxBg) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 20.dp).fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
            ScreenHeader(title, onBack)
            if (isWithdrawal && editing == null) {
                Text("Disponible: ${Money.format(balanceCents)}", color = PxBlueLight, modifier = Modifier.padding(bottom = 8.dp))
            }
            PixelField(amount, { amount = it; error = null }, "Cantidad (MXN)", decimal = true)
            Spacer(Modifier.height(12.dp))
            PixelField(concept, { concept = it }, "Concepto (libre)")
            if (editing != null) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PixelButton(DateFmt.date(at), Modifier.weight(1f), gold = false) { showDate = true }
                    PixelButton(DateFmt.time(at), Modifier.weight(1f), gold = false) { showTime = true }
                }
            } else {
                Text("Fecha y hora: se registran automáticamente al guardar.", color = PxDim, modifier = Modifier.padding(top = 12.dp))
            }
            error?.let { Text(it, color = PxDanger, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(20.dp))
            PixelButton(submitLabel, Modifier.fillMaxWidth(), gold = !isWithdrawal, enabled = !saving) { onTapSave() }
            Spacer(Modifier.height(24.dp))
        }
    }

    confirmCents?.let { cents ->
        AlertDialog(
            onDismissRequest = { confirmCents = null },
            title = { Text("¿Retirar dinero?") },
            text = { Text("Vas a retirar ${Money.format(cents)} de tu cofre.") },
            confirmButton = { TextButton({ confirmCents = null; sound.play(SoundEvent.CONFIRM); submit(cents) }) { Text("Retirar", color = PxDanger) } },
            dismissButton = { TextButton({ confirmCents = null }) { Text("Cancelar") } },
        )
    }

    if (showDate) DatePickDialog(at, { at = it }, { showDate = false })
    if (showTime) TimePickDialog(at, { at = it }, { showTime = false })
}

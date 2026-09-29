package com.example.cofre.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cofre.core.DateFmt
import com.example.cofre.core.Money
import com.example.cofre.core.WeekMath
import com.example.cofre.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime

/**
 * Pide el saldo disponible de una semana. Sirve para: semana actual, semana anterior (con selector de fecha)
 * y edición del saldo inicial (initialCents != null).
 */
@Composable
fun WeekSetupScreen(
    title: String,
    prompt: String,
    initialStartEpochDay: Long,
    allowDatePick: Boolean,
    initialCents: Long?,
    onBack: () -> Unit,
    onConfirm: (startEpochDay: Long, cents: Long, onError: (String) -> Unit) -> Unit,
) {
    var amount by rememberSaveable { mutableStateOf(initialCents?.let { Money.toInput(it) } ?: "") }
    var day by rememberSaveable { mutableStateOf(initialStartEpochDay) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val start = WeekMath.mondayOf(day)

    Scaffold(containerColor = PxBg) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 20.dp).fillMaxSize().imePadding()) {
            ScreenHeader(title, onBack)
            Spacer(Modifier.height(16.dp))
            Text(prompt, color = PxText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            if (allowDatePick) {
                PixelButton("Semana: ${WeekMath.rangeLabel(start)}", Modifier.fillMaxWidth(), gold = false) { showDate = true }
                Spacer(Modifier.height(12.dp))
            } else {
                Text("Semana: ${WeekMath.rangeLabel(start)}", color = PxBlueLight, modifier = Modifier.padding(bottom = 12.dp))
            }
            PixelField(amount, { amount = it; error = null }, "Disponible (MXN)", decimal = true)
            error?.let { Text(it, color = PxDanger, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(16.dp))
            PixelButton("CONFIRMAR", Modifier.fillMaxWidth(), enabled = !saving) {
                val cents = Money.parse(amount)
                if (cents == null) error = "Ingresa una cantidad válida (por ejemplo 1500 o 1250.50)."
                else { saving = true; onConfirm(start, cents) { msg -> saving = false; error = msg } }
            }
        }
    }

    if (showDate) {
        DatePickDialog(
            atMs = DateFmt.combine(LocalDate.ofEpochDay(day), LocalTime.NOON),
            onPick = { day = DateFmt.localDate(it).toEpochDay() },
            onDismiss = { showDate = false },
            maxEpochDay = LocalDate.now().toEpochDay(),
        )
    }
}

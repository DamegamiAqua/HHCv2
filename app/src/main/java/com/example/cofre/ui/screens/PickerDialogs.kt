package com.example.cofre.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.cofre.core.DateFmt
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

/** Selector de fecha. Conserva la hora de atMs. minEpochDay/maxEpochDay limitan los días seleccionables. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickDialog(
    atMs: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
    minEpochDay: Long? = null,
    maxEpochDay: Long? = null,
) {
    val selectable = remember(minEpochDay, maxEpochDay) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                return (minEpochDay == null || d >= minEpochDay) && (maxEpochDay == null || d <= maxEpochDay)
            }
        }
    }
    val st = rememberDatePickerState(
        initialSelectedDateMillis = DateFmt.localDate(atMs).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = selectable,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({
                st.selectedDateMillis?.let { ms ->
                    val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                    onPick(DateFmt.combine(d, DateFmt.localTime(atMs)))
                }
                onDismiss()
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } },
    ) { DatePicker(st) }
}

/** Selector de hora 24 h. Conserva la fecha de atMs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickDialog(atMs: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val t = DateFmt.localTime(atMs)
    val st = rememberTimePickerState(t.hour, t.minute, true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({ onPick(DateFmt.combine(DateFmt.localDate(atMs), LocalTime.of(st.hour, st.minute))); onDismiss() }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } },
        text = { TimePicker(st) },
    )
}

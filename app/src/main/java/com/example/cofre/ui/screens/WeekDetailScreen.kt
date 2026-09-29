package com.example.cofre.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cofre.core.DateFmt
import com.example.cofre.core.Money
import com.example.cofre.core.WeekMath
import com.example.cofre.data.WeekDetail
import com.example.cofre.data.AppSettings
import com.example.cofre.data.LocalMediaStore
import com.example.cofre.data.WeekEntry
import com.example.cofre.ui.theme.*

@Composable
fun WeekDetailScreen(
    detail: WeekDetail,
    onBack: () -> Unit,
    onAddExpense: () -> Unit,
    onTransfer: () -> Unit,
    onAnalysis: () -> Unit,
    onEditInitial: () -> Unit,
    onEditEntry: (WeekEntry) -> Unit,
    onDeleteEntry: (WeekEntry, (String?) -> Unit) -> Unit,
    settings: AppSettings,
    media: LocalMediaStore,
) {
    var toDelete by remember { mutableStateOf<WeekEntry?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var previewRef by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = PxBg) { pad ->
        Box(Modifier.fillMaxSize()) {
            BackgroundLayer(media, settings, "week", Modifier.fillMaxSize())
            LazyColumn(
            Modifier.padding(pad).padding(horizontal = 16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                ScreenHeader("Semana ${WeekMath.rangeLabel(detail.week.startEpochDay)}", onBack)
                WeekSummaryGrid(detail.week.initialCents, detail.spentCents, detail.savedCents, detail.availableCents)
                TextButton(onEditInitial) { Text("Editar saldo inicial", color = PxBlueLight) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton("+ GASTO", Modifier.weight(1f), gold = true, onClick = onAddExpense)
                    PixelButton("PASAR AL COFRE", Modifier.weight(1f), gold = false,
                        enabled = detail.availableCents > 0, onClick = onTransfer)
                }
                Spacer(Modifier.height(8.dp))
                PixelButton("ANÁLISIS SEMANAL", Modifier.fillMaxWidth(), gold = false, onClick = onAnalysis)
                Spacer(Modifier.height(8.dp))
                Text("MOVIMIENTOS", color = PxGold, fontWeight = FontWeight.Bold)
                if (detail.entries.isEmpty()) Text("Sin movimientos todavía.", color = PxDim)
            }
            items(detail.entries, key = { entryKey(it) }) { entry ->
                val spend = entry is WeekEntry.Spend
                val concept = when (entry) { is WeekEntry.Spend -> entry.expense.concept; is WeekEntry.Save -> entry.transfer.concept }
                val cents = when (entry) { is WeekEntry.Spend -> entry.expense.amountCents; is WeekEntry.Save -> entry.transfer.amountCents }
                PixelCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(concept, fontWeight = FontWeight.Bold, color = PxText)
                            Text("${if (spend) "Gasto" else "→ Cofre"} · ${DateFmt.date(entry.at)} · ${DateFmt.time(entry.at)}",
                                color = PxDim, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("-" + Money.format(cents), fontWeight = FontWeight.Bold, color = if (spend) PxDanger else PxBlueLight)
                    }
                    if (spend) {
                        val ref = (entry as WeekEntry.Spend).expense.photoUri
                        if (ref != null) LocalImage(media, ref, Modifier.fillMaxWidth().height(110.dp).clickable { previewRef = ref }, 384, androidx.compose.ui.layout.ContentScale.Fit)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton({ onEditEntry(entry) }) { Text("Editar", color = PxBlueLight) }
                        TextButton({ deleteError = null; toDelete = entry }) { Text("Borrar", color = PxDanger) }
                    }
                }
            }
        }
        }
    }

    previewRef?.let { ref -> PhotoPreviewDialog(media, ref) { previewRef = null } }

    toDelete?.let { entry ->
        val isSpend = entry is WeekEntry.Spend
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(if (isSpend) "¿Borrar gasto?" else "¿Borrar transferencia?") },
            text = {
                Column {
                    Text(if (isSpend) "El dinero vuelve al saldo disponible de la semana."
                    else "Se revertirá completa: el dinero vuelve a la semana y se resta del cofre.")
                    deleteError?.let { Text(it, color = PxDanger, modifier = Modifier.padding(top = 8.dp)) }
                }
            },
            confirmButton = {
                TextButton({ onDeleteEntry(entry) { err -> if (err == null) toDelete = null else deleteError = err } }) {
                    Text("Borrar", color = PxDanger)
                }
            },
            dismissButton = { TextButton({ toDelete = null }) { Text("Cancelar") } },
        )
    }
}

private fun entryKey(e: WeekEntry): String = when (e) {
    is WeekEntry.Spend -> "e${e.expense.id}"
    is WeekEntry.Save -> "t${e.transfer.id}"
}

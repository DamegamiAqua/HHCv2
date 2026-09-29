package com.example.cofre.ui.screens

import androidx.compose.foundation.layout.*
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
import com.example.cofre.data.TransactionEntity
import com.example.cofre.data.TxType
import com.example.cofre.ui.theme.*

private fun TxType.label() = when (this) {
    TxType.INITIAL -> "Saldo inicial"
    TxType.DEPOSIT -> "Aportación"
    TxType.WITHDRAWAL -> "Retiro"
    TxType.TRANSFER_IN -> "Desde semana"
}

@Composable
fun HistoryScreen(
    transactions: List<TransactionEntity>,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long, (String?) -> Unit) -> Unit,
) {
    var toDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = PxBg) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 16.dp).fillMaxSize()) {
            ScreenHeader("Historial", onBack)
            if (transactions.isEmpty()) {
                Text("Aún no hay movimientos. ¡Agrega tu primera moneda!", color = PxDim, modifier = Modifier.padding(16.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(transactions, key = { it.id }) { tx ->
                    PixelCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tx.concept, fontWeight = FontWeight.Bold, color = PxText)
                                Text("${tx.type.label()} · ${DateFmt.date(tx.occurredAt)} · ${DateFmt.time(tx.occurredAt)}",
                                    color = PxDim, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(Money.formatSigned(tx.amountCents), fontWeight = FontWeight.Bold,
                                color = if (tx.amountCents >= 0) PxGold else PxDanger)
                        }
                        if (tx.transferId != null) {
                            Text("Ligado a una semana: edítalo o bórralo desde ahí.", color = PxDim,
                                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton({ onEdit(tx.id) }) { Text("Editar", color = PxBlueLight) }
                                TextButton({ deleteError = null; toDelete = tx }) { Text("Borrar", color = PxDanger) }
                            }
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Borrar movimiento?") },
            text = {
                Column {
                    Text("${tx.concept} (${Money.formatSigned(tx.amountCents)}). El saldo se recalculará.")
                    deleteError?.let { Text(it, color = PxDanger, modifier = Modifier.padding(top = 8.dp)) }
                }
            },
            confirmButton = {
                TextButton({ onDelete(tx.id) { err -> if (err == null) toDelete = null else deleteError = err } }) {
                    Text("Borrar", color = PxDanger)
                }
            },
            dismissButton = { TextButton({ toDelete = null }) { Text("Cancelar") } },
        )
    }
}

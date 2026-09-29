package com.example.cofre.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cofre.core.Money
import com.example.cofre.core.WeekMath
import com.example.cofre.data.WeekSummary
import com.example.cofre.data.AppSettings
import com.example.cofre.data.LocalMediaStore
import com.example.cofre.ui.theme.*

@Composable
fun WeeksHistoryScreen(
    summaries: List<WeekSummary>,
    currentMonday: Long,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onAddPast: () -> Unit,
    settings: AppSettings,
    media: LocalMediaStore,
) {
    Scaffold(containerColor = PxBg) { pad ->
        Box(Modifier.fillMaxSize()) {
            BackgroundLayer(media, settings, "week", Modifier.fillMaxSize())
            Column(Modifier.padding(pad).padding(horizontal = 16.dp).fillMaxSize()) {
            ScreenHeader("Semanas", onBack)
            PixelButton("+ SEMANA ANTERIOR", Modifier.fillMaxWidth(), gold = false, onClick = onAddPast)
            Spacer(Modifier.height(12.dp))
            if (summaries.isEmpty()) {
                Text("Aún no hay semanas registradas.", color = PxDim, modifier = Modifier.padding(8.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(summaries, key = { it.id }) { w ->
                    PixelCard(Modifier.fillMaxWidth().clickable { onOpen(w.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(WeekMath.rangeLabel(w.startEpochDay), color = PxGold, fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f))
                            if (w.startEpochDay == currentMonday) Text("ACTUAL", color = PxBlueLight, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium)
                        }
                        Text(Money.format(w.availableCents) + " disponible", color = PxText, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp))
                        Text("Inicial ${Money.format(w.initialCents)} · Gastado ${Money.format(w.spentCents)} · Ahorrado ${Money.format(w.savedCents)}",
                            color = PxDim, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        }
    }
}

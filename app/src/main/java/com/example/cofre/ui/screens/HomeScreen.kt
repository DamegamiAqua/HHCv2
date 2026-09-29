package com.example.cofre.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cofre.core.Money
import com.example.cofre.core.WeekMath
import com.example.cofre.data.WeekSummary
import com.example.cofre.data.AppSettings
import com.example.cofre.data.LocalMediaStore
import com.example.cofre.ui.ChestUiState
import com.example.cofre.ui.LocalSoundEngine
import com.example.cofre.data.SoundEvent
import com.example.cofre.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    state: ChestUiState,
    pendingBurst: Boolean,
    onBurstConsumed: () -> Unit,
    pendingWithdrawal: Boolean,
    onWithdrawalConsumed: () -> Unit,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onHistory: () -> Unit,
    week: WeekSummary?,
    onWeek: () -> Unit,
    onWeeks: () -> Unit,
    onNewWeek: () -> Unit,
    onSettings: () -> Unit,
    onSecret: () -> Unit,
    finalUnlocked: Boolean,
    settings: AppSettings,
    media: LocalMediaStore,
) {
    val sound = LocalSoundEngine.current
    var burstKey by remember { mutableIntStateOf(0) }
    var withdrawalKey by remember { mutableIntStateOf(0) }
    val bounce = remember { Animatable(1f) }

    // Al volver de una aportación: dispara monedas y consume el evento.
    LaunchedEffect(pendingBurst) {
        if (pendingBurst) { burstKey++; sound.play(SoundEvent.COIN); onBurstConsumed() }
    }
    LaunchedEffect(pendingWithdrawal) {
        if (pendingWithdrawal) { withdrawalKey++; onWithdrawalConsumed() }
    }
    // Rebote del cofre cuando llegan las monedas.
    LaunchedEffect(state.goalReached) { if (state.goalReached) sound.play(SoundEvent.REWARD) }

    LaunchedEffect(burstKey, withdrawalKey) {
        if (burstKey > 0) {
            delay(220)
            bounce.animateTo(1.10f, tween(90))
            bounce.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium))
        } else if (withdrawalKey > 0) {
            bounce.animateTo(0.96f, tween(90))
            bounce.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium))
        }
    }

    // Contador: anima una fracción; el importe y todas las operaciones monetarias siguen siendo Long.
    var previousBalance by rememberSaveable { mutableLongStateOf(state.balanceCents) }
    var startBalance by rememberSaveable { mutableLongStateOf(state.balanceCents) }
    val fraction = remember { Animatable(1f) }
    LaunchedEffect(state.balanceCents) {
        startBalance = previousBalance
        previousBalance = state.balanceCents
        fraction.snapTo(0f)
        if (startBalance == state.balanceCents) fraction.snapTo(1f)
        else {
            if (state.balanceCents > startBalance) delay(550)
            fraction.animateTo(1f, tween(900))
        }
    }
    val delta = state.balanceCents - startBalance
    val step = (fraction.value * 1000f).toInt().coerceIn(0, 1000)
    val display = if (delta == 0L) state.balanceCents else {
        val q = delta / 1000L
        val r = delta % 1000L
        startBalance + q * step + r * step / 1000L
    }

    Scaffold(containerColor = PxBg) { pad ->
        Box(Modifier.fillMaxSize()) {
            BackgroundLayer(media, settings, "chest", Modifier.fillMaxSize())
            Column(
            Modifier.padding(pad).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Text(state.goalName.uppercase(), color = PxGold, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall)
            Text("Meta ${Money.format(state.targetCents)} MXN", color = PxDim)

            Box(Modifier.fillMaxWidth().height(300.dp)) {
                Chest(state.progress, Modifier.fillMaxSize().scale(bounce.value))
                CoinBurst(burstKey, Modifier.fillMaxSize())
            }

            Text(Money.format(display), color = PxGold, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displaySmall)
            Text("de ${Money.format(state.targetCents)}", color = PxText)
            Spacer(Modifier.height(12.dp))
            PixelProgress(state.progress)
            Text("${state.percent}%", color = PxBlueLight, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            if (state.surplusCents > 0) {
                Text("+${Money.format(state.surplusCents)} por encima de la meta", color = PxGoldLight)
            }
            Spacer(Modifier.height(8.dp))
            Text(message(state), color = PxText, style = MaterialTheme.typography.titleSmall)

            Spacer(Modifier.height(20.dp))
            if (week == null) {
                PixelCard(Modifier.fillMaxWidth()) {
                    Text("NUEVA SEMANA", color = PxGold, fontWeight = FontWeight.Bold)
                    Text("¿Cuánto dinero tienes disponible esta semana?", color = PxText, modifier = Modifier.padding(vertical = 8.dp))
                    PixelButton("DEFINIR SEMANA", Modifier.fillMaxWidth(), onClick = onNewWeek)
                }
            } else {
                PixelCard(Modifier.fillMaxWidth().clickable(onClick = onWeek)) {
                    Text("SEMANA ${WeekMath.rangeLabel(week.startEpochDay)}", color = PxGold, fontWeight = FontWeight.Bold)
                    Text(Money.format(week.availableCents), color = PxText, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge)
                    Text("disponible · gastado ${Money.format(week.spentCents)} · ahorrado ${Money.format(week.savedCents)}",
                        color = PxDim, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PixelButton("+ AGREGAR", Modifier.weight(1f), gold = true, onClick = onDeposit)
                PixelButton("− RETIRAR", Modifier.weight(1f), gold = false, enabled = state.balanceCents > 0, onClick = onWithdraw)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PixelButton("HIST. COFRE", Modifier.weight(1f), gold = false, onClick = onHistory)
                PixelButton("SEMANAS", Modifier.weight(1f), gold = false, onClick = onWeeks)
            }
            Spacer(Modifier.height(24.dp))
            if (finalUnlocked) {
                PixelCard(Modifier.fillMaxWidth()) {
                    Text("RECOMPENSA ESPECIAL DESBLOQUEADA", color = PxGold, fontWeight = FontWeight.Bold)
                    Text("Hay algo nuevo para celebrar tu progreso.", color = PxText, modifier = Modifier.padding(vertical = 6.dp))
                    PixelButton("VER FINAL", Modifier.fillMaxWidth(), gold = true, onClick = onSecret)
                }
                Spacer(Modifier.height(12.dp))
            }
            PixelButton("AJUSTES", Modifier.fillMaxWidth(), gold = false, onClick = onSettings)
            Spacer(Modifier.height(24.dp))
        }
        }
    }
}

private fun message(s: ChestUiState): String = when {
    s.goalReached -> "¡Meta alcanzada! La Hero Hunk es tuya."
    s.balanceCents == 0L -> "Tu cofre te espera. Cada moneda cuenta."
    s.percent >= 90 -> "¡Ya casi! Última recta."
    s.percent >= 50 -> "¡Más de la mitad! Sigue así."
    else -> "Estoy avanzando."
}

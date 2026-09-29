package com.example.cofre.ui.screens

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cofre.data.SoundEvent
import com.example.cofre.ui.LocalSoundEngine
import com.example.cofre.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun FinalSecretScreen(onBack: () -> Unit) {
    val sound = LocalSoundEngine.current
    var replayKey by rememberSaveable { mutableIntStateOf(0) }
    var phase by rememberSaveable { mutableIntStateOf(0) }
    val glow = remember { Animatable(0f) }
    val transition = rememberInfiniteTransition(label = "secret_particles")
    val travel by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing)),
        label = "secret_travel",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "secret_pulse",
    )

    LaunchedEffect(replayKey) {
        phase = 0
        glow.snapTo(0f)
        sound.play(SoundEvent.REWARD)
        glow.animateTo(1f, tween(900))
        delay(900)
        phase = 1
        delay(900)
        sound.play(SoundEvent.COIN)
        phase = 2
        delay(1450)
        phase = 3
    }

    Scaffold(containerColor = PxInk) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(PxInk, PxBg, Color(0xFF101B34), PxInk)
                    )
                )
                val center = Offset(size.width / 2f, size.height * 0.34f)
                val radius = size.minDimension * (0.20f + glow.value * 0.10f)
                drawCircle(PxGold.copy(alpha = 0.10f * glow.value), radius, center)
                drawCircle(PxBlue.copy(alpha = 0.08f * glow.value), radius * 1.35f, center)
                drawRect(PxBlueDark.copy(alpha = 0.35f), Offset(0f, size.height * 0.72f), Size(size.width, 4f))
                val roadY = size.height * 0.78f
                drawRect(PxPanel, Offset(0f, roadY), Size(size.width, size.height - roadY))
                val stripeOffset = (replayKey * 37f + travel * 96f) % 96f
                var x = -100f + stripeOffset
                while (x < size.width + 100f) {
                    drawRect(PxGold.copy(alpha = 0.70f), Offset(x, roadY + 34f), Size(52f, 6f))
                    x += 96f
                }
                if (phase >= 2) drawPixelBike(center, pulse)
                if (phase >= 1) {
                    repeat(16) { i ->
                        val px = ((i * 83 + replayKey * 31) % (size.width.toInt().coerceAtLeast(1))).toFloat()
                        val py = (size.height * 0.12f) + ((i * 47 + (travel * 80f).toInt()) % 260)
                        drawRect(PxGoldLight.copy(alpha = 0.45f), Offset(px, py), Size(4f, 4f))
                    }
                }
            }

            Column(
                Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    when (phase) {
                        0 -> "OBJETIVO ALCANZADO"
                        1 -> "MONEDA A MONEDA"
                        2 -> "EL CAMINO SE ABRE"
                        else -> "HERO RUN"
                    },
                    color = PxGold,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (phase >= 1) {
                        Text(
                            when (phase) {
                                1 -> "La constancia convirtió pequeños pasos en distancia."
                                2 -> "Ahorrar también es construir libertad para moverte."
                                else -> "Hero Hunk 160R 4V"
                            },
                            color = PxText,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (phase >= 3) {
                        Text("META: $15,000.00 MXN", color = PxGoldLight, fontWeight = FontWeight.Bold)
                        Text("Tu progreso ya forma parte de tu futuro.", color = PxBlueLight, textAlign = TextAlign.Center)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PixelButton("‹ VOLVER", Modifier.weight(1f), gold = false, onClick = onBack)
                    if (phase >= 3) PixelButton("REPETIR", Modifier.weight(1f), gold = true, onClick = { replayKey++ })
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelBike(center: Offset, scale: Float) {
    val s = minOf(size.width, size.height) / 280f
    val x = center.x
    val y = size.height * 0.62f
    val wheel = 26f * s
    val base = 48f * s
    drawCircle(PxInk, wheel, Offset(x - base, y))
    drawCircle(PxInk, wheel, Offset(x + base, y))
    drawCircle(PxGoldDark, wheel * 0.64f, Offset(x - base, y))
    drawCircle(PxGoldDark, wheel * 0.64f, Offset(x + base, y))
    drawRect(PxBlue, Offset(x - 42f * s, y - 30f * s), Size(84f * s, 24f * s))
    drawRect(PxGold, Offset(x - 18f * s, y - 46f * s), Size(54f * s, 17f * s))
    drawRect(PxText.copy(alpha = 0.9f), Offset(x + 27f * s, y - 50f * s), Size(11f * s, 12f * s))
    drawRect(PxBlueLight.copy(alpha = 0.9f), Offset(x + 35f * s, y - 28f * s), Size(10f * s, 8f * s))
    drawLine(PxGoldLight, Offset(x - 8f * s, y - 38f * s), Offset(x + 14f * s, y - 57f * s), 5f * s)
    drawCircle(PxGoldLight.copy(alpha = 0.55f * scale), 3f * s, Offset(x + 49f * s, y - 20f * s))
}

package com.example.cofre.ui.screens

import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.cofre.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Sprite 16x13. K=contorno, G/H/g=dorado (base/luz/sombra), B/b/L=azul (base/sombra/luz)
private val CHEST = listOf(
    "..KKKKKKKKKKKK..",
    ".KHHHHHHHHHHHHK.",
    ".KGGGGGGGGGGGGK.",
    ".KGBGGGGGGGGBGK.",
    ".KGGGGGGGGGGGGK.",
    ".KKKKKKKKKKKKKK.",
    ".KBBBBBLLBBBBBK.",
    ".KBBBBBLKBBBBBK.",
    ".KBBBBBLLBBBBBK.",
    ".KGBBBBBBBBBBGK.",
    ".KGbbbbbbbbbbGK.",
    ".KKKKKKKKKKKKKK.",
    "..KK........KK..",
)
private const val COLS = 16
private const val PILE_ROWS = 5

private fun cellColor(c: Char): Color? = when (c) {
    'K' -> PxInk; 'G' -> PxGold; 'H' -> PxGoldLight; 'g' -> PxGoldDark
    'B' -> PxBlue; 'b' -> PxBlueDark; 'L' -> PxBlueLight; else -> null
}

/** Cofre pixelart dorado con detalles azules. Sobre él se apila un montón de monedas según el avance. */
@Composable
fun Chest(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val rows = CHEST.size + PILE_ROWS
        val cell = minOf(size.width / COLS, size.height / rows)
        val ox = (size.width - cell * COLS) / 2f
        val oy = (size.height - cell * rows) / 2f
        drawCircle(PxBlue.copy(alpha = 0.18f), radius = cell * 9f, center = Offset(size.width / 2f, size.height * 0.6f))
        val level = (progress.coerceIn(0f, 1f) * PILE_ROWS).toInt()
        for (r in 0 until level) {
            val w = 10 - 2 * r
            val startCol = (COLS - w) / 2
            for (c in 0 until w) {
                val color = if ((c + r) % 2 == 0) PxGoldLight else PxGold
                drawRect(color, Offset(ox + (startCol + c) * cell, oy + (PILE_ROWS - 1 - r) * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
        CHEST.forEachIndexed { ri, row ->
            row.forEachIndexed { ci, ch ->
                cellColor(ch)?.let {
                    drawRect(it, Offset(ox + ci * cell, oy + (PILE_ROWS + ri) * cell), Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}

private class CoinSpec(val x: Float, val delay: Float)

private fun DrawScope.pixelCoin(c: Offset, u: Float, t: Float) {
    val w = if ((t * 8).toInt() % 2 == 0) 4 * u else 2 * u // "giro" alternando ancho
    drawRect(PxBlueDark, Offset(c.x - w / 2, c.y - 2 * u), Size(w, 4 * u))
    drawRect(PxGold, Offset(c.x - w / 2 + u / 2, c.y - 1.5f * u), Size(w - u, 3 * u))
    drawRect(PxGoldLight, Offset(c.x - w / 2 + u / 2, c.y - 1.5f * u), Size(u * 0.8f, u * 0.8f))
}

/** Monedas pixelart que caen en arco hacia el cofre + destellos. `trigger` > 0 dispara la animación. */
@Composable
fun CoinBurst(trigger: Int, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1600, easing = LinearEasing))
        }
    }
    val coins = remember(trigger) {
        val r = Random(trigger)
        List(8) { CoinSpec(0.15f + 0.7f * r.nextFloat(), 0.05f * it + 0.03f * r.nextFloat()) }
    }
    val p = progress.value
    if (trigger == 0 || p >= 1f) return
    Canvas(modifier) {
        val u = 5.dp.toPx()
        val target = Offset(size.width / 2f, size.height * 0.30f)
        coins.forEach { coin ->
            val t = ((p - coin.delay) / 0.42f).coerceIn(0f, 1f)
            if (t > 0f && t < 1f) {
                val s = Offset(size.width * coin.x, -u * 4)
                val ctrl = Offset((s.x + target.x) / 2f + (coin.x - 0.5f) * size.width * 0.3f, size.height * 0.02f)
                val pos = s * ((1 - t) * (1 - t)) + ctrl * (2 * (1 - t) * t) + target * (t * t)
                pixelCoin(pos, u, t)
            }
        }
        val sp = ((p - 0.45f) / 0.55f).coerceIn(0f, 1f)
        if (sp > 0f) {
            val colors = listOf(PxGold, PxGoldLight, PxBlueLight)
            for (k in 0 until 12) {
                val a = Math.toRadians(k * 30.0)
                val d = sp * size.width * 0.28f
                val pos = Offset(target.x + cos(a).toFloat() * d, target.y + sin(a).toFloat() * d)
                drawRect(colors[k % 3].copy(alpha = 1f - sp), Offset(pos.x - u / 2, pos.y - u / 2), Size(u, u))
            }
        }
    }
}

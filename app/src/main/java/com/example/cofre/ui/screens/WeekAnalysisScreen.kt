package com.example.cofre.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cofre.core.DateFmt
import com.example.cofre.core.Money
import com.example.cofre.core.WeekMath
import com.example.cofre.data.WeekDetail
import com.example.cofre.data.AppSettings
import com.example.cofre.data.LocalMediaStore
import com.example.cofre.ui.theme.*
import java.time.LocalDate

/** Acumulados al cierre de cada día (Long, centavos). */
class DayPoint(val spent: Long, val saved: Long, val available: Long)

/** Punto 0 = inicio de semana; luego un punto por día hasta lastDay (0..6 = lun..dom). */
fun buildSeries(d: WeekDetail, lastDay: Int): List<DayPoint> {
    val start = d.week.startEpochDay
    fun dayOf(ms: Long) = (DateFmt.localDate(ms).toEpochDay() - start).toInt().coerceIn(0, 6)
    val pts = mutableListOf(DayPoint(0, 0, d.week.initialCents))
    for (day in 0..lastDay) {
        val s = d.expenses.filter { dayOf(it.occurredAt) <= day }.sumOf { it.amountCents }
        val v = d.transfers.filter { dayOf(it.occurredAt) <= day }.sumOf { it.amountCents }
        pts += DayPoint(s, v, d.week.initialCents - s - v)
    }
    return pts
}

@Composable
fun WeekAnalysisScreen(detail: WeekDetail, onBack: () -> Unit, settings: AppSettings, media: LocalMediaStore) {
    val today = remember { LocalDate.now().toEpochDay() }
    val start = detail.week.startEpochDay
    val lastDay = (today - start).toInt().coerceIn(0, 6)
    val days = lastDay + 1 // días transcurridos (7 si la semana ya terminó)
    val points = buildSeries(detail, lastDay)

    Scaffold(containerColor = PxBg) { pad ->
        Box(Modifier.fillMaxSize()) {
            BackgroundLayer(media, settings, "week", Modifier.fillMaxSize())
            Column(Modifier.padding(pad).padding(horizontal = 16.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
            ScreenHeader("Análisis ${WeekMath.rangeLabel(start)}", onBack)
            WeekSummaryGrid(detail.week.initialCents, detail.spentCents, detail.savedCents, detail.availableCents)
            Spacer(Modifier.height(12.dp))
            PixelCard(Modifier.fillMaxWidth()) {
                Text("EVOLUCIÓN DE LA SEMANA", color = PxGold, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                WeekChart(points, detail.week.initialCents)
                Spacer(Modifier.height(8.dp))
                LegendRow(PxGold, "Disponible", detail.availableCents)
                LegendRow(PxDanger, "Gasto acumulado", detail.spentCents)
                LegendRow(PxBlueLight, "Ahorro acumulado", detail.savedCents)
            }
            Spacer(Modifier.height(12.dp))
            PixelCard(Modifier.fillMaxWidth()) {
                Text("Gasto promedio por día", color = PxDim, style = MaterialTheme.typography.labelSmall)
                Text(Money.format(detail.spentCents / days), color = PxText, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                val pct = if (detail.week.initialCents > 0) (detail.savedCents * 100 / detail.week.initialCents).toInt() else 0
                Text("Ahorraste el $pct% de tu saldo inicial", color = PxBlueLight, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, cents: Long) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, color = PxText, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(Money.format(cents), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

private val CHART_PAD = 10.dp
private val DAY_LABELS = listOf("L", "M", "X", "J", "V", "S", "D")

/**
 * Gráfica escalonada pixelart (3 series). Eje Y = 0..saldo inicial. Los valores se escalan con aritmética Long;
 * solo las coordenadas de pantalla son Float.
 */
@Composable
fun WeekChart(points: List<DayPoint>, initialCents: Long, modifier: Modifier = Modifier) {
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(200.dp).padding(horizontal = CHART_PAD, vertical = 6.dp)) {
            val maxV = maxOf(initialCents, 1L)
            val w = size.width
            val h = size.height.toInt()
            fun xf(i: Int) = w * i / 7f
            fun yf(v: Long): Float = (h - (h.toLong() * v / maxV).toInt()).toFloat()

            for (k in 0..4) {
                val yy = h * k / 4f
                drawLine(PxBlueDark, Offset(0f, yy), Offset(w, yy), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            }
            drawLine(PxBlue, Offset(0f, h.toFloat()), Offset(w, h.toFloat()), 4f, StrokeCap.Square)

            val sw = 3.dp.toPx()
            val m = 4.dp.toPx()
            stepSeries(points.map { it.saved }, PxBlueLight, ::xf, ::yf, sw, m)
            stepSeries(points.map { it.spent }, PxDanger, ::xf, ::yf, sw, m)
            stepSeries(points.map { it.available }, PxGold, ::xf, ::yf, sw, m)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = CHART_PAD)) {
            DAY_LABELS.forEach {
                Text(it, color = PxDim, textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun DrawScope.stepSeries(
    vals: List<Long>, color: Color, xf: (Int) -> Float, yf: (Long) -> Float, stroke: Float, marker: Float,
) {
    for (i in 1 until vals.size) {
        val y0 = yf(vals[i - 1]); val y1 = yf(vals[i])
        drawLine(color, Offset(xf(i - 1), y0), Offset(xf(i), y0), stroke, StrokeCap.Square)
        drawLine(color, Offset(xf(i), y0), Offset(xf(i), y1), stroke, StrokeCap.Square)
    }
    vals.forEachIndexed { i, v -> drawRect(color, Offset(xf(i) - marker, yf(v) - marker), Size(marker * 2, marker * 2)) }
}

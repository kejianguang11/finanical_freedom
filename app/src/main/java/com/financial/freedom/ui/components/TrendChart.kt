package com.financial.freedom.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financial.freedom.data.local.entity.DailySummary
import com.financial.freedom.ui.theme.FinancialColors
import java.math.BigDecimal
import java.math.RoundingMode

data class ChartPoint(
    val date: String,
    val value: Double,
    val rawValue: BigDecimal,
    val dayChange: BigDecimal,
    val dayChangePct: BigDecimal
)

@Composable
fun TrendChart(
    data: List<DailySummary>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val isUp = data.last().totalValueCNY >= (data.firstOrNull()?.totalValueCNY ?: BigDecimal.ZERO)
    val lineColor = if (isUp) FinancialColors.up else FinancialColors.down

    val points = remember(data) {
        data.map { s ->
            val rawValue = s.totalValueCNY
            ChartPoint(
                date = "${s.date.monthNumber}/${s.date.dayOfMonth}",
                value = rawValue.toDouble(),
                rawValue = rawValue,
                dayChange = s.dayChange,
                dayChangePct = s.dayChangePct
            )
        }
    }

    val minVal = remember(points) { points.minOf { it.value } }
    val maxVal = remember(points) { points.maxOf { it.value } }
    val range = (maxVal - minVal).coerceAtLeast(1.0)
    val midVal = (minVal + maxVal) / 2.0

    val yLabels = remember(minVal, maxVal) {
        listOf(
            formatYLabel(maxVal),
            formatYLabel((maxVal + midVal) / 2),
            formatYLabel(midVal),
            formatYLabel((midVal + minVal) / 2),
            formatYLabel(minVal)
        )
    }

    val xLabels = remember(points) {
        if (points.size <= 7) points.map { it.date }
        else {
            val step = (points.size - 1) / 6.0
            (0..6).map { i ->
                val idx = (i * step).toInt().coerceAtMost(points.size - 1)
                points[idx].date
            }
        }
    }

    var selectedPoint by remember { mutableStateOf<ChartPoint?>(null) }
    var selectedX by remember { mutableStateOf(0f) }
    var selectedY by remember { mutableStateOf(0f) }

    val density = LocalDensity.current

    Column(modifier = modifier) {
        // Chart area with Y-axis labels
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Y-axis labels
            Column(
                modifier = Modifier.width(56.dp).height(200.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                yLabels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Canvas
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val stepX = if (points.size > 1) w / (points.size - 1) else 0f

                                // Find nearest point
                                var nearestIdx = 0
                                var minDist = Float.MAX_VALUE
                                points.forEachIndexed { i, pt ->
                                    val px = i * stepX
                                    val py = h - ((pt.value - minVal) / range * h).toFloat()
                                    val dist = kotlin.math.sqrt(
                                        (offset.x - px) * (offset.x - px) + (offset.y - py) * (offset.y - py)
                                    )
                                    if (dist < minDist) { minDist = dist; nearestIdx = i }
                                }

                                val tapPt = points[nearestIdx]
                                val tapX = nearestIdx * stepX
                                val tapY = h - ((tapPt.value - minVal) / range * h).toFloat()

                                if (selectedPoint?.date == tapPt.date) {
                                    selectedPoint = null
                                } else {
                                    selectedPoint = tapPt
                                    selectedX = tapX
                                    selectedY = tapY
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val stepX = if (points.size > 1) w / (points.size - 1) else 0f

                    // Grid lines
                    val gridColor = Color.Gray.copy(alpha = 0.15f)
                    for (i in 0..4) {
                        val y = h * i / 4f
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    }

                    // Fill area
                    val fillPath = Path().apply {
                        moveTo(0f, h)
                        points.forEachIndexed { i, pt ->
                            val x = i * stepX
                            val y = h - ((pt.value - minVal) / range * h).toFloat()
                            lineTo(x, y)
                        }
                        lineTo((points.size - 1) * stepX, h)
                        close()
                    }
                    drawPath(fillPath, lineColor.copy(alpha = 0.1f))

                    // Line
                    val linePath = Path()
                    points.forEachIndexed { i, pt ->
                        val x = i * stepX
                        val y = h - ((pt.value - minVal) / range * h).toFloat()
                        if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                    }
                    drawPath(linePath, lineColor, style = Stroke(width = 2.5.dp.toPx()))

                    // Dots at each point
                    points.forEachIndexed { i, pt ->
                        val x = i * stepX
                        val y = h - ((pt.value - minVal) / range * h).toFloat()
                        drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(x, y))
                    }

                    // Draw selection indicator
                    selectedPoint?.let { sel ->
                        val sx = selectedX
                        val sy = selectedY
                        drawCircle(lineColor, radius = 7.dp.toPx(), center = Offset(sx, sy))
                        drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(sx, sy))
                    }
                }

                // Tooltip
                selectedPoint?.let { pt ->
                    val tipBg = MaterialTheme.colorScheme.inverseSurface
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(tipBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                pt.date,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.inverseOnSurface
                            )
                            Text(
                                formatMoney(pt.rawValue),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pt.dayChange >= BigDecimal.ZERO) FinancialColors.up else FinancialColors.down
                            )
                            Row {
                                val sign = if (pt.dayChange >= BigDecimal.ZERO) "+" else ""
                                Text(
                                    "$sign${formatMoney(pt.dayChange)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (pt.dayChange >= BigDecimal.ZERO) FinancialColors.up else FinancialColors.down
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "(${pt.dayChangePct}%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (pt.dayChange >= BigDecimal.ZERO) FinancialColors.up else FinancialColors.down
                                )
                            }
                        }
                    }
                }
            }
        }

        // X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 4.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            xLabels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatYLabel(value: Double): String {
    val abs = kotlin.math.abs(value)
    return when {
        abs >= 1_000_000 -> String.format("%.1fM", value / 1_000_000)
        abs >= 10_000 -> String.format("%.1f万", value / 10_000)
        abs >= 1_000 -> String.format("%.1fk", value / 1_000)
        else -> String.format("%.0f", value)
    }
}

private fun formatMoney(value: BigDecimal): String {
    val abs = value.abs()
    val integer = abs.toBigInteger().toString()
    val formatted = integer.reversed().chunked(3).joinToString(",").reversed()
    val decimal = abs.subtract(BigDecimal(abs.toBigInteger())).toPlainString()
        .removePrefix("0").take(4)
    val full = if (decimal.isNotEmpty() && decimal != ".00") "$formatted$decimal" else formatted
    return if (value < BigDecimal.ZERO) "-$full" else full
}

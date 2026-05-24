package com.financial.freedom.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.PathEffect
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

enum class TooltipType {
    NET_WORTH,
    EARNINGS
}

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
    modifier: Modifier = Modifier,
    showPercentage: Boolean = false,
    secondaryData: List<DailySummary>? = null,
    secondaryLabel: String? = null,
    secondaryColor: Color = FinancialColors.deposit,
    multiplier: BigDecimal = BigDecimal.ONE,
    tooltipType: TooltipType = TooltipType.NET_WORTH
) {
    if (data.isEmpty()) return

    val firstValue = remember(data) { data.firstOrNull()?.totalValueCNY ?: BigDecimal.ZERO }
    val isUp = data.last().totalValueCNY >= (data.firstOrNull()?.totalValueCNY ?: BigDecimal.ZERO)
    val lineColor = if (isUp) FinancialColors.up else FinancialColors.down

    val points = remember(data, showPercentage, firstValue) {
        data.map { s ->
            val rawValue = s.totalValueCNY
            val displayValue = if (showPercentage && firstValue > BigDecimal.ZERO) {
                rawValue.subtract(firstValue).divide(firstValue, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100)).setScale(2, java.math.RoundingMode.HALF_UP)
                    .toDouble()
            } else {
                rawValue.toDouble()
            }
            ChartPoint(
                date = "${s.date.monthNumber}/${s.date.dayOfMonth}",
                value = displayValue,
                rawValue = rawValue,
                dayChange = s.dayChange,
                dayChangePct = s.dayChangePct
            )
        }
    }

    val secondaryPoints = remember(secondaryData, showPercentage) {
        secondaryData?.filter { it.totalValueCNY > BigDecimal.ZERO }?.map { s ->
            val rawValue = s.totalValueCNY
            val displayValue = if (showPercentage && firstValue > BigDecimal.ZERO) {
                rawValue.subtract(firstValue).divide(firstValue, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100)).setScale(2, java.math.RoundingMode.HALF_UP)
                    .toDouble()
            } else {
                rawValue.toDouble()
            }
            ChartPoint(
                date = "${s.date.monthNumber}/${s.date.dayOfMonth}",
                value = displayValue,
                rawValue = rawValue,
                dayChange = BigDecimal.ZERO,
                dayChangePct = BigDecimal.ZERO
            )
        }
    }

    val rawMinVal = remember(points) { points.minOf { it.value } }
    val rawMaxVal = remember(points) { points.maxOf { it.value } }
    val (minVal, maxVal) = when (tooltipType) {
        TooltipType.EARNINGS -> {
            val absMax = maxOf(kotlin.math.abs(rawMinVal), kotlin.math.abs(rawMaxVal)).coerceAtLeast(1.0)
            Pair(-absMax, absMax)
        }
        TooltipType.NET_WORTH -> Pair(rawMinVal, rawMaxVal)
    }
    val range = (maxVal - minVal).coerceAtLeast(0.01)
    val midVal = (minVal + maxVal) / 2.0

    val yLabels = remember(minVal, maxVal, showPercentage, tooltipType) {
        if (showPercentage) {
            listOf(
                formatPctLabel(maxVal),
                formatPctLabel((maxVal + midVal) / 2),
                formatPctLabel(midVal),
                formatPctLabel((midVal + minVal) / 2),
                formatPctLabel(minVal)
            )
        } else {
            listOf(
                formatYLabel(maxVal, tooltipType),
                formatYLabel((maxVal + midVal) / 2, tooltipType),
                formatYLabel(midVal, tooltipType),
                formatYLabel((midVal + minVal) / 2, tooltipType),
                formatYLabel(minVal, tooltipType)
            )
        }
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

    var selectedPoint by remember(data) { mutableStateOf<ChartPoint?>(null) }
    var selectedX by remember(data) { mutableStateOf(0f) }
    var selectedY by remember(data) { mutableStateOf(0f) }

    val density = LocalDensity.current

    Column(modifier = modifier) {
        // Legend row
        if (secondaryData != null && secondaryLabel != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp).height(3.dp)
                        .background(lineColor, RoundedCornerShape(1.5.dp))
                )
                Spacer(Modifier.width(4.dp))
                Text("净值", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Row(
                    modifier = Modifier.width(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(4.dp).height(3.dp)
                                .background(secondaryColor, RoundedCornerShape(1.dp))
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Text(secondaryLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

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

                    // Line — per-segment color in earnings mode
                    if (tooltipType == TooltipType.EARNINGS) {
                        for (i in 0 until points.size - 1) {
                            val pt0 = points[i]
                            val pt1 = points[i + 1]
                            val x0 = i * stepX
                            val y0 = h - ((pt0.value - minVal) / range * h).toFloat()
                            val x1 = (i + 1) * stepX
                            val y1 = h - ((pt1.value - minVal) / range * h).toFloat()
                            val segColor = if (pt1.dayChange >= BigDecimal.ZERO) FinancialColors.up else FinancialColors.down
                            val segPath = Path().apply { moveTo(x0, y0); lineTo(x1, y1) }
                            drawPath(segPath, segColor, style = Stroke(width = 2.5.dp.toPx()))
                        }
                    } else {
                        val linePath = Path()
                        points.forEachIndexed { i, pt ->
                            val x = i * stepX
                            val y = h - ((pt.value - minVal) / range * h).toFloat()
                            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                        }
                        drawPath(linePath, lineColor, style = Stroke(width = 2.5.dp.toPx()))
                    }

                    // Secondary dashed line
                    secondaryPoints?.let { secPts ->
                        if (secPts.isNotEmpty()) {
                            val secPath = Path()
                            secPts.forEachIndexed { i, pt ->
                                val x = i * stepX
                                val y = h - ((pt.value - minVal) / range * h).toFloat()
                                if (i == 0) secPath.moveTo(x, y) else secPath.lineTo(x, y)
                            }
                            drawPath(secPath, secondaryColor, style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            ))
                        }
                    }

                    // Dots at each point
                    points.forEachIndexed { i, pt ->
                        val x = i * stepX
                        val y = h - ((pt.value - minVal) / range * h).toFloat()
                        val dotColor = when (tooltipType) {
                            TooltipType.EARNINGS ->
                                if (pt.dayChange >= BigDecimal.ZERO) FinancialColors.up else FinancialColors.down
                            TooltipType.NET_WORTH -> lineColor
                        }
                        drawCircle(dotColor, radius = 3.dp.toPx(), center = Offset(x, y))
                    }

                    // Draw selection indicator
                    selectedPoint?.let { sel ->
                        val sx = selectedX
                        val sy = selectedY
                        val selColor = when (tooltipType) {
                            TooltipType.EARNINGS ->
                                if (sel.dayChange >= BigDecimal.ZERO) FinancialColors.up else FinancialColors.down
                            TooltipType.NET_WORTH -> lineColor
                        }
                        drawCircle(selColor, radius = 7.dp.toPx(), center = Offset(sx, sy))
                        drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(sx, sy))
                    }
                }

                // Tooltip
                selectedPoint?.let { pt ->
                    val tipBg = MaterialTheme.colorScheme.inverseSurface
                    val isGain = pt.dayChange >= BigDecimal.ZERO
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(tipBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        when (tooltipType) {
                            TooltipType.EARNINGS -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        pt.date,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.inverseOnSurface
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            "${if (pt.dayChange >= BigDecimal.ZERO) "+" else ""}${formatMoney(pt.dayChange, multiplier)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGain) FinancialColors.up else FinancialColors.down
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "(${pt.dayChangePct}%)",
                                            fontSize = 11.sp,
                                            color = if (isGain) FinancialColors.up else FinancialColors.down
                                        )
                                    }
                                }
                            }
                            TooltipType.NET_WORTH -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        pt.date,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.inverseOnSurface
                                    )
                                    Text(
                                        formatMoney(pt.rawValue, multiplier),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.inverseOnSurface
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            "${if (pt.dayChange >= BigDecimal.ZERO) "+" else ""}${formatMoney(pt.dayChange, multiplier)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isGain) FinancialColors.up else FinancialColors.down
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            "(${pt.dayChangePct}%)",
                                            fontSize = 10.sp,
                                            color = if (isGain) FinancialColors.up else FinancialColors.down
                                        )
                                    }
                                }
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

private fun formatYLabel(value: Double, tooltipType: TooltipType = TooltipType.NET_WORTH): String {
    val abs = kotlin.math.abs(value)
    val decimals = tooltipType == TooltipType.EARNINGS
    return when {
        abs >= 1_000_000 -> formatLarge(value / 1_000_000, "M", decimals)
        abs >= 10_000 -> formatLarge(value / 10_000, "万", decimals)
        abs >= 1_000 -> formatLarge(value / 1_000, "k", decimals)
        else -> if (decimals) String.format("%.1f", value) else String.format("%.0f", value)
    }
}

private fun formatLarge(value: Double, suffix: String, decimals: Boolean): String {
    val formatted = if (decimals) String.format("%.1f", value) else String.format("%.1f", value)
    return if (formatted.endsWith(".0")) "${formatted.dropLast(2)}$suffix" else "$formatted$suffix"
}

private fun formatPctLabel(value: Double): String {
    val formatted = String.format("%.1f", value)
    val clean = if (formatted.endsWith(".0")) formatted.dropLast(2) else formatted
    return if (value >= 0) "+${clean}%" else "${clean}%"
}

private fun formatMoney(value: BigDecimal, multiplier: BigDecimal): String {
    return com.financial.freedom.ui.common.FormatUtils.formatMoney(value, multiplier)
}

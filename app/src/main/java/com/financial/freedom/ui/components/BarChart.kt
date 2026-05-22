package com.financial.freedom.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financial.freedom.ui.theme.FinancialColors
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

@Composable
fun BarChart(
    data: List<BigDecimal>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList()
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val barColor = if (data.sumOf { it } >= BigDecimal.ZERO) FinancialColors.up else FinancialColors.down

    BoxWithConstraints(modifier.fillMaxWidth().height(140.dp)) {
        val maxVal = data.maxOfOrNull { it.abs() } ?: BigDecimal.ONE
        val maxDouble = maxVal.toDouble().let { if (it == 0.0) 1.0 else it }
        val barCount = data.size
        val density = LocalDensity.current.density
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        Canvas(Modifier.fillMaxSize()) {
            if (barCount == 0) return@Canvas

            val totalGap = canvasWidth * 0.15f
            val gapPerBar = totalGap / (barCount + 1)
            val barWidth = (canvasWidth - totalGap) / barCount
            val textPadding = 40f

            data.forEachIndexed { index, value ->
                val barHeight = (abs(value.toDouble()) / maxDouble).toFloat() * (canvasHeight - textPadding - 10f)
                val x = gapPerBar + index * (barWidth + gapPerBar)
                val y = canvasHeight - textPadding - barHeight

                drawRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth.coerceAtLeast(4f), barHeight.coerceAtLeast(1f))
                )

                if (labels.isNotEmpty() && index < labels.size) {
                    val textLayout = textMeasurer.measure(
                        text = labels[index],
                        style = TextStyle(fontSize = 9.sp, color = Color.Gray)
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(x + barWidth / 2 - textLayout.size.width / 2, canvasHeight - textPadding + 6f)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniBarChart(
    data: Map<String, BigDecimal>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val sortedEntries = remember(data) { data.entries.toList() }
    val values = sortedEntries.map { it.value }
    val keys = sortedEntries.map { it.key }

    BarChart(
        data = values,
        labels = keys,
        modifier = modifier
    )
}

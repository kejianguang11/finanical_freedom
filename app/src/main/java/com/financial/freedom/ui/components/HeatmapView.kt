package com.financial.freedom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financial.freedom.data.local.entity.DailySummary
import com.financial.freedom.ui.theme.FinancialColors
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

@Composable
fun HeatmapView(
    data: List<DailySummary>,
    startDate: LocalDate,
    endDate: LocalDate,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier.padding(40.dp), contentAlignment = Alignment.Center) {
            Text("暂无收益数据", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
    val summaryMap = remember(data) { data.associateBy { it.date } }

    val firstDay = startDate
    val daysSinceMonday = (firstDay.dayOfWeek.ordinal + 6) % 7

    val totalWeeks = remember(startDate, endDate) {
        var count = 0
        var d = firstDay
        while (d <= endDate) {
            count++
            d = d.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
        }
        (count + daysSinceMonday + 6) / 7
    }

    val maxAbsChange = remember(data) {
        data.maxOfOrNull { it.dayChange.abs() } ?: BigDecimal.ONE
    }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            dayLabels.forEachIndexed { i, label ->
                if (i % 2 == 0) {
                    Text(
                        label,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(14.dp)
                    )
                } else {
                    Spacer(Modifier.width(14.dp))
                }
            }
            repeat(totalWeeks - 7) { Spacer(Modifier.width(14.dp)) }
        }

        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.Top) {
            repeat(totalWeeks) { weekIndex ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(7) { dayIndex ->
                        val cellIndex = weekIndex * 7 + dayIndex
                        val dateOrdinal = cellIndex - daysSinceMonday
                        val date = firstDay.plus(dateOrdinal, kotlinx.datetime.DateTimeUnit.DAY)

                        val summary = if (date in startDate..endDate) summaryMap[date] else null

                        val color = when {
                            summary == null -> MaterialTheme.colorScheme.surfaceVariant
                            summary.dayChange > BigDecimal.ZERO -> {
                                val intensity = (summary.dayChange / maxAbsChange).toFloat().coerceIn(0.05f, 1f)
                                FinancialColors.up.copy(alpha = intensity * 0.7f + 0.1f)
                            }
                            summary.dayChange < BigDecimal.ZERO -> {
                                val intensity = (summary.dayChange.abs() / maxAbsChange).toFloat().coerceIn(0.05f, 1f)
                                FinancialColors.down.copy(alpha = intensity * 0.7f + 0.1f)
                            }
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Legend
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("跌", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(0.15f, 0.4f, 0.65f, 0.9f).forEach { alpha ->
                Box(
                    Modifier
                        .padding(horizontal = 1.dp)
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(FinancialColors.down.copy(alpha = alpha))
                )
            }
            Box(
                Modifier
                    .padding(horizontal = 1.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            listOf(0.15f, 0.4f, 0.65f, 0.9f).forEach { alpha ->
                Box(
                    Modifier
                        .padding(horizontal = 1.dp)
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(FinancialColors.up.copy(alpha = alpha))
                )
            }
            Text("涨", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

package com.financial.freedom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financial.freedom.ui.theme.FinancialColors
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

data class CalendarDay(
    val date: LocalDate,
    val value: BigDecimal = BigDecimal.ZERO,
    val isCurrentMonth: Boolean = true,
    val label: String = ""
)

private val dayHeaders = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun CalendarView(
    currentMonth: LocalDate,
    days: List<CalendarDay>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val colorForDay: (CalendarDay) -> androidx.compose.ui.graphics.Color = { day ->
        when {
            !day.isCurrentMonth -> surfaceColor
            day.value > BigDecimal.ZERO -> FinancialColors.upContainer
            day.value < BigDecimal.ZERO -> FinancialColors.downContainer
            else -> surfaceVariant
        }
    }
    Column(modifier) {
        // Month navigation
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "<",
                Modifier.clickable(onClick = onPrevMonth).padding(8.dp),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${currentMonth.year}年${currentMonth.monthNumber}月",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                ">",
                Modifier.clickable(onClick = onNextMonth).padding(8.dp),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(16.dp))

        // Day headers
        Row(Modifier.fillMaxWidth()) {
            dayHeaders.forEach { day ->
                Text(
                    day,
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Calendar grid: 6 rows x 7 columns
        val rows = days.chunked(7)
        rows.forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).height(52.dp)) {
                week.forEach { day ->
                    val bgColor = colorForDay(day)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .clickable { if (day.isCurrentMonth) onDayClick(day.date) }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${day.date.dayOfMonth}",
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = if (day.isCurrentMonth) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            if (day.value != BigDecimal.ZERO && day.isCurrentMonth) {
                                val sign = if (day.value > BigDecimal.ZERO) "+" else ""
                                Text(
                                    "$sign${formatCompact(day.value)}",
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    color = if (day.value > BigDecimal.ZERO) FinancialColors.up
                                    else FinancialColors.down
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCompact(value: BigDecimal): String {
    val abs = value.abs()
    val intVal = abs.toBigInteger()
    return when {
        intVal >= java.math.BigInteger("100000000") ->
            String.format("%.1f亿", abs.toDouble() / 100_000_000)
        intVal >= java.math.BigInteger("10000") ->
            String.format("%.1f万", abs.toDouble() / 10_000)
        else -> intVal.toString()
    }
}

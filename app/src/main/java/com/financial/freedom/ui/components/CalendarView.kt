package com.financial.freedom.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.financial.freedom.ui.theme.FinancialColors
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
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
    selectedDate: LocalDate? = null,
    modifier: Modifier = Modifier
) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val maxAbs = rememberMaxAbs(days)

    val infiniteTransition = rememberInfiniteTransition()
    val todayGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse)
    )

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

        // Calendar grid
        val rows = days.chunked(7)
        rows.forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp).height(60.dp)) {
                week.forEach { day ->
                    val isSelected = day.date == selectedDate && day.isCurrentMonth
                    val isFuture = day.date > today
                    val bgColor = colorForDay(day, maxAbs, surfaceColor, surfaceVariant, isFuture)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .then(
                                if (isSelected) Modifier.border(
                                    2.dp,
                                    FinancialColors.gold.copy(alpha = todayGlowAlpha),
                                    RoundedCornerShape(8.dp)
                                ) else Modifier
                            )
                            .clickable { if (day.isCurrentMonth && !isFuture) onDayClick(day.date) }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${day.date.dayOfMonth}",
                                fontSize = 12.sp,
                                maxLines = 1,
                                color = if (day.isCurrentMonth && !isFuture)
                                    MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                            if (day.value != BigDecimal.ZERO && day.isCurrentMonth && !isFuture) {
                                val sign = when {
                                    day.value > BigDecimal.ZERO -> "+"
                                    day.value < BigDecimal.ZERO -> "-"
                                    else -> ""
                                }
                                Text(
                                    "$sign${formatCompact(day.value)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
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

@Composable
private fun rememberMaxAbs(days: List<CalendarDay>): BigDecimal {
    val max = days.filter { it.isCurrentMonth }
        .map { it.value.abs() }
        .maxOrNull() ?: BigDecimal.ZERO
    return if (max == BigDecimal.ZERO) BigDecimal.ONE else max
}

private fun colorForDay(
    day: CalendarDay,
    maxAbs: BigDecimal,
    surfaceColor: androidx.compose.ui.graphics.Color,
    surfaceVariant: androidx.compose.ui.graphics.Color,
    isFuture: Boolean
): androidx.compose.ui.graphics.Color {
    if (!day.isCurrentMonth) return surfaceColor
    if (isFuture) return surfaceVariant.copy(alpha = 0.3f)
    if (day.value == BigDecimal.ZERO) return surfaceVariant.copy(alpha = 0.5f)

    val fraction = (day.value.abs().toDouble() / maxAbs.toDouble()).toFloat().coerceIn(0f, 1f)
    val alpha = 0.08f + fraction * 0.32f

    return if (day.value > BigDecimal.ZERO)
        FinancialColors.up.copy(alpha = alpha)
    else
        FinancialColors.down.copy(alpha = alpha)
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

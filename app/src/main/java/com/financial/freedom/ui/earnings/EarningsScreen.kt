package com.financial.freedom.ui.earnings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.ui.components.CalendarDay
import com.financial.freedom.ui.components.CalendarView
import com.financial.freedom.ui.theme.FinancialColors
import java.math.BigDecimal
import java.math.RoundingMode

private val views = listOf("日", "周", "月", "年")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(
    viewModel: EarningsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = state.selectedView,
            modifier = Modifier.padding(horizontal = 16.dp),
            containerColor = MaterialTheme.colorScheme.background,
            edgePadding = 0.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedView]),
                    color = FinancialColors.gold
                )
            }
        ) {
            views.forEachIndexed { index, title ->
                Tab(
                    selected = state.selectedView == index,
                    onClick = { viewModel.selectView(index) },
                    text = {
                        Text(
                            title,
                            fontWeight = if (state.selectedView == index) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (state.selectedView == index) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        when (state.selectedView) {
            0 -> DayEarningsView(state, viewModel)
            1 -> WeekEarningsView(state, viewModel)
            2 -> MonthEarningsView(state)
            3 -> YearEarningsView(state)
        }
    }

    if (state.showBreakdown) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissBreakdown() },
            sheetState = rememberModalBottomSheetState()
        ) {
            BreakdownSheet(state.selectedDayBreakdown)
        }
    }
}

@Composable
private fun BreakdownSheet(breakdown: List<com.financial.freedom.data.local.entity.DailyBreakdownItem>) {
    Column(Modifier.padding(24.dp)) {
        Text("收益明细", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        if (breakdown.isEmpty()) {
            Text("暂无明细数据", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            var total = BigDecimal.ZERO
            breakdown.forEach { item ->
                total += item.changeCNY
                val icon = when (item.type) {
                    "DEPOSIT" -> "存款"
                    "STOCK" -> "股票"
                    "FUND" -> "基金"
                    "GOLD" -> "黄金"
                    else -> item.type
                }
                val changeText = if (item.changeCNY >= BigDecimal.ZERO)
                    "+${formatMoney(item.changeCNY)}" else formatMoney(item.changeCNY)
                val color = if (item.changeCNY >= BigDecimal.ZERO)
                    FinancialColors.up else FinancialColors.down
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(icon, style = MaterialTheme.typography.bodyMedium)
                    Text(changeText, color = color, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("合计", fontWeight = FontWeight.Bold)
                val totalText = if (total >= BigDecimal.ZERO) "+${formatMoney(total)}" else formatMoney(total)
                Text(totalText,
                    fontWeight = FontWeight.Bold,
                    color = if (total >= BigDecimal.ZERO) FinancialColors.up else FinancialColors.down)
            }
        }
    }
}

// ===== Day View =====
@Composable
private fun DayEarningsView(state: EarningsUiState, viewModel: EarningsViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        CalendarView(
            currentMonth = state.currentMonth,
            days = state.dayEarnings.map { day ->
                CalendarDay(
                    date = day.date,
                    value = day.totalChange,
                    isCurrentMonth = day.isCurrentMonth
                )
            },
            onPrevMonth = { viewModel.changeMonth(false) },
            onNextMonth = { viewModel.changeMonth(true) },
            onDayClick = { date -> viewModel.selectDay(date) }
        )
    }
}

// ===== Week View: Monthly grouping =====
@Composable
private fun WeekEarningsView(state: EarningsUiState, viewModel: EarningsViewModel) {
    if (state.weekEarnings.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无收益数据", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Month navigation header
        item {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "<",
                    Modifier.clickable(onClick = { viewModel.changeViewMonth(false) }).padding(8.dp),
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${state.currentViewMonth.year}年${state.currentViewMonth.monthNumber}月",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    ">",
                    Modifier.clickable(onClick = { viewModel.changeViewMonth(true) }).padding(8.dp),
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Week cards
        itemsIndexed(state.weekEarnings) { index, week ->
            val isUp = week.totalChange >= BigDecimal.ZERO
            val isExpanded = state.weekExpandedIndex == index

            ElevatedCard(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleWeekExpanded(index) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    // Week header
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${week.weekLabel}  ${week.startDate.monthNumber}.${week.startDate.dayOfMonth} - ${week.endDate.monthNumber}.${week.endDate.dayOfMonth}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        val totalText = if (week.totalChange >= BigDecimal.ZERO)
                            "+${formatMoney(week.totalChange)}" else formatMoney(week.totalChange)
                        Text(
                            totalText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isUp) FinancialColors.up else FinancialColors.down
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Daily bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.days.forEach { day ->
                            val barColor = when {
                                day.totalChange > BigDecimal.ZERO -> FinancialColors.up
                                day.totalChange < BigDecimal.ZERO -> FinancialColors.down
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val maxBarH = 40.dp
                                val allChanges = week.days.map { it.totalChange.abs() }
                                val maxAbs = allChanges.maxOrNull() ?: BigDecimal.ONE
                                val barFraction = if (maxAbs > BigDecimal.ZERO)
                                    day.totalChange.abs().toDouble() / maxAbs.toDouble() else 0.0
                                val barH = (maxBarH * barFraction.toFloat()).coerceAtLeast(3.dp)

                                Box(
                                    modifier = Modifier.width(24.dp).height(maxBarH),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .height(barH)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(barColor)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${day.date.monthNumber}.${day.date.dayOfMonth}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Expanded daily detail
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            week.days.filter { it.totalChange != BigDecimal.ZERO }.forEach { day ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${day.date.monthNumber}.${day.date.dayOfMonth} ${dayOfWeekLabel(day.dayOfWeek)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val dayText = if (day.totalChange >= BigDecimal.ZERO)
                                        "+${formatMoney(day.totalChange)}" else formatMoney(day.totalChange)
                                    Text(
                                        dayText,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (day.totalChange >= BigDecimal.ZERO) FinancialColors.up else FinancialColors.down
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun dayOfWeekLabel(dow: kotlinx.datetime.DayOfWeek): String = when (dow) {
    kotlinx.datetime.DayOfWeek.MONDAY -> "周一"
    kotlinx.datetime.DayOfWeek.TUESDAY -> "周二"
    kotlinx.datetime.DayOfWeek.WEDNESDAY -> "周三"
    kotlinx.datetime.DayOfWeek.THURSDAY -> "周四"
    kotlinx.datetime.DayOfWeek.FRIDAY -> "周五"
    kotlinx.datetime.DayOfWeek.SATURDAY -> "周六"
    kotlinx.datetime.DayOfWeek.SUNDAY -> "周日"
}

// ===== Month View =====
@Composable
private fun MonthEarningsView(state: EarningsUiState) {
    if (state.monthEarnings.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无收益数据", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "${state.monthEarnings.first().year}年 月收益",
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))

        val monthNames = listOf("1月", "2月", "3月", "4月", "5月", "6月",
            "7月", "8月", "9月", "10月", "11月", "12月")

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.monthEarnings) { _, month ->
                val isUp = month.totalChange >= BigDecimal.ZERO
                    ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                monthNames[month.month - 1],
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "↑${month.upDays}  ↓${month.downDays}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val totalText = if (month.totalChange >= BigDecimal.ZERO)
                            "+${formatMoney(month.totalChange)}" else formatMoney(month.totalChange)
                        Text(
                            totalText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isUp) FinancialColors.up else FinancialColors.down
                        )
                    }
                }
            }
        }
    }
}

// ===== Year View =====
@Composable
private fun YearEarningsView(state: EarningsUiState) {
    if (state.yearEarnings.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无收益数据", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("年度汇总", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }

        itemsIndexed(state.yearEarnings) { _, year ->
            val isUp = year.totalChange >= BigDecimal.ZERO
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "${year.year}年",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("年度总收益", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            val totalText = if (year.totalChange >= BigDecimal.ZERO)
                                "+${formatMoney(year.totalChange)}" else formatMoney(year.totalChange)
                            Text(totalText, fontWeight = FontWeight.Bold,
                                color = if (isUp) FinancialColors.up else FinancialColors.down)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("上涨天数", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text("${year.upDays}", fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("下跌天数", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text("${year.downDays}", fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("活跃天数", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text("${year.totalDays}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun formatMoney(value: BigDecimal): String {
    val abs = value.abs().setScale(2, RoundingMode.HALF_UP)
    val intPart = abs.toBigInteger().toString()
    val formatted = intPart.reversed().chunked(3).joinToString(",").reversed()
    val decimal = abs.subtract(BigDecimal(abs.toBigInteger())).toPlainString()
        .removePrefix("0").take(4)
    val fullValue = if (decimal.isNotEmpty() && decimal != ".00") "$formatted$decimal" else formatted
    return if (value < BigDecimal.ZERO) "-$fullValue" else fullValue
}

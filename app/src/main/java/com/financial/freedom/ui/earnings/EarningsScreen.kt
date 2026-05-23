package com.financial.freedom.ui.earnings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import java.math.RoundingMode

private val views = listOf("日", "周", "月", "年")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(
    viewModel: EarningsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(initialPage = state.selectedView, pageCount = { views.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectView(pagerState.currentPage)
    }

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.padding(horizontal = 16.dp),
            containerColor = MaterialTheme.colorScheme.background,
            edgePadding = 0.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = FinancialColors.gold
                )
            }
        ) {
            views.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            title,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // 在 pager 内部重新收集 state，确保 HorizontalPager 的页面缓存能感知倍率变化
        val pageState by viewModel.uiState.collectAsState()
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> DayEarningsView(pageState, viewModel)
                1 -> WeekEarningsView(pageState, viewModel)
                2 -> MonthEarningsView(pageState, viewModel)
                3 -> YearEarningsView(pageState)
            }
        }
    }

}

// ===== Day View =====
@Composable
private fun DayEarningsView(state: EarningsUiState, viewModel: EarningsViewModel) {
    // Compute monthly totals for summary bar
    val currentMonthDays = state.dayEarnings.filter { it.isCurrentMonth }
    val monthTotal = currentMonthDays.fold(BigDecimal.ZERO) { acc, d -> acc + d.totalChange }
    val monthUpDays = currentMonthDays.count { it.totalChange > BigDecimal.ZERO }
    val monthDownDays = currentMonthDays.count { it.totalChange < BigDecimal.ZERO }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .then(Modifier.verticalScroll(rememberScrollState()))
    ) {
        // Monthly summary bar
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${state.currentMonth.monthNumber}月收益",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isUp = monthTotal >= BigDecimal.ZERO
                val totalText = formatChangeAmount(monthTotal, state.displayMultiplier)
                Text(
                    totalText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (monthTotal == BigDecimal.ZERO) MaterialTheme.colorScheme.onSurfaceVariant
                        else if (isUp) FinancialColors.up else FinancialColors.down,
                    maxLines = 1,
                    softWrap = false
                )
                if (monthUpDays + monthDownDays > 0) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "涨${monthUpDays}跌${monthDownDays}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        CalendarView(
            currentMonth = state.currentMonth,
            days = state.dayEarnings.map { day ->
                CalendarDay(
                    date = day.date,
                    value = day.totalChange.multiply(state.displayMultiplier),
                    isCurrentMonth = day.isCurrentMonth
                )
            },
            onPrevMonth = { viewModel.changeMonth(false) },
            onNextMonth = { viewModel.changeMonth(true) },
            onDayClick = { date -> viewModel.selectDay(date) },
            selectedDate = state.selectedDay
        )

        // Inline breakdown panel for selected day
        if (state.selectedDay != null) {
            Spacer(Modifier.height(12.dp))
            DayBreakdownPanel(state)
        }
    }
}

@Composable
private fun DayBreakdownPanel(state: EarningsUiState) {
    val day = state.selectedDay ?: return
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${day.monthNumber}月${day.dayOfMonth}日",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val dayTotal = state.dayBreakdown.fold(BigDecimal.ZERO) { acc, b -> acc + b.changeCNY }
                if (dayTotal != BigDecimal.ZERO) {
                    Text(
                        formatChangeAmount(dayTotal, state.displayMultiplier),
                        fontWeight = FontWeight.Bold,
                        color = if (dayTotal >= BigDecimal.ZERO) FinancialColors.up else FinancialColors.down,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (state.dayBreakdown.isEmpty()) {
                Text(
                    "当日暂无收益明细",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.dayBreakdown.forEach { item ->
                    val isUp = item.changeCNY >= BigDecimal.ZERO
                    val changeText = formatChangeAmount(item.changeCNY, state.displayMultiplier)

                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.label,
                            Modifier.width(48.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            changeText,
                            Modifier.width(90.dp),
                            fontWeight = FontWeight.Medium,
                            color = if (isUp) FinancialColors.up else FinancialColors.down,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(item.fraction)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (isUp) FinancialColors.up.copy(alpha = 0.5f)
                                        else FinancialColors.down.copy(alpha = 0.5f)
                                    )
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${(item.fraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(32.dp)
                        )
                    }
                }

                val totalChange = state.dayBreakdown.fold(BigDecimal.ZERO) { acc, b -> acc + b.changeCNY }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("合计", fontWeight = FontWeight.Bold)
                    Text(
                        formatChangeAmount(totalChange, state.displayMultiplier),
                        fontWeight = FontWeight.Bold,
                        color = if (totalChange >= BigDecimal.ZERO) FinancialColors.up
                        else FinancialColors.down,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

// ===== Week View: Month nav + grid + details below (matching Day view pattern) =====
@Composable
private fun WeekEarningsView(state: EarningsUiState, viewModel: EarningsViewModel) {
    if (state.weekEarnings.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无收益数据", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val monthTotal = state.weekEarnings.fold(BigDecimal.ZERO) { acc, w -> acc + w.totalChange }
    val weekRows = state.weekEarnings.chunked(2)

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .then(Modifier.verticalScroll(rememberScrollState()))
    ) {
        // Month navigation header
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                ">",
                Modifier.clickable(onClick = { viewModel.changeViewMonth(true) }).padding(8.dp),
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Month summary
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${state.currentViewMonth.monthNumber}月收益",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            val totalIsUp = monthTotal >= BigDecimal.ZERO
            Text(
                formatChangeAmount(monthTotal, state.displayMultiplier),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (monthTotal == BigDecimal.ZERO) MaterialTheme.colorScheme.onSurfaceVariant
                    else if (totalIsUp) FinancialColors.up else FinancialColors.down,
                maxLines = 1,
                softWrap = false
            )
        }

        // 2-column week grid
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            weekRows.forEachIndexed { rowIdx, row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEachIndexed { colIdx, week ->
                        val weekIdx = rowIdx * 2 + colIdx
                        val isSelected = state.selectedWeekIndex == weekIdx
                        val isUp = week.totalChange >= BigDecimal.ZERO

                        ElevatedCard(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (isSelected) Modifier.border(1.5.dp, FinancialColors.gold, RoundedCornerShape(14.dp))
                                    else Modifier
                                )
                                .clickable { viewModel.selectWeek(weekIdx) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.elevatedCardElevation(
                                defaultElevation = if (isSelected) 4.dp else 2.dp
                            )
                        ) {
                            Column(
                                Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    week.weekLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected)
                                        FinancialColors.gold
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${week.startDate.monthNumber}.${week.startDate.dayOfMonth}-${week.endDate.monthNumber}.${week.endDate.dayOfMonth}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    formatChangeAmountShort(week.totalChange, state.displayMultiplier),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = adaptiveAmountFont(week.totalChange, 15, state.displayMultiplier).sp,
                                    color = if (week.totalChange == BigDecimal.ZERO) MaterialTheme.colorScheme.onSurfaceVariant
                                        else if (isUp) FinancialColors.up else FinancialColors.down,
                                    maxLines = 2,
                                    softWrap = true
                                )
                            }
                        }
                    }
                    if (row.size < 2) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Breakdown panel below grid (separated, like Day view)
        if (state.selectedWeekIndex != null) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = FinancialColors.gold.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))
            WeekBreakdownPanel(state)
        }
    }
}

@Composable
private fun WeekBreakdownPanel(state: EarningsUiState) {
    val week = state.weekEarnings.getOrNull(state.selectedWeekIndex ?: -1) ?: return

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${week.weekLabel}  ${week.startDate.monthNumber}.${week.startDate.dayOfMonth}-${week.endDate.monthNumber}.${week.endDate.dayOfMonth}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val weekTotal = state.weekBreakdown.fold(BigDecimal.ZERO) { acc, b -> acc + b.changeCNY }
                if (weekTotal != BigDecimal.ZERO) {
                    Text(
                        formatChangeAmount(weekTotal, state.displayMultiplier),
                        fontWeight = FontWeight.Bold,
                        color = if (weekTotal >= BigDecimal.ZERO) FinancialColors.up else FinancialColors.down,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (state.weekBreakdown.isEmpty()) {
                Text(
                    "本周暂无收益明细",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.weekBreakdown.forEach { item ->
                    val isUp = item.changeCNY >= BigDecimal.ZERO
                    val changeText = formatChangeAmount(item.changeCNY, state.displayMultiplier)

                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.label,
                            Modifier.width(48.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            changeText,
                            Modifier.width(90.dp),
                            fontWeight = FontWeight.Medium,
                            color = if (isUp) FinancialColors.up else FinancialColors.down,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(item.fraction)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (isUp) FinancialColors.up.copy(alpha = 0.5f)
                                        else FinancialColors.down.copy(alpha = 0.5f)
                                    )
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${(item.fraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(32.dp)
                        )
                    }
                }

                val totalChange = state.weekBreakdown.fold(BigDecimal.ZERO) { acc, b -> acc + b.changeCNY }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("合计", fontWeight = FontWeight.Bold)
                    Text(
                        formatChangeAmount(totalChange, state.displayMultiplier),
                        fontWeight = FontWeight.Bold,
                        color = if (totalChange >= BigDecimal.ZERO) FinancialColors.up
                        else FinancialColors.down,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

// ===== Month View =====
@Composable
private fun MonthEarningsView(state: EarningsUiState, viewModel: EarningsViewModel) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val monthNames = listOf("1月", "2月", "3月", "4月", "5月", "6月",
        "7月", "8月", "9月", "10月", "11月", "12月")

    // Compute year total for summary
    val yearTotal = state.monthEarnings.fold(BigDecimal.ZERO) { acc, m -> acc + m.totalChange }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Year navigation
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "<",
                    Modifier.clickable(onClick = { viewModel.changeYear(false) }).padding(8.dp),
                    fontSize = 22.sp,
                    color = if (state.selectedYear > state.earliestYear)
                        MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
                Text(
                    "${state.selectedYear}年",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    ">",
                    Modifier.clickable(onClick = { viewModel.changeYear(true) }).padding(8.dp),
                    fontSize = 22.sp,
                    color = if (state.selectedYear < today.year)
                        MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            }
        }

        // Year total summary
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "年度总收益",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatChangeAmount(yearTotal, state.displayMultiplier),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (yearTotal == BigDecimal.ZERO) MaterialTheme.colorScheme.onSurfaceVariant
                        else if (yearTotal >= BigDecimal.ZERO) FinancialColors.up else FinancialColors.down,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // 4x3 month grid
        item {
            val rows = state.monthEarnings.chunked(4)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { month ->
                            val isFuture = (state.selectedYear == today.year && month.month > today.monthNumber)
                                || state.selectedYear > today.year
                            val isSelected = state.selectedMonth == month.month
                            val hasData = month.totalChange != BigDecimal.ZERO

                            val isUp = month.totalChange >= BigDecimal.ZERO
                            val monthFontSize = if (hasData) adaptiveAmountFont(month.totalChange, 14, state.displayMultiplier) else 14

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .then(
                                        if (isSelected) Modifier.border(
                                            1.5.dp, FinancialColors.gold, RoundedCornerShape(12.dp)
                                        ) else Modifier
                                    )
                                    .clickable { if (!isFuture) viewModel.selectMonth(month.month) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        monthNames[month.month - 1],
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) FinancialColors.gold
                                            else if (isFuture) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    if (isFuture) {
                                        Text(
                                            "—",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        )
                                    } else if (!hasData) {
                                        Text(
                                            "0",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            formatChangeAmountShort(month.totalChange, state.displayMultiplier),
                                            fontSize = monthFontSize.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUp) FinancialColors.up else FinancialColors.down,
                                            maxLines = 2,
                                            softWrap = true
                                        )
                                    }
                                }
                            }
                        }
                        // Fill remaining columns with empty space
                        repeat(4 - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Asset breakdown panel for selected month
        if (state.selectedMonth != null) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "${state.selectedMonth}月明细",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))

                        if (state.monthBreakdown.isEmpty()) {
                            Text(
                                "该月暂无收益数据",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val totalChange = state.monthBreakdown.fold(BigDecimal.ZERO) { acc, b -> acc + b.changeCNY }

                            state.monthBreakdown.forEach { item ->
                                val isUp = item.changeCNY >= BigDecimal.ZERO
                                val changeText = formatChangeAmount(item.changeCNY, state.displayMultiplier)

                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        item.label,
                                        Modifier.width(48.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        changeText,
                                        Modifier.width(90.dp),
                                        fontWeight = FontWeight.Medium,
                                        color = if (isUp) FinancialColors.up else FinancialColors.down,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(item.fraction)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    if (isUp) FinancialColors.up.copy(alpha = 0.5f)
                                                    else FinancialColors.down.copy(alpha = 0.5f)
                                                )
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${(item.fraction * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(32.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("合计", fontWeight = FontWeight.Bold)
                                Text(
                                    formatChangeAmount(totalChange, state.displayMultiplier),
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalChange >= BigDecimal.ZERO) FinancialColors.up
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
                            Text(
                                formatChangeAmount(year.totalChange, state.displayMultiplier),
                                fontWeight = FontWeight.Bold,
                                color = if (year.totalChange == BigDecimal.ZERO) MaterialTheme.colorScheme.onSurfaceVariant
                                    else if (isUp) FinancialColors.up else FinancialColors.down,
                                maxLines = 1,
                                softWrap = false)
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

fun formatMoney(value: BigDecimal, multiplier: BigDecimal = BigDecimal.ONE): String {
    return com.financial.freedom.ui.common.FormatUtils.formatMoney(value, multiplier)
}

fun formatMoneyShort(value: BigDecimal, multiplier: BigDecimal = BigDecimal.ONE): String {
    return com.financial.freedom.ui.common.FormatUtils.formatMoneyShort(value, multiplier)
}

/** Format change amount as absolute value (no +/- sign). Color indicates direction. */
fun formatChangeAmount(value: BigDecimal, multiplier: BigDecimal = BigDecimal.ONE): String {
    return formatMoney(value.abs(), multiplier)
}

fun formatChangeAmountShort(value: BigDecimal, multiplier: BigDecimal = BigDecimal.ONE): String {
    return formatMoneyShort(value.abs(), multiplier)
}

/** Adaptive font size: shrink when the number string is long to avoid ellipsis. */
fun adaptiveAmountFont(value: BigDecimal, baseSp: Int, multiplier: BigDecimal = BigDecimal.ONE): Int {
    val display = formatChangeAmountShort(value, multiplier)
    return when {
        display.length > 9 -> (baseSp - 3).coerceAtLeast(10)
        display.length > 7 -> (baseSp - 2).coerceAtLeast(10)
        else -> baseSp
    }
}

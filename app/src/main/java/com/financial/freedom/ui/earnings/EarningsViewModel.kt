package com.financial.freedom.ui.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.entity.DailyBreakdownItem
import com.financial.freedom.data.local.entity.DailySummary
import com.financial.freedom.data.repository.SummaryRepository
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.DataVerifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class DayEarning(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    val totalChange: BigDecimal,
    val isCurrentMonth: Boolean,
    val breakdown: List<DailyBreakdownItem> = emptyList()
)

data class WeekEarning(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalChange: BigDecimal,
    val days: List<DayEarning>,
    val weekLabel: String  // e.g. "第1周"
)

data class MonthEarning(
    val year: Int,
    val month: Int,
    val totalChange: BigDecimal,
    val upDays: Int,
    val downDays: Int,
    val labels: List<String> = emptyList()
)

data class MonthBreakdown(
    val type: String,
    val label: String,
    val changeCNY: BigDecimal,
    val fraction: Float = 0f
)

data class YearEarning(
    val year: Int,
    val totalChange: BigDecimal,
    val upDays: Int,
    val downDays: Int,
    val totalDays: Int,
    val yearEndTotalValue: BigDecimal? = null,
    val yearOverYearChange: BigDecimal? = null,
    val yearOverYearChangePct: BigDecimal? = null
)

enum class YearViewMode { EARNINGS, TOTAL_ASSETS }

data class EarningsUiState(
    val selectedView: Int = 0,
    val currentMonth: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val currentViewMonth: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val dayEarnings: List<DayEarning> = emptyList(),
    val weekEarnings: List<WeekEarning> = emptyList(),
    val monthEarnings: List<MonthEarning> = emptyList(),
    val yearEarnings: List<YearEarning> = emptyList(),
    val selectedDay: LocalDate? = null,
    val dayBreakdown: List<MonthBreakdown> = emptyList(),
    val selectedWeekIndex: Int? = null,
    val weekBreakdown: List<MonthBreakdown> = emptyList(),
    val displayMultiplier: java.math.BigDecimal = java.math.BigDecimal.ONE,
    val selectedYear: Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).year,
    val selectedMonth: Int? = null,
    val monthBreakdown: List<MonthBreakdown> = emptyList(),
    val earliestYear: Int = Clock.System.todayIn(TimeZone.currentSystemDefault()).year,
    val yearViewMode: YearViewMode = YearViewMode.EARNINGS
)

@HiltViewModel
class EarningsViewModel @Inject constructor(
    private val summaryRepository: SummaryRepository,
    private val accountManager: AccountManager,
    private val dataVerifier: DataVerifier,
    private val displaySettings: com.financial.freedom.domain.settings.DisplaySettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(EarningsUiState())
    val uiState: StateFlow<EarningsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            displaySettings.multiplierFlow.collect { multiplier ->
                _uiState.value = _uiState.value.copy(displayMultiplier = multiplier)
            }
        }
        viewModelScope.launch { loadDayView() }
        // 倍率变化时刷新当前视图的展示
        viewModelScope.launch {
            displaySettings.multiplierFlow.drop(1).collect {
                when (_uiState.value.selectedView) {
                    0 -> loadDayView()
                    1 -> loadWeekView()
                    2 -> loadMonthView()
                    3 -> loadYearView()
                }
            }
        }
    }

    fun selectView(index: Int) {
        if (_uiState.value.selectedView == index) return
        _uiState.value = _uiState.value.copy(selectedView = index)
        viewModelScope.launch {
            when (index) {
                0 -> loadDayView()
                1 -> loadWeekView()
                2 -> loadMonthView()
                3 -> loadYearView()
            }

            // 加载视图时验证数据一致性
            val accountId = accountManager.currentAccountId.value ?: return@launch
            try {
                dataVerifier.verifyAll(accountId)
            } catch (_: Exception) {}
        }
    }

    fun changeMonth(forward: Boolean) {
        val current = _uiState.value.currentMonth
        val newMonth = if (forward) {
            if (current.monthNumber == 12) LocalDate(current.year + 1, 1, 1)
            else LocalDate(current.year, current.monthNumber + 1, 1)
        } else {
            if (current.monthNumber == 1) LocalDate(current.year - 1, 12, 1)
            else LocalDate(current.year, current.monthNumber - 1, 1)
        }
        _uiState.value = _uiState.value.copy(currentMonth = newMonth)
        viewModelScope.launch { loadDayView() }
    }

    fun changeViewMonth(forward: Boolean) {
        val current = _uiState.value.currentViewMonth
        val newMonth = if (forward) {
            if (current.monthNumber == 12) LocalDate(current.year + 1, 1, 1)
            else LocalDate(current.year, current.monthNumber + 1, 1)
        } else {
            if (current.monthNumber == 1) LocalDate(current.year - 1, 12, 1)
            else LocalDate(current.year, current.monthNumber - 1, 1)
        }
        _uiState.value = _uiState.value.copy(currentViewMonth = newMonth)
        viewModelScope.launch { loadWeekView() }
    }

    fun selectWeek(index: Int) {
        val current = _uiState.value.selectedWeekIndex
        if (current == index) {
            _uiState.value = _uiState.value.copy(selectedWeekIndex = null, weekBreakdown = emptyList())
        } else {
            _uiState.value = _uiState.value.copy(selectedWeekIndex = index)
            viewModelScope.launch { loadWeekBreakdown(index) }
        }
    }

    private suspend fun loadWeekBreakdown(index: Int) {
        val accountId = accountManager.currentAccountId.value ?: return
        val week = _uiState.value.weekEarnings.getOrNull(index) ?: return
        val items = summaryRepository.getBreakdownsByDateRange(week.startDate, week.endDate, accountId)
        val byType = items.groupBy { it.type }

        val typeLabels = mapOf(
            "DEPOSIT" to "存款", "STOCK" to "股票", "FUND" to "基金", "GOLD" to "黄金"
        )
        val breakdowns = typeLabels.map { (type, label) ->
            val typeItems = byType[type] ?: emptyList()
            val totalChange = typeItems.fold(BigDecimal.ZERO) { acc, item -> acc + item.changeCNY }
            MonthBreakdown(type = type, label = label, changeCNY = totalChange)
        }.filter { it.changeCNY != BigDecimal.ZERO }

        val maxAbs = breakdowns.maxOfOrNull { it.changeCNY.abs() } ?: BigDecimal.ONE
        val withFractions = breakdowns.map { b ->
            val fraction = if (maxAbs > BigDecimal.ZERO)
                (b.changeCNY.abs().toDouble() / maxAbs.toDouble()).toFloat() else 0f
            b.copy(fraction = fraction)
        }

        _uiState.value = _uiState.value.copy(weekBreakdown = withFractions)
    }

    fun selectDay(date: LocalDate) {
        val current = _uiState.value.selectedDay
        if (current == date) {
            _uiState.value = _uiState.value.copy(selectedDay = null, dayBreakdown = emptyList())
        } else {
            _uiState.value = _uiState.value.copy(selectedDay = date)
            viewModelScope.launch { loadDayBreakdown(date) }
        }
    }

    private suspend fun loadDayBreakdown(date: LocalDate) {
        val accountId = accountManager.currentAccountId.value ?: return
        val breakdown = summaryRepository.getBreakdown(date, accountId)
        val typeLabels = mapOf(
            "DEPOSIT" to "存款", "STOCK" to "股票", "FUND" to "基金", "GOLD" to "黄金"
        )
        val items = typeLabels.map { (type, label) ->
            val item = breakdown.find { it.type == type }
            MonthBreakdown(
                type = type, label = label,
                changeCNY = item?.changeCNY ?: BigDecimal.ZERO
            )
        }.filter { it.changeCNY != BigDecimal.ZERO }

        val maxAbs = items.maxOfOrNull { it.changeCNY.abs() } ?: BigDecimal.ONE
        val withFractions = items.map { b ->
            val fraction = if (maxAbs > BigDecimal.ZERO)
                (b.changeCNY.abs().toDouble() / maxAbs.toDouble()).toFloat() else 0f
            b.copy(fraction = fraction)
        }

        _uiState.value = _uiState.value.copy(dayBreakdown = withFractions)
    }

    private suspend fun loadDayView() {
        val accountId = accountManager.currentAccountId.value ?: return
        val month = _uiState.value.currentMonth
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val firstDay = LocalDate(month.year, month.monthNumber, 1)
        val lastDay = if (month.year == today.year && month.monthNumber == today.monthNumber) today
        else firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

        val breakdowns = summaryRepository.getBreakdownsByDateRange(firstDay, lastDay, accountId)
            .groupBy { it.date }

        val dayOfWeek = firstDay.dayOfWeek
        val daysInPrevMonth = (dayOfWeek.ordinal + 6) % 7

        val days = mutableListOf<DayEarning>()

        for (i in daysInPrevMonth - 1 downTo 0) {
            val date = firstDay.minus(i + 1, DateTimeUnit.DAY)
            days.add(DayEarning(date, date.dayOfWeek, BigDecimal.ZERO, false))
        }

        var current = firstDay
        while (current <= lastDay) {
            val dayItems = breakdowns[current] ?: emptyList()
            val totalChange = visibleTotal(dayItems)
            days.add(DayEarning(
                date = current, dayOfWeek = current.dayOfWeek,
                totalChange = totalChange,
                isCurrentMonth = current.monthNumber == month.monthNumber && current.year == month.year
            ))
            current = current.plus(1, DateTimeUnit.DAY)
        }

        while (days.size % 7 != 0) {
            days.add(DayEarning(current, current.dayOfWeek, BigDecimal.ZERO, false))
            current = current.plus(1, DateTimeUnit.DAY)
        }

        _uiState.value = _uiState.value.copy(dayEarnings = days)
        // Auto-select today and load breakdown
        selectDay(today)
    }

    private suspend fun loadWeekView() {
        val accountId = accountManager.currentAccountId.value ?: return
        val viewMonth = _uiState.value.currentViewMonth

        // Get first and last day of the view month
        val firstOfMonth = LocalDate(viewMonth.year, viewMonth.monthNumber, 1)
        val lastOfMonth = firstOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

        // Extend to full weeks (Monday to Sunday)
        var weekStart = firstOfMonth
        while (weekStart.dayOfWeek != DayOfWeek.MONDAY) {
            weekStart = weekStart.minus(1, DateTimeUnit.DAY)
        }
        var weekEnd = lastOfMonth
        while (weekEnd.dayOfWeek != DayOfWeek.SUNDAY) {
            weekEnd = weekEnd.plus(1, DateTimeUnit.DAY)
        }

        val summaries = summaryRepository.getListByDateRange(weekStart, weekEnd, accountId)
            .associateBy { it.date }
        val breakdowns = summaryRepository.getBreakdownsByDateRange(weekStart, weekEnd, accountId)
            .groupBy { it.date }

        val weeks = mutableListOf<WeekEarning>()
        var ws = weekStart
        var weekNum = 1

        while (ws <= weekEnd) {
            val we = ws.plus(6, DateTimeUnit.DAY)
            val weekDays = mutableListOf<DayEarning>()
            var weekTotal = BigDecimal.ZERO

            var d = ws
            while (d <= we) {
                val dayItems = breakdowns[d] ?: emptyList()
                val change = visibleTotal(dayItems)
                weekTotal += change
                weekDays.add(DayEarning(d, d.dayOfWeek, change, d.monthNumber == viewMonth.monthNumber))
                d = d.plus(1, DateTimeUnit.DAY)
            }

            weeks.add(WeekEarning(
                startDate = ws, endDate = we, totalChange = weekTotal,
                days = weekDays, weekLabel = "第${weekNum}周"
            ))
            weekNum++
            ws = we.plus(1, DateTimeUnit.DAY)
        }

        _uiState.value = _uiState.value.copy(weekEarnings = weeks)
        // Auto-select current week (contains today)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val currentWeekIdx = weeks.indexOfFirst { today >= it.startDate && today <= it.endDate }
        if (currentWeekIdx >= 0) selectWeek(currentWeekIdx)
    }

    fun changeYear(forward: Boolean) {
        val current = _uiState.value.selectedYear
        val newYear = if (forward) current + 1 else current - 1
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        if (newYear > today.year) return
        _uiState.value = _uiState.value.copy(
            selectedYear = newYear,
            selectedMonth = null,
            monthBreakdown = emptyList()
        )
        viewModelScope.launch { loadMonthView() }
    }

    fun selectMonth(month: Int) {
        val state = _uiState.value
        if (state.selectedMonth == month) {
            _uiState.value = state.copy(selectedMonth = null, monthBreakdown = emptyList())
        } else {
            _uiState.value = state.copy(selectedMonth = month)
            viewModelScope.launch { loadMonthBreakdown(month) }
        }
    }

    private suspend fun loadMonthView() {
        val accountId = accountManager.currentAccountId.value ?: return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val year = _uiState.value.selectedYear

        val yearStart = LocalDate(year, 1, 1)
        val yearEnd = if (year == today.year) today else LocalDate(year, 12, 31)
        val items = summaryRepository.getBreakdownsByDateRange(yearStart, yearEnd, accountId)

        val byMonth = items.groupBy { it.date.monthNumber }

        val months = (1..12).map { month ->
            val monthItems = byMonth[month] ?: emptyList()
            val byDate = monthItems.groupBy { it.date }
            val totalChange = byDate.values.fold(BigDecimal.ZERO) { acc, dayItems -> acc + visibleTotal(dayItems) }
            val up = byDate.values.count { dayItems -> visibleTotal(dayItems) > BigDecimal.ZERO }
            val down = byDate.values.count { dayItems -> visibleTotal(dayItems) < BigDecimal.ZERO }
            MonthEarning(
                year = year, month = month, totalChange = totalChange,
                upDays = up, downDays = down
            )
        }

        // Find earliest year with data
        val earliestDate = summaryRepository.getEarliestDate(accountId)
        val earliestYr = earliestDate?.year ?: today.year

        _uiState.value = _uiState.value.copy(
            monthEarnings = months,
            earliestYear = earliestYr
        )
        // Auto-select current month if viewing current year
        if (year == today.year) {
            selectMonth(today.monthNumber)
        }
    }

    private suspend fun loadMonthBreakdown(month: Int) {
        val accountId = accountManager.currentAccountId.value ?: return
        val year = _uiState.value.selectedYear
        val firstDay = LocalDate(year, month, 1)
        val lastDay = if (month == 12) LocalDate(year, 12, 31)
            else LocalDate(year, month + 1, 1).minus(1, DateTimeUnit.DAY)

        val items = summaryRepository.getBreakdownsByDateRange(firstDay, lastDay, accountId)
        val byType = items.groupBy { it.type }

        val typeLabels = mapOf(
            "DEPOSIT" to "存款",
            "STOCK" to "股票",
            "FUND" to "基金",
            "GOLD" to "黄金"
        )

        val breakdowns = typeLabels.map { (type, label) ->
            val typeItems = byType[type] ?: emptyList()
            val totalChange = typeItems.fold(BigDecimal.ZERO) { acc, item -> acc + item.changeCNY }
            MonthBreakdown(type = type, label = label, changeCNY = totalChange)
        }.filter { it.changeCNY != BigDecimal.ZERO }

        val maxAbs = breakdowns.maxOfOrNull { it.changeCNY.abs() } ?: BigDecimal.ONE
        val withFractions = breakdowns.map { b ->
            val fraction = if (maxAbs > BigDecimal.ZERO)
                (b.changeCNY.abs().toDouble() / maxAbs.toDouble()).toFloat() else 0f
            b.copy(fraction = fraction)
        }

        _uiState.value = _uiState.value.copy(monthBreakdown = withFractions)
    }

    private suspend fun loadYearView() {
        val accountId = accountManager.currentAccountId.value ?: return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val startDate = LocalDate(today.year - 2, 1, 1)
        val items = summaryRepository.getBreakdownsByDateRange(startDate, today, accountId)

        val byYear = items.groupBy { it.date.year }

        val years = byYear.map { (year, yearItems) ->
            val byDate = yearItems.groupBy { it.date }
            val totalChange = byDate.values.fold(BigDecimal.ZERO) { acc, dayItems -> acc + visibleTotal(dayItems) }
            val up = byDate.values.count { dayItems -> visibleTotal(dayItems) > BigDecimal.ZERO }
            val down = byDate.values.count { dayItems -> visibleTotal(dayItems) < BigDecimal.ZERO }
            YearEarning(year, totalChange, up, down, byDate.values.size)
        }.sortedByDescending { it.year }

        // Load year-end totalValueCNY from DailySummary for total asset mode
        val assetStartDate = LocalDate(today.year - 3, 1, 1)
        val summaries = summaryRepository.getListByDateRange(assetStartDate, today, accountId)
        val summariesByYear = summaries.groupBy { it.date.year }

        val yearEndValues = mutableMapOf<Int, BigDecimal>()
        for ((year, yearSummaries) in summariesByYear) {
            val lastSummary = yearSummaries.maxByOrNull { it.date }
            if (lastSummary != null) {
                yearEndValues[year] = lastSummary.netWorth
            }
        }

        val enrichedYears = years.map { ye ->
            val endValue = yearEndValues[ye.year]
            val prevYearEndValue = yearEndValues[ye.year - 1]
            val yoyChange = if (endValue != null && prevYearEndValue != null)
                endValue - prevYearEndValue else null
            val yoyChangePct = if (yoyChange != null && prevYearEndValue != null && prevYearEndValue != BigDecimal.ZERO)
                yoyChange.multiply(BigDecimal("100")).divide(prevYearEndValue, 4, RoundingMode.HALF_UP) else null

            ye.copy(
                yearEndTotalValue = endValue,
                yearOverYearChange = yoyChange,
                yearOverYearChangePct = yoyChangePct
            )
        }

        _uiState.value = _uiState.value.copy(yearEarnings = enrichedYears)
    }

    fun selectYearViewMode(mode: YearViewMode) {
        _uiState.value = _uiState.value.copy(yearViewMode = mode)
    }

    companion object {
        /** 4 个可见资产分类的 daily change 之和（不含 FX 残差） */
        private val VISIBLE_TYPES = listOf("DEPOSIT", "STOCK", "FUND", "GOLD")

        private fun visibleTotal(items: List<DailyBreakdownItem>): BigDecimal {
            return VISIBLE_TYPES.fold(BigDecimal.ZERO) { acc, type ->
                acc + (items.firstOrNull { it.type == type }?.changeCNY ?: BigDecimal.ZERO)
            }
        }
    }
}

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

data class YearEarning(
    val year: Int,
    val totalChange: BigDecimal,
    val upDays: Int,
    val downDays: Int,
    val totalDays: Int
)

data class EarningsUiState(
    val selectedView: Int = 0,
    val currentMonth: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val currentViewMonth: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val dayEarnings: List<DayEarning> = emptyList(),
    val weekEarnings: List<WeekEarning> = emptyList(),
    val monthEarnings: List<MonthEarning> = emptyList(),
    val yearEarnings: List<YearEarning> = emptyList(),
    val selectedDayBreakdown: List<DailyBreakdownItem> = emptyList(),
    val showBreakdown: Boolean = false,
    val weekExpandedIndex: Int? = null,
    val displayMultiplier: java.math.BigDecimal = java.math.BigDecimal.ONE
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

    fun toggleWeekExpanded(index: Int) {
        val current = _uiState.value.weekExpandedIndex
        _uiState.value = _uiState.value.copy(weekExpandedIndex = if (current == index) null else index)
    }

    fun selectDay(date: LocalDate) {
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            val breakdown = summaryRepository.getBreakdown(date, accountId)
            _uiState.value = _uiState.value.copy(selectedDayBreakdown = breakdown, showBreakdown = true)
        }
    }

    fun dismissBreakdown() {
        _uiState.value = _uiState.value.copy(showBreakdown = false)
    }

    private suspend fun loadDayView() {
        val accountId = accountManager.currentAccountId.value ?: return
        val month = _uiState.value.currentMonth
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val firstDay = LocalDate(month.year, month.monthNumber, 1)
        val lastDay = if (month.year == today.year && month.monthNumber == today.monthNumber) today
        else firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

        val summaries = summaryRepository.getListByDateRange(firstDay, lastDay, accountId)
            .associateBy { it.date }

        val dayOfWeek = firstDay.dayOfWeek
        val daysInPrevMonth = (dayOfWeek.ordinal + 6) % 7

        val days = mutableListOf<DayEarning>()

        for (i in daysInPrevMonth - 1 downTo 0) {
            val date = firstDay.minus(i + 1, DateTimeUnit.DAY)
            days.add(DayEarning(date, date.dayOfWeek, BigDecimal.ZERO, false))
        }

        var current = firstDay
        while (current <= lastDay) {
            val summary = summaries[current]
            days.add(DayEarning(
                date = current, dayOfWeek = current.dayOfWeek,
                totalChange = summary?.dayChange ?: BigDecimal.ZERO,
                isCurrentMonth = true
            ))
            current = current.plus(1, DateTimeUnit.DAY)
        }

        while (days.size % 7 != 0) {
            days.add(DayEarning(current, current.dayOfWeek, BigDecimal.ZERO, false))
            current = current.plus(1, DateTimeUnit.DAY)
        }

        _uiState.value = _uiState.value.copy(dayEarnings = days)
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

        val weeks = mutableListOf<WeekEarning>()
        var ws = weekStart
        var weekNum = 1

        while (ws <= weekEnd) {
            val we = ws.plus(6, DateTimeUnit.DAY)
            val weekDays = mutableListOf<DayEarning>()
            var weekTotal = BigDecimal.ZERO

            var d = ws
            while (d <= we) {
                val s = summaries[d]
                val change = s?.dayChange ?: BigDecimal.ZERO
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
    }

    private suspend fun loadMonthView() {
        val accountId = accountManager.currentAccountId.value ?: return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val yearStart = LocalDate(today.year, 1, 1)
        val summaries = summaryRepository.getListByDateRange(yearStart, today, accountId)

        val byMonth = summaries.groupBy { it.date.monthNumber }

        val months = (1..12).map { month ->
            val days = byMonth[month] ?: emptyList()
            val totalChange = days.fold(BigDecimal.ZERO) { acc, s -> acc + s.dayChange }
            val up = days.count { it.dayChange > BigDecimal.ZERO }
            val down = days.count { it.dayChange < BigDecimal.ZERO }
            MonthEarning(
                year = today.year, month = month, totalChange = totalChange,
                upDays = up, downDays = down
            )
        }

        _uiState.value = _uiState.value.copy(monthEarnings = months)
    }

    private suspend fun loadYearView() {
        val accountId = accountManager.currentAccountId.value ?: return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val startDate = LocalDate(today.year - 2, 1, 1)
        val summaries = summaryRepository.getListByDateRange(startDate, today, accountId)

        val byYear = summaries.groupBy { it.date.year }

        val years = byYear.map { (year, days) ->
            val totalChange = days.fold(BigDecimal.ZERO) { acc, s -> acc + s.dayChange }
            val up = days.count { it.dayChange > BigDecimal.ZERO }
            val down = days.count { it.dayChange < BigDecimal.ZERO }
            YearEarning(year, totalChange, up, down, days.size)
        }.sortedByDescending { it.year }

        _uiState.value = _uiState.value.copy(yearEarnings = years)
    }
}

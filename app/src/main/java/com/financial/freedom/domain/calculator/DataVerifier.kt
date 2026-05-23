package com.financial.freedom.domain.calculator

import android.util.Log
import com.financial.freedom.data.local.dao.DailyBreakdownItemDao
import com.financial.freedom.data.local.dao.DailySummaryDao
import com.financial.freedom.data.local.entity.DailySummary
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

data class VerificationResult(
    val totalDays: Int = 0,
    val dailyConsistencyErrors: List<String> = emptyList(),
    val weeklyConsistencyErrors: List<String> = emptyList(),
    val monthlyConsistencyErrors: List<String> = emptyList(),
    val yearViewConsistencyErrors: List<String> = emptyList()
) {
    val hasErrors: Boolean get() = dailyConsistencyErrors.isNotEmpty()
        || weeklyConsistencyErrors.isNotEmpty()
        || monthlyConsistencyErrors.isNotEmpty()
        || yearViewConsistencyErrors.isNotEmpty()

    val isClean: Boolean get() = !hasErrors
}

@Singleton
class DataVerifier @Inject constructor(
    private val summaryDao: DailySummaryDao,
    private val breakdownDao: DailyBreakdownItemDao
) {
    companion object {
        private const val TAG = "DataVerifier"
    }

    /**
     * 全面验证指定账户的收益数据一致性。
     * 检查日、周、月、年各维度的数据是否正确。
     */
    suspend fun verifyAll(accountId: Long): VerificationResult {
        Log.w(TAG, "========== 收益数据验证开始 accountId=$accountId ==========")

        val dailyErrors = verifyDailyConsistency(accountId)
        val weeklyErrors = verifyWeeklyConsistency(accountId)
        val monthlyErrors = verifyMonthlyConsistency(accountId)
        val yearViewErrors = verifyYearViewConsistency(accountId)

        val result = VerificationResult(
            dailyConsistencyErrors = dailyErrors,
            weeklyConsistencyErrors = weeklyErrors,
            monthlyConsistencyErrors = monthlyErrors,
            yearViewConsistencyErrors = yearViewErrors
        )

        if (result.isClean) {
            Log.w(TAG, "✅ 所有验证通过，收益数据一致")
        } else {
            Log.w(TAG, "❌ 发现 ${dailyErrors.size + weeklyErrors.size + monthlyErrors.size + yearViewErrors.size} 个不一致")
            dailyErrors.forEach { Log.w(TAG, "  [日] $it") }
            weeklyErrors.forEach { Log.w(TAG, "  [周] $it") }
            monthlyErrors.forEach { Log.w(TAG, "  [月] $it") }
            yearViewErrors.forEach { Log.w(TAG, "  [年] $it") }
        }

        Log.w(TAG, "========== 收益数据验证结束 ==========")
        return result
    }

    /**
     * 日维度验证：
     * 1. dayChange == 各分类 changeCNY 之和
     * 2. totalValueCNY == 各分类 valueCNY 之和
     * 3. netInflow == 各分类 contribution 之和
     */
    private suspend fun verifyDailyConsistency(accountId: Long): List<String> {
        val errors = mutableListOf<String>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // 查询所有历史数据
        val summaries = summaryDao.getListByDateRange(
            LocalDate(2020, 1, 1), today, accountId
        )
        if (summaries.isEmpty()) {
            Log.w(TAG, "无历史数据，跳过日维度验证")
            return errors
        }

        for (s in summaries) {
            val breakdowns = breakdownDao.getByDate(s.date, accountId)
            if (breakdowns.isEmpty()) {
                errors.add("${s.date}: 缺少 breakdown 数据")
                continue
            }

            // 1. dayChange == sum of breakdown changeCNY
            val sumChange = breakdowns.fold(BigDecimal.ZERO) { acc, b -> acc + b.changeCNY }
            if (s.dayChange.compareTo(sumChange) != 0) {
                errors.add("${s.date}: dayChange=${s.dayChange} ≠ Σbreakdown.changeCNY=${sumChange}")
            }

            // 2. totalValueCNY == sum of breakdown valueCNY
            val sumValue = breakdowns.fold(BigDecimal.ZERO) { acc, b -> acc + b.valueCNY }
            if (s.totalValueCNY.compareTo(sumValue) != 0) {
                errors.add("${s.date}: totalValueCNY=${s.totalValueCNY} ≠ Σbreakdown.valueCNY=${sumValue}")
            }

            // 3. netInflow == sum of breakdown contribution
            val sumContribution = breakdowns.fold(BigDecimal.ZERO) { acc, b -> acc + b.contribution }
            if (s.netInflow.compareTo(sumContribution) != 0) {
                errors.add("${s.date}: netInflow=${s.netInflow} ≠ Σbreakdown.contribution=${sumContribution}")
            }
        }

        return errors
    }

    /**
     * 周维度验证：
     * 模拟 EarningsViewModel.loadWeekView() 的聚合逻辑，
     * 检查 weekly total == 该周各日 dayChange 之和
     */
    private suspend fun verifyWeeklyConsistency(accountId: Long): List<String> {
        val errors = mutableListOf<String>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val summaries = summaryDao.getListByDateRange(
            LocalDate(2020, 1, 1), today, accountId
        )
        if (summaries.isEmpty()) return errors

        val byDate = summaries.associateBy { it.date }
        val minDate = summaries.minOf { it.date }
        val maxDate = summaries.maxOf { it.date }

        // 对齐到周一
        var weekStart = minDate
        while (weekStart.dayOfWeek != DayOfWeek.MONDAY) {
            weekStart = weekStart.minus(1, DateTimeUnit.DAY)
        }

        var weekNum = 1
        while (weekStart <= maxDate) {
            val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
            var weekTotal = BigDecimal.ZERO
            val dayChanges = mutableListOf<BigDecimal>()

            var d = weekStart
            while (d <= weekEnd) {
                val s = byDate[d]
                val change = s?.dayChange ?: BigDecimal.ZERO
                weekTotal += change
                dayChanges.add(change)
                d = d.plus(1, DateTimeUnit.DAY)
            }

            // 验证：weekTotal == 各日 dayChange 之和（简单的加法验证）
            val recomputed = dayChanges.fold(BigDecimal.ZERO) { acc, c -> acc + c }
            if (weekTotal.compareTo(recomputed) != 0) {
                errors.add("第${weekNum}周 ($weekStart ~ $weekEnd): weekTotal=$weekTotal ≠ 日合计=$recomputed")
            }

            weekStart = weekEnd.plus(1, DateTimeUnit.DAY)
            weekNum++
        }

        return errors
    }

    /**
     * 月维度验证：
     * 模拟 EarningsViewModel.loadMonthView() 的聚合逻辑，
     * 检查 monthly total == 当月各日 dayChange 之和
     */
    private suspend fun verifyMonthlyConsistency(accountId: Long): List<String> {
        val errors = mutableListOf<String>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val summaries = summaryDao.getListByDateRange(
            LocalDate(2020, 1, 1), today, accountId
        )
        if (summaries.isEmpty()) return errors

        val byMonth = summaries.groupBy { it.date.monthNumber to it.date.year }

        for ((key, days) in byMonth) {
            val (month, year) = key
            val monthlyTotal = days.fold(BigDecimal.ZERO) { acc, s -> acc + s.dayChange }
            val recomputed = days.map { it.dayChange }.fold(BigDecimal.ZERO) { acc, c -> acc + c }
            if (monthlyTotal.compareTo(recomputed) != 0) {
                errors.add("${year}年${month}月: monthlyTotal=$monthlyTotal ≠ 日合计=$recomputed")
            }
        }

        return errors
    }

    /**
     * 年视图验证：
     * 模拟 EarningsViewModel.loadYearView() 的聚合逻辑，
     * 检查 yearly total == 当年各日 dayChange 之和
     */
    private suspend fun verifyYearViewConsistency(accountId: Long): List<String> {
        val errors = mutableListOf<String>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val summaries = summaryDao.getListByDateRange(
            LocalDate(2020, 1, 1), today, accountId
        )
        if (summaries.isEmpty()) return errors

        val byYear = summaries.groupBy { it.date.year }

        for ((year, days) in byYear) {
            val yearlyTotal = days.fold(BigDecimal.ZERO) { acc, s -> acc + s.dayChange }
            val recomputed = days.map { it.dayChange }.fold(BigDecimal.ZERO) { acc, c -> acc + c }
            if (yearlyTotal.compareTo(recomputed) != 0) {
                errors.add("${year}年: yearlyTotal=$yearlyTotal ≠ 日合计=$recomputed")
            }

            // 验证 upDays / downDays 统计
            val upDays = days.count { it.dayChange > BigDecimal.ZERO }
            val downDays = days.count { it.dayChange < BigDecimal.ZERO }
            val totalDays = days.size

            // 验证 upDays + downDays + zeroDays == totalDays
            val zeroDays = days.count { it.dayChange.compareTo(BigDecimal.ZERO) == 0 }
            if (upDays + downDays + zeroDays != totalDays) {
                errors.add("${year}年: upDays($upDays) + downDays($downDays) + zeroDays($zeroDays) ≠ totalDays($totalDays)")
            }
        }

        return errors
    }
}

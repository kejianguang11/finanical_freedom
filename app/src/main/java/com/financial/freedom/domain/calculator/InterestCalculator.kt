package com.financial.freedom.domain.calculator

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.until
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class InterestCalculator @Inject constructor() {

    private val daysPerYear = BigDecimal(365)

    /**
     * 计算每日利息 = 本金 × 年化利率 ÷ 365
     */
    fun dailyInterest(principal: BigDecimal, annualRate: BigDecimal): BigDecimal =
        principal.multiply(annualRate).divide(daysPerYear, 10, RoundingMode.HALF_UP)

    /**
     * 计算截至某日的累计利息
     */
    fun accruedInterest(
        principal: BigDecimal,
        annualRate: BigDecimal,
        startDate: LocalDate,
        maturityDate: LocalDate,
        asOfDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): BigDecimal {
        val endDate = minOf(maturityDate, asOfDate)
        val days = startDate.until(endDate, DateTimeUnit.DAY).toInt().coerceAtLeast(0)
        return dailyInterest(principal, annualRate).multiply(BigDecimal(days))
    }

    /**
     * 计算存款持有天数
     */
    fun holdingDays(startDate: LocalDate, maturityDate: LocalDate, asOfDate: LocalDate): Int =
        startDate.until(minOf(maturityDate, asOfDate), DateTimeUnit.DAY).toInt().coerceAtLeast(0)
}

package com.financial.freedom.domain.calculator

import com.financial.freedom.data.local.entity.Deposit
import com.financial.freedom.data.local.entity.Holding
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class ValuationCalculator @Inject constructor(
    private val interestCalculator: InterestCalculator
) {

    /**
     * 计算存款 CNY 估值 = (本金 + 累计利息) × 汇率
     */
    fun calcDepositValueCNY(
        deposit: Deposit,
        exchangeRate: BigDecimal,
        asOfDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
    ): BigDecimal {
        val interest = if (deposit.status == "active") {
            interestCalculator.accruedInterest(
                principal = deposit.principal,
                annualRate = deposit.interestRate,
                startDate = deposit.startDate,
                maturityDate = deposit.maturityDate,
                asOfDate = asOfDate
            )
        } else {
            // 已到期：利息算到到期日
            interestCalculator.accruedInterest(
                principal = deposit.principal,
                annualRate = deposit.interestRate,
                startDate = deposit.startDate,
                maturityDate = deposit.maturityDate,
                asOfDate = deposit.maturityDate
            )
        }
        return (deposit.principal + interest).multiply(exchangeRate)
            .setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * 计算持仓 CNY 估值 = 数量 × 当前单价 × 汇率
     */
    fun calcHoldingValueCNY(
        holding: Holding,
        currentPrice: BigDecimal,
        exchangeRate: BigDecimal = BigDecimal.ONE
    ): BigDecimal = holding.quantity.multiply(currentPrice).multiply(exchangeRate)
        .setScale(2, RoundingMode.HALF_UP)

    /**
     * 计算持仓盈亏
     */
    fun calcHoldingPnL(
        holding: Holding,
        currentPrice: BigDecimal,
        exchangeRate: BigDecimal = BigDecimal.ONE
    ): BigDecimal {
        val costTotal = holding.quantity.multiply(holding.costPrice).multiply(exchangeRate)
        val currentTotal = calcHoldingValueCNY(holding, currentPrice, exchangeRate)
        return currentTotal.subtract(costTotal).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * 计算持仓盈亏百分比
     */
    fun calcHoldingPnLPct(
        holding: Holding,
        currentPrice: BigDecimal,
        exchangeRate: BigDecimal = BigDecimal.ONE
    ): BigDecimal {
        val costTotal = holding.quantity.multiply(holding.costPrice).multiply(exchangeRate)
        if (costTotal.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        val currentTotal = calcHoldingValueCNY(holding, currentPrice, exchangeRate)
        return currentTotal.subtract(costTotal)
            .divide(costTotal, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
    }
}

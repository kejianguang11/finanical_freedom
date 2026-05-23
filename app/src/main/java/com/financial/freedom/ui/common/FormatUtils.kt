package com.financial.freedom.ui.common

import java.math.BigDecimal
import java.math.RoundingMode

object FormatUtils {

    /**
     * Format amount with 2 decimal places and comma grouping.
     * Multiplier is applied to the raw value before formatting.
     */
    fun formatMoney(value: BigDecimal, multiplier: BigDecimal = BigDecimal.ONE): String {
        val adjusted = value * multiplier
        val rounded = adjusted.setScale(2, RoundingMode.HALF_UP)
        val abs = rounded.abs()
        val integer = abs.toBigInteger().toString()
        val formattedInt = integer.reversed().chunked(3).joinToString(",").reversed()
        val decimal = abs.subtract(BigDecimal(abs.toBigInteger())).toPlainString().removePrefix("0")
        val full = if (decimal.isNotEmpty() && decimal != ".00") "$formattedInt$decimal" else formattedInt
        return if (rounded < BigDecimal.ZERO) "-$full" else full
    }

    /**
     * Format amount with 0 decimal places and comma grouping.
     */
    fun formatMoneyShort(value: BigDecimal, multiplier: BigDecimal = BigDecimal.ONE): String {
        val adjusted = value * multiplier
        val abs = adjusted.abs().setScale(0, RoundingMode.HALF_UP)
        val intPart = abs.toBigInteger().toString()
        val formatted = intPart.reversed().chunked(3).joinToString(",").reversed()
        return if (adjusted < BigDecimal.ZERO) "-$formatted" else formatted
    }

    /**
     * Format a signed change value with "+" prefix for positive values.
     */
    fun formatSignedChange(change: BigDecimal, multiplier: BigDecimal = BigDecimal.ONE): String {
        val formatted = formatMoney(change, multiplier)
        return if (change * multiplier >= BigDecimal.ZERO) "+$formatted" else formatted
    }

    /**
     * Format allocation value with ¥ prefix.
     */
    fun formatAllocationValue(value: BigDecimal?, multiplier: BigDecimal = BigDecimal.ONE): String {
        if (value == null) return "¥0"
        val formatted = formatMoneyShort(value, multiplier)
        return if (value * multiplier < BigDecimal.ZERO) "-¥${formatted.removePrefix("-")}" else "¥$formatted"
    }

    /**
     * Parse a formatted money string back to BigDecimal.
     */
    fun parseMoneyValue(s: String): BigDecimal? {
        return runCatching {
            BigDecimal(s.replace(",", "").replace("+", "").replace("¥", "").trim())
        }.getOrNull()
    }
}

package com.financial.freedom.domain.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisplaySettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("display_settings", Context.MODE_PRIVATE)

    private val _multiplier = MutableStateFlow(loadMultiplier())
    val multiplierFlow: StateFlow<BigDecimal> = _multiplier.asStateFlow()

    fun getMultiplier(): BigDecimal = _multiplier.value

    fun setMultiplier(value: BigDecimal) {
        prefs.edit().putString("display_multiplier", value.toPlainString()).apply()
        _multiplier.value = value
    }

    private fun loadMultiplier(): BigDecimal {
        val raw = prefs.getString("display_multiplier", null) ?: return BigDecimal.ONE
        return runCatching { BigDecimal(raw) }.getOrDefault(BigDecimal.ONE)
    }
}

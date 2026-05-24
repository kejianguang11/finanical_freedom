package com.financial.freedom.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {

    // Auth
    @Serializable data object Welcome : Route
    @Serializable data object PinUnlock : Route
    @Serializable data object AccountList : Route

    // Main
    @Serializable data object Main : Route  // Pager-based main screen (replaces Home/Holdings/Earnings/Settings/Cash/Credit)

    @Serializable data object Home : Route  // kept for backward compat

    @Serializable data class Holdings(val tab: Int = 0) : Route

    @Serializable data object Earnings : Route

    @Serializable data object Settings : Route

    @Serializable data class HoldingDetail(val holdingId: Long) : Route

    @Serializable data object AddDeposit : Route

    @Serializable data class EditDeposit(val depositId: Long) : Route

    @Serializable data class BankDeposits(val bankName: String, val status: String) : Route

    @Serializable data class AddHolding(val type: String) : Route  // STOCK / FUND / GOLD

    @Serializable data class EditHolding(val holdingId: Long) : Route

    @Serializable data class GoldDetail(val holdingId: Long) : Route

    @Serializable data object Cash : Route

    @Serializable data object Credit : Route
}

package com.financial.freedom.ui.theme

import androidx.compose.ui.graphics.Color

object FinancialColors {
    // 涨红跌绿（中国习惯）
    val up = Color(0xFFC0392B)
    val upContainer = Color(0x1AC0392B)
    val upBg = Color(0xFFFDF2F2)
    val down = Color(0xFF27AE60)
    val downContainer = Color(0x1A27AE60)
    val downBg = Color(0xFFEDF7F0)

    // 金色 CTA / 选中态
    val gold = Color(0xFFB7930A)
    val goldLight = Color(0xFFFEF9E7)

    // 资产类型色（仅用于图标和点缀）
    val deposit = Color(0xFF4A6FA5)
    val stock = Color(0xFFC0392B)
    val fund = Color(0xFF8B5CF6)
    val goldAsset = Color(0xFFB7930A)
    val cash = Color(0xFF2E7D32)
    val receivable = Color(0xFFE67E22)
    val debt = Color(0xFFE74C3C)

    // 向后兼容
    val depositBg = Color(0xFFF0F4FA)
    val stockBg = Color(0xFFFDF2F2)
    val fundBg = Color(0xFFF5F0FA)
    val goldBg = Color(0xFFFEF9E7)

    // 浅色背景暖灰
    val pageBg = Color(0xFFF5F5F7)
}

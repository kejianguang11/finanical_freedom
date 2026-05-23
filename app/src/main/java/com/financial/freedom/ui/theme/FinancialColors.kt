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

    // v18: 银行色彩身份 — 12 色低饱和金融色调色板
    val BankPalette = listOf(
        Color(0xFF4A6FA5),  // 0  稳重蓝
        Color(0xFFB84C3B),  // 1  深红
        Color(0xFF1A5276),  // 2  深蓝
        Color(0xFF8B1A1A),  // 3  暗红
        Color(0xFF2E7D32),  // 4  翠绿
        Color(0xFF6B8E23),  // 5  橄榄绿
        Color(0xFF1565C0),  // 6  亮蓝
        Color(0xFF00695C),  // 7  青绿
        Color(0xFF3E6B89),  // 8  灰蓝
        Color(0xFFC21807),  // 9  正红
        Color(0xFF7B4B94),  // 10 梅紫
        Color(0xFF8E5E2E)   // 11 棕金
    )

    // v18: 股票板块色 (按行业/板块)
    val SectorColors = mapOf(
        "白酒" to Color(0xFFB84C3B),
        "科技" to Color(0xFF1565C0),
        "金融" to Color(0xFFB7930A),
        "医药" to Color(0xFF00897B),
        "能源" to Color(0xFFE65100),
        "消费" to Color(0xFF6A1B9A),
        "制造" to Color(0xFF37474F),
        "其他" to Color(0xFF546E7A)
    )
    val defaultSectorColor = Color(0xFF546E7A)

    // v18: 基金类型色
    val FundTypeColors = mapOf(
        "指数" to Color(0xFF1565C0),
        "主动" to Color(0xFF8B5CF6),
        "债券" to Color(0xFF2E7D32),
        "混合" to Color(0xFFE67E22),
        "货币" to Color(0xFF00897B),
        "QDII" to Color(0xFFB7930A)
    )
    val defaultFundTypeColor = Color(0xFF8B5CF6)

    /** 根据名称 hash 从色板取色，保证确定性 */
    fun bankColor(name: String): Color {
        val index = name.hashCode().mod(BankPalette.size)
        return BankPalette[index]
    }

    /** 根据名称 hash 取色板索引 */
    fun bankColorIndex(name: String): Int =
        name.hashCode().mod(BankPalette.size)

    /** 获取银行名的首字符（用于图标） */
    fun bankInitial(name: String): String {
        if (name.isEmpty()) return "银"
        return name.take(1)
    }
}

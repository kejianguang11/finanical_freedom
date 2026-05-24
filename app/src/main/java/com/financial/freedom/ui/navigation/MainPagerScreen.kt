package com.financial.freedom.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.ui.cash.CashScreen
import com.financial.freedom.ui.credit.CreditScreen
import com.financial.freedom.ui.earnings.EarningsScreen
import com.financial.freedom.ui.holdings.DepositsPage
import com.financial.freedom.ui.holdings.FundPage
import com.financial.freedom.ui.holdings.GoldPage
import com.financial.freedom.ui.holdings.HoldingsUiState
import com.financial.freedom.ui.holdings.HoldingsViewModel
import com.financial.freedom.ui.holdings.StockPage
import com.financial.freedom.ui.home.HomeScreen
import com.financial.freedom.ui.settings.SettingsScreen
import com.financial.freedom.ui.theme.FinancialColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class PagerPage(val index: Int) {
    HOME(0),
    STOCK(1), FUND(2), GOLD(3),
    DEPOSIT(4),
    CASH(5), CREDIT(6),
    EARNINGS(7), SETTINGS(8);

    companion object {
        fun fromIndex(index: Int): PagerPage = entries.first { it.index == index }

        /** Bottom nav anchor → first page of that section */
        fun anchorFor(bottomNavLabel: String): Int = when (bottomNavLabel) {
            "首页" -> HOME.index
            "持仓" -> STOCK.index
            "收益" -> EARNINGS.index
            "设置" -> SETTINGS.index
            else -> HOME.index
        }
    }
}

/** Which bottom nav item is highlighted for a given page */
fun bottomNavIndexFor(page: Int): Int = when (page) {
    0 -> 0             // 首页
    in 1..6 -> 1       // 持仓 (投资+存款+现金+信用)
    7 -> 2             // 收益
    8 -> 3             // 设置
    else -> 0
}

// ===== Category Quick-Jump Data =====

private data class CategoryChip(
    val label: String,
    val anchorPage: Int,
    val pageRange: IntRange
)

private val categories = listOf(
    CategoryChip("投资", 1, 1..3),
    CategoryChip("存款", 4, 4..4),
    CategoryChip("现金", 5, 5..5),
    CategoryChip("信用", 6, 6..6)
)

@Composable
fun MainPagerScreen(
    pagerState: androidx.compose.foundation.pager.PagerState,
    coroutineScope: CoroutineScope,
    onHoldingClick: (Long) -> Unit = {},
    goldClick: (Long) -> Unit,
    onBankClick: (String, String) -> Unit = { _, _ -> },
    onAddDeposit: () -> Unit = {},
    onAddHolding: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentPage = pagerState.currentPage
    val holdingsViewModel: HoldingsViewModel = hiltViewModel()
    val holdingsState by holdingsViewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        // Category quick-jump strip — pages 1-6 only
        if (currentPage in 1..6) {
            CategoryNavStrip(
                currentPage = currentPage,
                holdingsState = holdingsState,
                onCategoryClick = { anchor ->
                    coroutineScope.launch { pagerState.animateScrollToPage(anchor) }
                }
            )
        }

        // Section indicator — pages 1-3 only (investment sub-pages)
        if (currentPage in 1..3) {
            SectionIndicator(
                currentPage = currentPage,
                holdingsState = holdingsState,
                onItemClick = { targetPage ->
                    coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0
        ) { page ->
            PageContent(
                page = PagerPage.fromIndex(page),
                pagerScope = coroutineScope,
                pagerState = pagerState,
                onHoldingClick = onHoldingClick,
                goldClick = goldClick,
                onBankClick = onBankClick,
                onAddDeposit = onAddDeposit,
                onAddHolding = onAddHolding
            )
        }
    }
}

@Composable
private fun PageContent(
    page: PagerPage,
    pagerScope: CoroutineScope,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onHoldingClick: (Long) -> Unit,
    goldClick: (Long) -> Unit,
    onBankClick: (String, String) -> Unit,
    onAddDeposit: () -> Unit,
    onAddHolding: (String) -> Unit
) {
    when (page) {
        PagerPage.HOME -> HomeScreen(
            onScrollToPage = { idx -> pagerScope.launch { pagerState.animateScrollToPage(idx) } },
            onAddDeposit = onAddDeposit,
            onAddHolding = onAddHolding
        )
        PagerPage.STOCK -> StockPage(
            onHoldingClick = onHoldingClick,
            onAddHolding = { onAddHolding("STOCK") }
        )
        PagerPage.FUND -> FundPage(
            onHoldingClick = onHoldingClick,
            onAddHolding = { onAddHolding("FUND") }
        )
        PagerPage.GOLD -> GoldPage(
            onHoldingClick = goldClick,
            onAddHolding = { onAddHolding("GOLD") }
        )
        PagerPage.DEPOSIT -> DepositsPage(onBankClick = onBankClick, onAddDeposit = onAddDeposit)
        PagerPage.CASH -> CashScreen()
        PagerPage.CREDIT -> CreditScreen()
        PagerPage.EARNINGS -> EarningsScreen()
        PagerPage.SETTINGS -> SettingsScreen()
    }
}

// ===== Category Quick-Jump Strip =====

@Composable
private fun CategoryNavStrip(
    currentPage: Int,
    holdingsState: HoldingsUiState,
    onCategoryClick: (Int) -> Unit
) {
    val activeCat = categories.find { currentPage in it.pageRange }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEach { cat ->
                    val isActive = currentPage in cat.pageRange

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onCategoryClick(cat.anchorPage) }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            cat.label,
                            fontSize = 14.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isActive) FinancialColors.gold
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    if (isActive) FinancialColors.gold
                                    else FinancialColors.gold.copy(alpha = 0f)
                                )
                        )
                    }
                }
            }

            // v17: 分类汇总行
            val summaryLabel: String
            val summaryValue: String
            val summaryToday: String
            val todayColor: androidx.compose.ui.graphics.Color

            when {
                currentPage in 1..3 -> {
                    summaryLabel = "投资总计"
                    summaryValue = "¥${holdingsState.investmentTotalValue}"
                    summaryToday = holdingsState.investmentTodayChange
                    todayColor = if (holdingsState.investmentIsUp) FinancialColors.up else FinancialColors.down
                }
                currentPage == 4 -> {
                    summaryLabel = "存款总计"
                    summaryValue = "¥${holdingsState.depositTotalValue}"
                    summaryToday = holdingsState.depositTodayInterest
                    todayColor = FinancialColors.up
                }
                currentPage == 5 -> {
                    summaryLabel = "现金余额"
                    summaryValue = ""  // cash managed separately
                    summaryToday = ""
                    todayColor = MaterialTheme.colorScheme.onSurface
                }
                currentPage == 6 -> {
                    summaryLabel = "净借出"
                    summaryValue = ""
                    summaryToday = ""
                    todayColor = MaterialTheme.colorScheme.onSurface
                }
                else -> {
                    summaryLabel = ""
                    summaryValue = ""
                    summaryToday = ""
                    todayColor = MaterialTheme.colorScheme.onSurface
                }
            }

            if (summaryLabel.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 12.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        summaryLabel,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            summaryValue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (summaryToday.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                summaryToday,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = todayColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===== Section Indicator (clickable) =====

private data class SectionInfo(
    val category: String,
    val items: List<String>,
    val targetPages: List<Int>,
    val currentIndex: Int
)

private fun sectionInfoFor(page: Int): SectionInfo = when (page) {
    1 -> SectionInfo("投资", listOf("股票", "基金", "黄金"), listOf(1, 2, 3), 0)
    2 -> SectionInfo("投资", listOf("股票", "基金", "黄金"), listOf(1, 2, 3), 1)
    3 -> SectionInfo("投资", listOf("股票", "基金", "黄金"), listOf(1, 2, 3), 2)
    else -> SectionInfo("", emptyList(), emptyList(), 0)
}

@Composable
private fun SectionIndicator(
    currentPage: Int,
    holdingsState: HoldingsUiState,
    onItemClick: (Int) -> Unit
) {
    val info = sectionInfoFor(currentPage)
    if (info.category.isEmpty()) return

    // v17: 确定当前子类的汇总数据
    val subTotalValue: String
    val subToday: String
    val subTodayColor: androidx.compose.ui.graphics.Color

    when (currentPage) {
        1 -> {
            subTotalValue = "股票市值  ¥${holdingsState.stockTotalValue}"
            subToday = holdingsState.stockTodayChange
            subTodayColor = if (holdingsState.stockIsUp) FinancialColors.up else FinancialColors.down
        }
        2 -> {
            subTotalValue = "基金市值  ¥${holdingsState.fundTotalValue}"
            subToday = holdingsState.fundTodayChange
            subTodayColor = if (holdingsState.fundIsUp) FinancialColors.up else FinancialColors.down
        }
        3 -> {
            subTotalValue = "黄金市值  ¥${holdingsState.goldTotalValue}"
            subToday = holdingsState.goldTodayChange
            subTodayColor = if (holdingsState.goldIsUp) FinancialColors.up else FinancialColors.down
        }
        else -> {
            subTotalValue = ""
            subToday = ""
            subTodayColor = MaterialTheme.colorScheme.onSurface
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                info.category,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                info.items.forEachIndexed { index, name ->
                    val isCurrent = index == info.currentIndex
                    Text(
                        name,
                        fontSize = 15.sp,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isCurrent)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            info.targetPages.getOrNull(index)?.let { onItemClick(it) }
                        }
                    )
                    if (index < info.items.size - 1) {
                        Text(
                            "  ·  ",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(info.items.size) { dotIndex ->
                    val isActive = dotIndex == info.currentIndex
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) FinancialColors.gold
                                else FinancialColors.gold.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            // v17: 子类汇总行
            if (subTotalValue.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        subTotalValue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subToday.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            subToday,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = subTodayColor
                        )
                    }
                }
            }
        }
    }
}

package com.financial.freedom.ui.holdings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.ui.theme.FinancialColors
import kotlinx.coroutines.launch

private val topTabs = listOf("持仓", "存款", "现金", "信用")
private val holdingSubTabs = listOf("股票", "基金", "黄金")
private val depositSubTabs = listOf("持有中", "已到期")

@Composable
fun HoldingsScreen(
    initialTab: Int = 0,
    onHoldingClick: (Long) -> Unit = {},
    onDepositClick: (Long) -> Unit = {},
    onAddDeposit: () -> Unit = {},
    onAddHolding: (String) -> Unit = {},
    onNavigateToCash: () -> Unit = {},
    onNavigateToCredit: () -> Unit = {},
    viewModel: HoldingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val topPagerState = rememberPagerState(initialPage = 0, pageCount = { topTabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        // Top-level tab row: 持仓 / 存款 / 现金 / 信用
        ScrollableTabRow(
            selectedTabIndex = topPagerState.currentPage,
            modifier = Modifier.padding(horizontal = 16.dp),
            containerColor = MaterialTheme.colorScheme.background,
            edgePadding = 0.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[topPagerState.currentPage]),
                    color = FinancialColors.gold
                )
            }
        ) {
            topTabs.forEachIndexed { index, title ->
                Tab(
                    selected = topPagerState.currentPage == index,
                    onClick = {
                        if (index == 2) { onNavigateToCash(); return@Tab }
                        if (index == 3) { onNavigateToCredit(); return@Tab }
                        coroutineScope.launch { topPagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            title,
                            fontWeight = if (topPagerState.currentPage == index) FontWeight.SemiBold
                            else FontWeight.Normal,
                            color = if (topPagerState.currentPage == index)
                                MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = topPagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false // 禁止滑动到现金/信用（它们会导航离开）
        ) { page ->
            when (page) {
                0 -> HoldingSubTabs(state, onHoldingClick, onAddHolding)
                1 -> DepositSubTabs(state, onDepositClick, onAddDeposit)
                else -> {} // pages 2/3 handled by navigation
            }
        }
    }
}

// ===== 持仓子标签：股票 / 基金 / 黄金 =====
@Composable
private fun HoldingSubTabs(
    state: HoldingsUiState,
    onHoldingClick: (Long) -> Unit,
    onAddHolding: (String) -> Unit
) {
    val subPagerState = rememberPagerState(pageCount = { holdingSubTabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = subPagerState.currentPage,
            modifier = Modifier.padding(horizontal = 16.dp),
            containerColor = MaterialTheme.colorScheme.background,
            edgePadding = 0.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[subPagerState.currentPage]),
                    color = FinancialColors.gold
                )
            }
        ) {
            holdingSubTabs.forEachIndexed { index, title ->
                Tab(
                    selected = subPagerState.currentPage == index,
                    onClick = { coroutineScope.launch { subPagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            title,
                            fontWeight = if (subPagerState.currentPage == index) FontWeight.SemiBold
                            else FontWeight.Normal,
                            color = if (subPagerState.currentPage == index)
                                MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        HorizontalPager(state = subPagerState, modifier = Modifier.fillMaxSize()) { page ->
            val padMod = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            when (page) {
                0 -> StockTab(state, padMod, onHoldingClick, onAddHolding)
                1 -> FundTab(state, padMod, onHoldingClick, onAddHolding)
                2 -> GoldTab(state, padMod, onHoldingClick, onAddHolding)
            }
        }
    }
}

// ===== 存款子标签：持有中 / 已到期 =====
@Composable
private fun DepositSubTabs(
    state: HoldingsUiState,
    onDepositClick: (Long) -> Unit,
    onAddDeposit: () -> Unit
) {
    val subPagerState = rememberPagerState(pageCount = { depositSubTabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = subPagerState.currentPage,
            modifier = Modifier.padding(horizontal = 16.dp),
            containerColor = MaterialTheme.colorScheme.background,
            edgePadding = 0.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[subPagerState.currentPage]),
                    color = FinancialColors.gold
                )
            }
        ) {
            depositSubTabs.forEachIndexed { index, title ->
                Tab(
                    selected = subPagerState.currentPage == index,
                    onClick = { coroutineScope.launch { subPagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            title,
                            fontWeight = if (subPagerState.currentPage == index) FontWeight.SemiBold
                            else FontWeight.Normal,
                            color = if (subPagerState.currentPage == index)
                                MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        HorizontalPager(state = subPagerState, modifier = Modifier.fillMaxSize()) { page ->
            val padMod = Modifier.fillMaxSize().padding(horizontal = 16.dp)
            when (page) {
                0 -> ActiveDepositTab(state, padMod, onDepositClick, onAddDeposit)
                1 -> MaturedDepositTab(state, padMod, onDepositClick)
            }
        }
    }
}

@Composable
private fun ActiveDepositTab(state: HoldingsUiState, modifier: Modifier, onDepositClick: (Long) -> Unit, onAddDeposit: () -> Unit) {
    LazyColumn(modifier = modifier) {
        if (state.deposits.isEmpty()) {
            item { EmptyHint("暂无存款", "点击 + 添加") { onAddDeposit() } }
        } else {
            items(state.deposits, key = { "deposit_${it.id}" }) { d ->
                DepositCard(d, onClick = { onDepositClick(d.id) })
                Spacer(Modifier.height(12.dp))
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun MaturedDepositTab(state: HoldingsUiState, modifier: Modifier, onDepositClick: (Long) -> Unit) {
    LazyColumn(modifier = modifier) {
        if (state.maturedDeposits.isEmpty()) {
            item { EmptyHint("暂无已到期存款", "到期存款将自动赎回到现金") {} }
        } else {
            items(state.maturedDeposits, key = { "matured_${it.id}" }) { d ->
                MaturedDepositCard(d, onClick = { onDepositClick(d.id) })
                Spacer(Modifier.height(12.dp))
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun StockTab(state: HoldingsUiState, modifier: Modifier, onHoldingClick: (Long) -> Unit, onAddHolding: (String) -> Unit) {
    LazyColumn(modifier = modifier) {
        if (state.stocks.isEmpty()) {
            item { EmptyHint("暂无股票", "点击 + 添加") { onAddHolding("STOCK") } }
        } else {
            items(state.stocks, key = { "stock_${it.id}" }) { h ->
                HoldingCard(h, onClick = { onHoldingClick(h.id) })
                Spacer(Modifier.height(12.dp))
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun FundTab(state: HoldingsUiState, modifier: Modifier, onHoldingClick: (Long) -> Unit, onAddHolding: (String) -> Unit) {
    LazyColumn(modifier = modifier) {
        if (state.funds.isEmpty()) {
            item { EmptyHint("暂无基金", "点击 + 添加") { onAddHolding("FUND") } }
        } else {
            items(state.funds, key = { "fund_${it.id}" }) { h ->
                HoldingCard(h, onClick = { onHoldingClick(h.id) })
                Spacer(Modifier.height(12.dp))
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun GoldTab(state: HoldingsUiState, modifier: Modifier, onHoldingClick: (Long) -> Unit, onAddHolding: (String) -> Unit) {
    LazyColumn(modifier = modifier) {
        if (state.golds.isEmpty()) {
            item { EmptyHint("暂无黄金", "点击 + 添加") { onAddHolding("GOLD") } }
        } else {
            items(state.golds, key = { "gold_${it.id}" }) { h ->
                HoldingCard(h, onClick = { onHoldingClick(h.id) })
                Spacer(Modifier.height(12.dp))
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ===== Deposit Card (Active - 持有中) v18 compact =====
@Composable
fun DepositCard(deposit: DepositDisplay, onClick: () -> Unit = {}) {
    val animatedProgress by animateFloatAsState(
        targetValue = deposit.progress,
        animationSpec = tween(800),
        label = "progress"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(FinancialColors.deposit, FinancialColors.deposit.copy(alpha = 0.3f))
                        ),
                        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
            )
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).weight(1f)) {
                // 行 1：银行名 + 当前估值
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(deposit.bank, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(deposit.currentValue, fontWeight = FontWeight.Bold,
                        fontSize = 17.sp, color = FinancialColors.deposit)
                }
                Spacer(Modifier.height(4.dp))

                // 行 2：本金 · 利率 | 今日利息
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("本金 ${deposit.principal} · ${deposit.rate}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(deposit.todayInterest, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (deposit.todayInterest.startsWith("+")) FinancialColors.up else FinancialColors.down)
                }
                Spacer(Modifier.height(6.dp))

                // 行 3：日期范围 + 进度
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${deposit.startDate} → ${deposit.maturityDate}", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${deposit.holdingDays}/${deposit.totalDays}天 (${(deposit.progress * 100).toInt()}%)",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = FinancialColors.deposit,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(6.dp))

                // 行 4：已产生利息 + 年利率
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("累计利息 ${deposit.accruedInterest}", fontSize = 12.sp,
                        fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text("年利率 ${deposit.rate}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ===== Matured Deposit Card (已到期) v18 compact =====
@Composable
fun MaturedDepositCard(deposit: DepositDisplay, onClick: () -> Unit = {}) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(FinancialColors.deposit.copy(alpha = 0.4f),
                        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(deposit.bank, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("已到期 ✓", fontSize = 11.sp,
                        color = FinancialColors.down, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("本金 ${deposit.principal}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("总利息 ${deposit.accruedInterest}", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("持有 ${deposit.totalDays} 天 · 到期 ${deposit.maturityDate}", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${deposit.maturityDate} 自动赎回", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ===== Holding Card (股票/基金/黄金) =====
@Composable
fun HoldingCard(holding: HoldingDisplay, onClick: () -> Unit = {}) {
    val typeColor = when (holding.type) {
        "STOCK" -> FinancialColors.stock
        "FUND" -> FinancialColors.fund
        "GOLD" -> FinancialColors.goldAsset
        else -> FinancialColors.deposit
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(typeColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )
            Column(Modifier.padding(16.dp).weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(holding.name, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(holding.symbol, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("持有 ${holding.quantity}", style = MaterialTheme.typography.labelMedium,
                        color = typeColor, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("成本 ${holding.costPrice}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("现价 ${holding.currentPrice}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                val valueLabel = if (holding.currency != "CNY") "市值 (¥${holding.marketValue})" else "市值 ¥${holding.marketValue}"
                Text(valueLabel, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("持仓盈亏", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        val pnlColor = when {
                            holding.totalPnL.startsWith("+") -> FinancialColors.up
                            holding.totalPnL.startsWith("-") -> FinancialColors.down
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Text(holding.totalPnL, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = pnlColor)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("今日", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        val todayColor = when {
                            holding.todayChange.startsWith("+") -> FinancialColors.up
                            holding.todayChange.startsWith("-") -> FinancialColors.down
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Text(holding.todayChange, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = todayColor)
                    }
                }
            }
        }
    }
}


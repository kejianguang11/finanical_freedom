package com.financial.freedom.ui.holdings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.ui.theme.FinancialColors
import kotlinx.coroutines.launch

private val tabs = listOf("存款", "股票", "基金", "黄金")

@Composable
fun HoldingsScreen(
    initialTab: Int = 0,
    onHoldingClick: (Long) -> Unit = {},
    onAddDeposit: () -> Unit = {},
    onAddHolding: (String) -> Unit = {},
    viewModel: HoldingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(initialPage = initialTab, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val padMod = Modifier.fillMaxSize().padding(horizontal = 16.dp)

            when (page) {
                0 -> DepositTab(state, padMod, onAddDeposit)
                1 -> StockTab(state, padMod, onHoldingClick, onAddHolding)
                2 -> FundTab(state, padMod, onHoldingClick, onAddHolding)
                3 -> GoldTab(state, padMod, onHoldingClick, onAddHolding)
            }
        }
    }
}

@Composable
private fun DepositTab(state: HoldingsUiState, modifier: Modifier, onAddDeposit: () -> Unit) {
    LazyColumn(modifier = modifier) {
        if (state.deposits.isEmpty()) {
            item { EmptyHint("暂无存款", "点击 + 添加") { onAddDeposit() } }
        } else {
            items(state.deposits, key = { "deposit_${it.id}" }) { d ->
                DepositCard(d)
                Spacer(Modifier.height(8.dp))
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
                Spacer(Modifier.height(8.dp))
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
                Spacer(Modifier.height(8.dp))
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
                Spacer(Modifier.height(8.dp))
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun DepositCard(deposit: DepositDisplay) {
    val animatedProgress by animateFloatAsState(
        targetValue = deposit.progress,
        animationSpec = tween(800),
        label = "progress"
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(deposit.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "¥${deposit.currentValue}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FinancialColors.deposit
                )
            }

            Spacer(Modifier.height(2.dp))

            Text("${deposit.bank}  ·  本金 ${deposit.principal}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(12.dp))

            // 时间线和进度条
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("存入 ${deposit.startDate}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("到期 ${deposit.maturityDate}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = FinancialColors.deposit,
                trackColor = FinancialColors.depositBg,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "持有 ${deposit.holdingDays} / ${deposit.totalDays} 天  (${(deposit.progress * 100).toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            HorizontalDivider()

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("已产生利息", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${deposit.accruedInterest} ${deposit.currency}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("年利率", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    Text(deposit.rate, style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("今日利息", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    deposit.todayInterest,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (deposit.todayInterest.startsWith("+")) FinancialColors.up else FinancialColors.down
                )
            }
        }
    }
}

@Composable
private fun HoldingCard(holding: HoldingDisplay, onClick: () -> Unit = {}) {
    val typeColor = when (holding.type) {
        "STOCK" -> FinancialColors.stock
        "FUND" -> FinancialColors.fund
        "GOLD" -> FinancialColors.gold
        else -> FinancialColors.deposit
    }
    val typeBg = when (holding.type) {
        "STOCK" -> FinancialColors.stockBg
        "FUND" -> FinancialColors.fundBg
        "GOLD" -> FinancialColors.goldBg
        else -> FinancialColors.depositBg
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${holding.name}  ${holding.symbol}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "持有 ${holding.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "成本 ${holding.costPrice}  ·  现价 ${holding.currentPrice}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("持仓盈亏", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    val pnlColor = if (holding.totalPnL.startsWith("+")) FinancialColors.up
                    else if (holding.totalPnL.startsWith("-")) FinancialColors.down
                    else MaterialTheme.colorScheme.onSurface
                    Text(holding.totalPnL, style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold, color = pnlColor, fontSize = 18.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("今日", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    val todayColor = if (holding.todayChange.startsWith("+")) FinancialColors.up
                    else if (holding.todayChange.startsWith("-")) FinancialColors.down
                    else MaterialTheme.colorScheme.onSurface
                    Text(holding.todayChange, style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold, color = todayColor, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

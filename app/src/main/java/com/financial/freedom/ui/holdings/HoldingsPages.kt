package com.financial.freedom.ui.holdings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.ui.theme.FinancialColors

// ===== Stock Page (Pager page 1) =====
@Composable
fun StockPage(
    onHoldingClick: (Long) -> Unit = {},
    onAddHolding: () -> Unit = {},
    viewModel: HoldingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    HoldingGroupListPage(
        groups = state.stockGroups,
        onHoldingClick = onHoldingClick,
        onAddHolding = onAddHolding,
        emptyTitle = "暂无股票",
        emptySubtitle = "点击 + 添加股票持仓"
    )
}

// ===== Fund Page (Pager page 2) =====
@Composable
fun FundPage(
    onHoldingClick: (Long) -> Unit = {},
    onAddHolding: () -> Unit = {},
    viewModel: HoldingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    HoldingGroupListPage(
        groups = state.fundGroups,
        onHoldingClick = onHoldingClick,
        onAddHolding = onAddHolding,
        emptyTitle = "暂无基金",
        emptySubtitle = "点击 + 添加基金持仓"
    )
}

// ===== Gold Page (Pager page 3) =====
@Composable
fun GoldPage(
    onHoldingClick: (Long) -> Unit = {},
    onAddHolding: () -> Unit = {},
    viewModel: HoldingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    HoldingListPage(
        items = state.golds,
        itemKey = { "gold_${it.id}" },
        onHoldingClick = onHoldingClick,
        onAddHolding = onAddHolding,
        emptyTitle = "暂无黄金",
        emptySubtitle = "点击 + 添加黄金持仓"
    )
}

// ===== Deposits Page (Pager page 4) — v18 合并持有中+已到期 =====
@Composable
fun DepositsPage(
    onBankClick: (String, String) -> Unit = { _, _ -> },
    onAddDeposit: () -> Unit = {},
    viewModel: HoldingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            if (state.bankGroups.isEmpty()) {
                item {
                    EmptyHint("暂无存款", "点击 + 添加定期存款", onClick = onAddDeposit)
                }
            } else {
                itemsIndexed(state.bankGroups, key = { _, g -> "bank_${g.bank}" }) { index, group ->
                    val allMatured = group.maturedCount >= group.depositCount
                    BankGroupCard(
                        group = group,
                        isMatured = allMatured,
                        onClick = { onBankClick(group.bank, if (allMatured) "matured" else "active") }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = onAddDeposit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加存款")
        }
    }
}

// MaturedDepositsPage removed in v18 — matured deposits shown inline in DepositsPage

// ===== Shared list composable for stock/fund/gold =====
@Composable
private fun HoldingListPage(
    items: List<HoldingDisplay>,
    itemKey: (HoldingDisplay) -> String,
    onHoldingClick: (Long) -> Unit,
    onAddHolding: () -> Unit,
    emptyTitle: String,
    emptySubtitle: String
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            if (items.isEmpty()) {
                item {
                    EmptyHint(emptyTitle, emptySubtitle, onClick = onAddHolding)
                }
            } else {
                itemsIndexed(items, key = { _, h -> itemKey(h) }) { index, h ->
                    HoldingCard(h, onClick = { onHoldingClick(h.id) })
                    Spacer(Modifier.height(12.dp))
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = onAddHolding,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加")
        }
    }
}

// ===== Shared group list for stock/fund (v18 — per-group sector color) =====
@Composable
private fun HoldingGroupListPage(
    groups: List<HoldingGroupDisplay>,
    onHoldingClick: (Long) -> Unit,
    onAddHolding: () -> Unit,
    emptyTitle: String,
    emptySubtitle: String
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            if (groups.isEmpty()) {
                item {
                    EmptyHint(emptyTitle, emptySubtitle, onClick = onAddHolding)
                }
            } else {
                itemsIndexed(groups, key = { _, group -> "${group.type}_${group.symbol}" }) { index, group ->
                    HoldingGroupCard(
                        group = group,
                        groupColor = group.sectorColor,
                        onClick = { onHoldingClick(group.mainHoldingId) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = onAddHolding,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加")
        }
    }
}

// ===== Holding Group Card (v18) — entire card tappable, market value + P&L prominent =====
@Composable
private fun HoldingGroupCard(
    group: HoldingGroupDisplay,
    groupColor: Color,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val hasBuyRecords = group.buyRecords.isNotEmpty()

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
            // 左侧色条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(groupColor, groupColor.copy(alpha = 0.3f))
                        ),
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )

            Column(Modifier.padding(14.dp).weight(1f)) {
                // 头部：symbol + 市场标签 + 名称
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(
                            group.symbol,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            group.market,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        group.name,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 核心信息：市值（大字号）+ 今日涨跌
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("持仓市值", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            group.marketValue,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            group.todayChange,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (group.isUp) FinancialColors.up else FinancialColors.down
                        )
                        Text(
                            "今日 ${group.todayChangePct}",
                            fontSize = 12.sp,
                            color = if (group.isUp) FinancialColors.up else FinancialColors.down
                        )
                    }
                }

                // 迷你走势图
                if (group.priceHistory.size >= 2) {
                    Spacer(Modifier.height(10.dp))
                    val sparklineColor = if (group.priceHistory.last() >= group.priceHistory.first())
                        FinancialColors.up else FinancialColors.down
                    MiniSparkline(
                        prices = group.priceHistory,
                        lineColor = sparklineColor,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(10.dp))

                // 持仓盈亏 + 持有信息
                val unitLabel = if (group.type == "FUND") "份" else "股"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            group.totalPnL,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (group.isUp) FinancialColors.up else FinancialColors.down
                        )
                        Text(
                            "持仓盈亏 ${group.totalPnLPct}",
                            fontSize = 12.sp,
                            color = if (group.isUp) FinancialColors.up else FinancialColors.down
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "持有 ${group.totalQuantity} $unitLabel · 均价 ${group.avgCost}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "现价 ${group.currentPrice}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 买入记录展开/折叠
                if (hasBuyRecords) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { expanded = !expanded }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (expanded) "▼ ${group.buyRecords.size} 笔买入记录"
                            else "▶ ${group.buyRecords.size} 笔买入记录",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (expanded) "收起" else "展开",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            group.buyRecords.forEachIndexed { index, record ->
                                BuyRecordItem(
                                    record = record,
                                    index = index,
                                    totalRecords = group.buyRecords.size,
                                    groupColor = groupColor
                                )
                                if (index < group.buyRecords.size - 1) {
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuyRecordItem(
    record: BuyRecordDisplay,
    index: Int,
    totalRecords: Int,
    groupColor: Color
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "买入 #${totalRecords - index}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (index == totalRecords - 1) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "（首次）",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    record.date,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "${record.quantity} 股 @ ${record.price}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("成本", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(record.cost, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("当前市值", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(record.currentValue, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "盈亏 ${record.pnl} (${record.pnlPct})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (record.isUp) FinancialColors.up else FinancialColors.down
            )
        }
    }
}

// ===== Bank Group Card (v18) — 银行专属色 + 首字图标 =====
@Composable
fun BankGroupCard(
    group: BankGroupDisplay,
    isMatured: Boolean = false,
    onClick: () -> Unit = {}
) {
    val bankColor = FinancialColors.BankPalette.getOrElse(group.colorIndex) { FinancialColors.deposit }
    val allMatured = group.maturedCount >= group.depositCount
    val cardAlpha = if (allMatured) 0.6f else 1f

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (allMatured) 2.dp else 3.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
            // 左侧色条 — 银行专属色
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                bankColor.copy(alpha = cardAlpha),
                                bankColor.copy(alpha = 0.3f * cardAlpha)
                            )
                        ),
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )
            Column(Modifier.padding(12.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // v18: 银行首字图标
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bankColor.copy(alpha = cardAlpha)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                FinancialColors.bankInitial(group.bank),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    group.bank,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha)
                                )
                                // v18: 已到期 badge
                                if (group.maturedCount > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "已到期 ${group.maturedCount}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFC0392B),
                                        modifier = Modifier
                                            .background(
                                                Color(0xFFC0392B).copy(alpha = 0.1f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "${group.depositCount} 笔存单",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("本金合计", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥${group.totalPrincipal}", fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("当前估值", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¥${group.totalCurrentValue}", fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = bankColor.copy(alpha = cardAlpha))
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (!allMatured) {
                    LinearProgressIndicator(
                        progress = { group.weightedProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = bankColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "综合进度 ${(group.weightedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "最早到期 ${group.nearestMaturity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "已全部到期",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("今日利息", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        group.todayTotalInterest,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (group.isInterestUp) FinancialColors.up else FinancialColors.down
                    )
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("查看存单", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 2.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ===== Mini Sparkline (48dp, no axes, 30-day trend) =====
@Composable
private fun MiniSparkline(
    prices: List<java.math.BigDecimal>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val values = remember(prices) { prices.map { it.toDouble() } }
    val minVal = values.min()
    val maxVal = values.max()
    val range = (maxVal - minVal).coerceAtLeast(0.01)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stepX = if (values.size > 1) w / (values.size - 1) else 0f

        // Fill area below line (10% opacity)
        val fillPath = Path().apply {
            moveTo(0f, h)
            values.forEachIndexed { i, v ->
                val x = i * stepX
                val y = h - ((v - minVal) / range * h).toFloat()
                lineTo(x, y)
            }
            lineTo((values.size - 1) * stepX, h)
            close()
        }
        drawPath(fillPath, lineColor.copy(alpha = 0.1f))

        // Line (1.5dp)
        val linePath = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = h - ((v - minVal) / range * h).toFloat()
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        drawPath(linePath, lineColor, style = Stroke(width = 1.5.dp.toPx()))
    }
}

// ===== Reusable EmptyHint (shared) =====
@Composable
fun EmptyHint(title: String, subtitle: String, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
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

package com.financial.freedom.ui.holdings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.ui.theme.FinancialColors
import java.math.BigDecimal

private val detailRanges = listOf("1月", "3月", "1年")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingDetailScreen(
    holdingId: Long,
    onEdit: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: HoldingDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showPercentage by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(holdingId) { viewModel.loadHolding(holdingId) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("删除「${state.name}」后将无法恢复，确定删除吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteHolding { onBack() } }) {
                    Text("删除", color = FinancialColors.up)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("${state.name} (${state.symbol})")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── 当前市值（Hero）──
            Text(
                "当前市值",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "¥ ${state.marketValue}",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val changeColor = if (state.isUp) FinancialColors.up else FinancialColors.down
                Text(
                    text = "${state.todayChange} (${state.todayChangePct})",
                    color = changeColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "今日",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── 现价 + 走势图 ──
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().height(240.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    // 现价行：单价 + 今日涨跌额/涨幅 同行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "现价",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "¥${state.currentPrice}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val pricePctClean = state.priceChangePct.removeSuffix("%")
                            val pricePctVal = pricePctClean.toDoubleOrNull() ?: 0.0
                            val priceChgColor = when {
                                pricePctVal > 0 -> FinancialColors.up
                                pricePctVal < 0 -> FinancialColors.down
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                text = state.priceChange,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = priceChgColor
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "(${state.priceChangePct})",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = priceChgColor
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Range tabs + %/¥ toggle
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            detailRanges.forEachIndexed { i, label ->
                                val isSelected = state.selectedRange == i
                                val textColor by animateColorAsState(
                                    if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { viewModel.selectRange(i) }
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = textColor
                                    )
                                    if (isSelected) {
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            Modifier
                                                .width(20.dp)
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(1.5.dp))
                                                .background(FinancialColors.gold)
                                        )
                                    }
                                }
                            }
                        }
                        // 价格 / 百分比切换
                        Text(
                            text = if (showPercentage) "%" else "¥",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = FinancialColors.gold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showPercentage = !showPercentage }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.priceHistory.isNotEmpty()) {
                        val sorted = state.priceHistory.sortedBy { it.date }
                        val chartData = sorted.mapIndexed { i, snap ->
                            val prevPrice = if (i > 0) sorted[i - 1].unitPrice else snap.unitPrice
                            val dayChg = snap.unitPrice.subtract(prevPrice)
                            val dayChgPct = if (prevPrice > BigDecimal.ZERO)
                                dayChg.divide(prevPrice, 6, java.math.RoundingMode.HALF_UP)
                                    .multiply(java.math.BigDecimal(100))
                                    .setScale(2, java.math.RoundingMode.HALF_UP)
                            else BigDecimal.ZERO
                            com.financial.freedom.data.local.entity.DailySummary(
                                date = snap.date,
                                totalValueCNY = snap.unitPrice,
                                dayChange = dayChg,
                                dayChangePct = dayChgPct
                            )
                        }
                        com.financial.freedom.ui.components.TrendChart(
                            data = chartData,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            showPercentage = showPercentage
                        )
                    } else {
                        Box(
                            Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "暂无价格数据",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 盈亏双卡片
            Row(Modifier.fillMaxWidth()) {
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "持仓盈亏",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            state.totalPnL,
                            color = if (state.isUp) FinancialColors.up else FinancialColors.down,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            state.totalPnLPct,
                            color = if (state.isUp) FinancialColors.up else FinancialColors.down,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "今日盈亏",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            state.todayChange,
                            color = if (state.isUp) FinancialColors.up else FinancialColors.down,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            state.todayChangePct,
                            color = if (state.isUp) FinancialColors.up else FinancialColors.down,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 持有信息 ──
            Text(
                "持有信息",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    InfoRow("持有数量", state.quantity)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    InfoRow("成本价", "¥${state.costPrice}")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    InfoRow("总成本", "¥${state.totalCost}")
                }
            }

            Spacer(Modifier.height(20.dp))

            // 交易记录
            if (state.transactions.isNotEmpty()) {
                Text(
                    "交易记录",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                state.transactions.take(5).forEach { txn ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${txn.date}  ${txn.type}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "${txn.quantity} × ¥${txn.price}",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 加仓/减仓按钮（仅股票/基金，且 active 状态）
            if (state.type != "GOLD" && state.status == "active") {
                Row(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { viewModel.showAddDialog() },
                        modifier = Modifier.weight(1f)
                    ) { Text("+ 加仓") }
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.showReduceDialog() },
                        modifier = Modifier.weight(1f)
                    ) { Text("- 减仓") }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 已清仓标记
            if (state.status == "closed") {
                Text(
                    "已清仓",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
            }

            // 操作按钮
            Row(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text("编辑")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("删除")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // 加仓弹窗
    if (state.showAddDialog) {
        AddPositionDialog(
            currentQty = state.quantity,
            currentAvgCost = state.costPrice,
            holdingName = state.name,
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { qty, price, date, deduct ->
                viewModel.addPosition(qty, price, date, deduct,
                    onSuccess = { viewModel.hideAddDialog() },
                    onError = { /* handled in dialog */ }
                )
            }
        )
    }

    // 减仓弹窗
    if (state.showReduceDialog) {
        ReducePositionDialog(
            currentQty = state.quantity,
            currentAvgCost = state.costPrice,
            holdingName = state.name,
            onDismiss = { viewModel.hideReduceDialog() },
            onConfirm = { qty, price, date, addToCash ->
                viewModel.reducePosition(qty, price, date, addToCash,
                    onSuccess = { viewModel.hideReduceDialog() },
                    onError = { /* handled in dialog */ }
                )
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

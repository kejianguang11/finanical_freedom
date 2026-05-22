package com.financial.freedom.ui.holdings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.ui.components.TrendChart
import com.financial.freedom.ui.theme.FinancialColors
import java.math.BigDecimal

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

    LaunchedEffect(holdingId) { viewModel.loadHolding(holdingId) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("删除「${state.name}」后将无法恢复，确定删除吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteHolding { onBack() } }) { Text("删除", color = FinancialColors.up) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${state.name} (${state.symbol})") },
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
            // 当前价格
            Text("¥ ${state.currentPrice}", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Row {
                Text(
                    text = "${state.todayChange} (${state.todayChangePct}) 今日",
                    color = if (state.isUp) FinancialColors.up else FinancialColors.down,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(16.dp))

            // 价格走势图
            Card(
                modifier = Modifier.fillMaxWidth().height(170.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1月", "3月", "1年").forEachIndexed { i, label ->
                            FilterChip(
                                selected = state.selectedRange == i,
                                onClick = { viewModel.selectRange(i) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.priceHistory.isNotEmpty()) {
                        // Compute day-over-day changes for the price chart
                        val sorted = state.priceHistory.sortedBy { it.date }
                        val chartData = sorted.mapIndexed { i, snap ->
                            val prevPrice = if (i > 0) sorted[i - 1].unitPrice else snap.unitPrice
                            val dayChg = snap.unitPrice.subtract(prevPrice)
                            val dayChgPct = if (prevPrice > BigDecimal.ZERO)
                                dayChg.divide(prevPrice, 6, java.math.RoundingMode.HALF_UP)
                                    .multiply(java.math.BigDecimal(100)).setScale(2, java.math.RoundingMode.HALF_UP)
                            else BigDecimal.ZERO
                            com.financial.freedom.data.local.entity.DailySummary(
                                date = snap.date,
                                totalValueCNY = snap.unitPrice,
                                dayChange = dayChg,
                                dayChangePct = dayChgPct
                            )
                        }
                        TrendChart(data = chartData, modifier = Modifier.fillMaxWidth().height(110.dp))
                    } else {
                        Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
                            Text("暂无价格数据", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 盈亏双卡片
            Row(Modifier.fillMaxWidth()) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("持仓盈亏", style = MaterialTheme.typography.labelSmall)
                        Text(state.totalPnL,
                            color = if (state.isUp) FinancialColors.up else FinancialColors.down,
                            fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(state.totalPnLPct,
                            color = if (state.isUp) FinancialColors.up else FinancialColors.down,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Card(modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("今日盈亏", style = MaterialTheme.typography.labelSmall)
                        Text(state.todayChange,
                            color = if (state.isUp) FinancialColors.up else FinancialColors.down,
                            fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(state.todayChangePct,
                            color = if (state.isUp) FinancialColors.up else FinancialColors.down,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 持有信息
            Text("持有信息", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            InfoRow("持有数量", state.quantity)
            InfoRow("成本价", "¥${state.costPrice}")
            InfoRow("总成本", "¥${state.totalCost}")
            InfoRow("当前市值", "¥${state.marketValue}")

            Spacer(Modifier.height(16.dp))

            // 交易记录
            if (state.transactions.isNotEmpty()) {
                Text("交易记录", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                state.transactions.take(5).forEach { txn ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${txn.date}  ${txn.type}")
                        Text("${txn.quantity} × ¥${txn.price}")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

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
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

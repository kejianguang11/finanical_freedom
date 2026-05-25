package com.financial.freedom.ui.holdings.deposit

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.ui.holdings.DepositDisplay
import com.financial.freedom.ui.theme.FinancialColors
import kotlin.math.roundToInt

data class MaturedBankGroup(
    val bank: String,
    val deposits: List<DepositDisplay>,
    val totalPrincipal: String,
    val totalAmount: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaturedDepositsScreen(
    onBack: () -> Unit,
    viewModel: MaturedDepositsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var settleWithCashId by remember { mutableStateOf(-1L) }
    var settleOnlyId by remember { mutableStateOf(-1L) }
    var deleteTargetId by remember { mutableStateOf(-1L) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("已到期存单", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${state.totalCount} 笔 · 本息合计 ${state.grandTotal}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (state.bankGroups.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无已到期存单", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
            ) {
                state.bankGroups.forEachIndexed { groupIdx, group ->
                    item(key = "matured_header_${group.bank}") {
                        Spacer(Modifier.height(8.dp))
                        BankGroupHeader(group = group)
                        Spacer(Modifier.height(8.dp))
                    }
                    itemsIndexed(
                        group.deposits,
                        key = { _, d -> "matured_deposit_${d.id}" }
                    ) { index, deposit ->
                        MaturedDepositCard(
                            deposit = deposit,
                            index = index,
                            totalCount = group.deposits.size,
                            onSettleWithCash = { settleWithCashId = it },
                            onSettleOnly = { settleOnlyId = it },
                            onDelete = { deleteTargetId = it }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    item(key = "matured_spacer_${group.bank}") {
                        Spacer(Modifier.height(8.dp))
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // 入账确认（生成现金流水）
    if (settleWithCashId > 0) {
        val name = findDepositName(state, settleWithCashId)
        AlertDialog(
            onDismissRequest = { settleWithCashId = -1 },
            title = { Text("确认入账") },
            text = { Text("将「${name}」本金+利息入账到现金？\n\n系统将自动生成现金流水，并标记为已入账。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.settleWithCash(settleWithCashId) { settleWithCashId = -1 }
                }) {
                    Text("确认入账")
                }
            },
            dismissButton = {
                TextButton(onClick = { settleWithCashId = -1 }) { Text("取消") }
            }
        )
    }

    // 仅标记（不生成流水）
    if (settleOnlyId > 0) {
        val name = findDepositName(state, settleOnlyId)
        AlertDialog(
            onDismissRequest = { settleOnlyId = -1 },
            title = { Text("仅标记已入账") },
            text = { Text("将「${name}」标记为已入账？\n\n不会生成现金流水，仅更改存单状态。\n适合已手动处理过资金的情况。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markSettledOnly(settleOnlyId) { settleOnlyId = -1 }
                }) {
                    Text("仅标记")
                }
            },
            dismissButton = {
                TextButton(onClick = { settleOnlyId = -1 }) { Text("取消") }
            }
        )
    }

    // 删除确认
    if (deleteTargetId > 0) {
        val name = findDepositName(state, deleteTargetId)
        AlertDialog(
            onDismissRequest = { deleteTargetId = -1 },
            title = { Text("删除存单") },
            text = { Text("确定要删除「${name}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDeposit(deleteTargetId) { deleteTargetId = -1 }
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = -1 }) { Text("取消") }
            }
        )
    }
}

private fun findDepositName(state: MaturedDepositsUiState, id: Long): String {
    for (group in state.bankGroups) {
        group.deposits.find { it.id == id }?.let { return it.name }
    }
    return ""
}

@Composable
private fun BankGroupHeader(group: MaturedBankGroup) {
    val bankColor = FinancialColors.bankColor(group.bank)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(bankColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                FinancialColors.bankInitial(group.bank),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(group.bank, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Text(
            "${group.deposits.size} 笔 · 本金 ${group.totalPrincipal} · 本息 ${group.totalAmount}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MaturedDepositCard(
    deposit: DepositDisplay,
    index: Int,
    totalCount: Int,
    onSettleWithCash: (Long) -> Unit,
    onSettleOnly: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    val progressPct = (deposit.progress * 100).roundToInt()
    val barColor = Color(0xFFB0B0B0)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(colors = listOf(barColor, barColor.copy(alpha = 0.3f))),
                        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
            )
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "NO.${index + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = barColor,
                            modifier = Modifier
                                .background(barColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            deposit.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        "${deposit.principal} · ${deposit.rate}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("到期本息", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            deposit.currentValue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("累计利息", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            deposit.accruedInterest,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FinancialColors.up
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "${deposit.startDate} → ${deposit.maturityDate}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${deposit.holdingDays}/${deposit.totalDays} 天 · ${progressPct}%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { deposit.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Spacer(Modifier.height(8.dp))

                // 按钮区：已入账只显示删除，未入账显示入账/仅标记
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (deposit.status == "settled") {
                        TextButton(onClick = { onDelete(deposit.id) }) {
                            Text("删除", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        TextButton(onClick = { onSettleWithCash(deposit.id) }) {
                            Text("入账", fontSize = 12.sp, color = FinancialColors.up)
                        }
                        Spacer(Modifier.width(4.dp))
                        TextButton(onClick = { onSettleOnly(deposit.id) }) {
                            Text("仅标记", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

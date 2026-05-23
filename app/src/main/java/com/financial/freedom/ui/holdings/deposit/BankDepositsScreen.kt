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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.ui.holdings.DepositDisplay
import com.financial.freedom.ui.theme.FinancialColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankDepositsScreen(
    bankName: String,
    status: String,
    onBack: () -> Unit,
    onEditDeposit: (Long) -> Unit,
    onAddDeposit: () -> Unit,
    viewModel: BankDepositsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var deleteTargetId by remember { mutableStateOf(-1L) }

    LaunchedEffect(bankName, status) {
        viewModel.load(bankName, status)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(bankName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${state.depositCount} 笔存单",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onAddDeposit) {
                        Icon(Icons.Filled.Add, contentDescription = "添加存单")
                    }
                }
            )
        }
    ) { padding ->
        if (state.deposits.isEmpty() && state.bankName.isNotEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "暂无存单",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // 汇总卡片
                item {
                    BankSummaryCard(state)
                    Spacer(Modifier.height(16.dp))
                }

                // 存单列表
                items(state.deposits, key = { "deposit_${it.id}" }) { deposit ->
                    DepositCard(
                        deposit = deposit,
                        isMatured = status == "matured",
                        onEdit = { onEditDeposit(deposit.id) },
                        onDelete = { id -> deleteTargetId = id }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (deleteTargetId > 0) {
        val depositName = state.deposits.find { it.id == deleteTargetId }?.name ?: ""
        AlertDialog(
            onDismissRequest = { deleteTargetId = -1 },
            title = { Text("删除存单") },
            text = { Text("确定要删除「${depositName}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDeposit(deleteTargetId) { deleteTargetId = -1 }
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = -1 }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun BankSummaryCard(state: BankDepositsUiState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${state.bankName} · ${state.depositCount} 笔存单",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (state.status == "matured") {
                    Text(
                        "已到期 ✓",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("本金合计", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.totalPrincipal, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("累计利息", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.totalInterest, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = FinancialColors.deposit)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("当前估值", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.totalCurrentValue, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("今日利息", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        state.todayTotalInterest,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FinancialColors.up
                    )
                }
            }
        }
    }
}

@Composable
private fun DepositCard(
    deposit: DepositDisplay,
    isMatured: Boolean,
    onEdit: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val cardAlpha = if (isMatured) 0.6f else 1f

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            colors = listOf(
                                FinancialColors.deposit.copy(alpha = cardAlpha),
                                FinancialColors.deposit.copy(alpha = 0.3f * cardAlpha)
                            )
                        ),
                        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
            )
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp).weight(1f)) {
                // 行 1：存单名 | 本金 · 利率
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        deposit.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha)
                    )
                    Text(
                        "${deposit.principal} · ${deposit.rate}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(6.dp))

                // 行 2：当前估值 | 今日利息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("当前估值", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            deposit.currentValue,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("今日利息", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            deposit.todayInterest,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = FinancialColors.up
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 行 3：日期 + 进度条
                Text(
                    "${deposit.startDate} → ${deposit.maturityDate}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${deposit.holdingDays}/${deposit.totalDays} 天",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "累计利息 ${deposit.accruedInterest}",
                        fontSize = 11.sp,
                        color = FinancialColors.deposit.copy(alpha = cardAlpha),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { deposit.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = FinancialColors.deposit,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Spacer(Modifier.height(6.dp))

                // 编辑/删除 按钮
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("编辑", fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(2.dp))
                    TextButton(onClick = { onDelete(deposit.id) }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("删除", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

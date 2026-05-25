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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.ui.holdings.DepositDisplay
import com.financial.freedom.ui.theme.FinancialColors
import kotlin.math.roundToInt

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

    val bankColor = FinancialColors.bankColor(bankName)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bankColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                FinancialColors.bankInitial(bankName),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(bankName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${state.depositCount} 笔存单",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                Text("暂无存单", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    BankSummaryCard(state, bankColor)
                    Spacer(Modifier.height(16.dp))
                }

                itemsIndexed(state.deposits, key = { _, d -> "deposit_${d.id}" }) { index, deposit ->
                    DepositCard(
                        deposit = deposit,
                        index = index,
                        totalCount = state.deposits.size,
                        bankColor = bankColor,
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
private fun BankSummaryCard(state: BankDepositsUiState, bankColor: Color) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bankColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            FinancialColors.bankInitial(state.bankName),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${state.bankName} · ${state.depositCount} 笔存单",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
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
                        color = bankColor)
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

private fun progressBarColor(progress: Float, alpha: Float = 1f, bankColor: Color): Color {
    val lightnessFactor = 1f + (1f - progress) * 0.35f
    return bankColor.copy(
        red = (bankColor.red * lightnessFactor).coerceIn(0f, 1f),
        green = (bankColor.green * lightnessFactor).coerceIn(0f, 1f),
        blue = (bankColor.blue * lightnessFactor).coerceIn(0f, 1f),
        alpha = alpha
    )
}

@Composable
private fun DepositCard(
    deposit: DepositDisplay,
    index: Int,
    totalCount: Int,
    bankColor: Color,
    onEdit: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val barColor = progressBarColor(deposit.progress, 1f, bankColor)
    val progressPct = (deposit.progress * 100).roundToInt()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Max)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(barColor, barColor.copy(alpha = 0.2f))
                        ),
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
                            color = MaterialTheme.colorScheme.onSurface
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("当前估值", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            deposit.currentValue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("今日利息", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            deposit.todayInterest,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (deposit.isInterestUp) FinancialColors.up else FinancialColors.down
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
                    Text(
                        "累计利息 ${deposit.accruedInterest}",
                        fontSize = 11.sp,
                        color = barColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { deposit.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
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

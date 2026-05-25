package com.financial.freedom.ui.holdings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.data.local.entity.PriceSnapshot
import com.financial.freedom.data.local.entity.Transaction
import com.financial.freedom.ui.theme.FinancialColors
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldDetailScreen(
    holdingId: Long,
    onBack: () -> Unit,
    viewModel: GoldViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Delete all confirmation
    if (state.showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteAllDialog() },
            title = { Text("确认删除", fontWeight = FontWeight.SemiBold) },
            text = { Text("将删除所有黄金持仓记录、交易记录和价格数据，此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAllHoldings { onBack() } }) {
                    Text("删除全部", color = FinancialColors.up)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteAllDialog() }) { Text("取消") }
            }
        )
    }

    // Add dialog
    if (state.showAddDialog) {
        GoldPurchaseDialog(
            editing = state.editingTransaction,
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { date, price, grams, deduct ->
                if (state.editingTransaction != null) {
                    viewModel.updatePurchase(state.editingTransaction!!, date, price, grams)
                } else {
                    viewModel.addPurchase(date, price, grams, deduct)
                }
            }
        )
    }

    // Reduce dialog
    if (state.showReduceDialog) {
        GoldReduceDialog(
            onDismiss = { viewModel.hideReduceDialog() },
            onConfirm = { date, price, grams, addToCash ->
                viewModel.reducePosition(date, price, grams, addToCash)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("黄金详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (state.holding == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("暂无黄金持仓", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // ── Hero: current price + market value ──
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("当前价格", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "¥${state.currentPrice}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${state.todayChange} (${state.todayChangePct})",
                                    fontSize = 13.sp,
                                    color = if (state.isTodayUp) FinancialColors.up else FinancialColors.down
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("持仓市值", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    state.marketValue,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // ── Chart ──
            if (state.priceHistory.size >= 2 || state.transactions.isNotEmpty()) {
                item { Spacer(Modifier.height(12.dp)) }
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
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
                                    "价格走势",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    ChartTimeRange.entries.forEach { range ->
                                        val isSelected = state.chartTimeRange == range
                                        Text(
                                            text = range.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { viewModel.setChartTimeRange(range) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "点击圆点查看交易详情",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            GoldDetailChart(
                                priceHistory = state.priceHistory,
                                transactions = state.transactions,
                                modifier = Modifier.fillMaxWidth().height(220.dp)
                            )
                        }
                    }
                }
            }

            // ── P&L Stats ──
            item { Spacer(Modifier.height(12.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("持仓盈亏", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                state.totalPnL,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isUp) FinancialColors.up else FinancialColors.down
                            )
                            Text(
                                state.totalPnLPct,
                                fontSize = 12.sp,
                                color = if (state.isUp) FinancialColors.up else FinancialColors.down
                            )
                        }
                    }
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("今日盈亏", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                state.todayChange,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isTodayUp) FinancialColors.up else FinancialColors.down
                            )
                            Text(
                                state.todayChangePct,
                                fontSize = 12.sp,
                                color = if (state.isTodayUp) FinancialColors.up else FinancialColors.down
                            )
                        }
                    }
                }
            }

            // ── Cost Info ──
            item { Spacer(Modifier.height(12.dp)) }
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("持仓信息", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(12.dp))
                        InfoRow("持有克数", "${state.totalGrams} 克")
                        Spacer(Modifier.height(8.dp))
                        InfoRow("成本均价", "¥${state.avgCostPrice}")
                        Spacer(Modifier.height(8.dp))
                        InfoRow("总成本", "¥${state.totalCost}")
                        Spacer(Modifier.height(8.dp))
                        InfoRow("当前市价", "¥${state.currentPrice}")
                    }
                }
            }

            // ── Action Buttons ──
            item { Spacer(Modifier.height(12.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewModel.showAddDialog() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = FinancialColors.gold.copy(alpha = 0.1f)),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = FinancialColors.gold, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("买入", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = FinancialColors.gold)
                        }
                    }
                    ElevatedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewModel.showReduceDialog() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = FinancialColors.up.copy(alpha = 0.1f)),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = null, tint = FinancialColors.up, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("减仓", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = FinancialColors.up)
                        }
                    }
                }
            }

            // ── Delete All ──
            item { Spacer(Modifier.height(8.dp)) }
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.showDeleteAllDialog() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = FinancialColors.up.copy(alpha = 0.05f)),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = FinancialColors.up, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("删除全部持仓", fontSize = 14.sp, color = FinancialColors.up)
                    }
                }
            }

            // ── Transaction List ──
            if (state.transactions.isNotEmpty()) {
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    Text(
                        "交易记录 (${state.transactions.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }

                val sortedTxs = state.transactions.sortedByDescending { it.date }
                items(sortedTxs.size) { index ->
                    val tx = sortedTxs[index]
                    GoldTradeRecord(
                        transaction = tx,
                        index = index,
                        totalRecords = sortedTxs.size,
                        onEdit = { viewModel.startEdit(tx) },
                        onDelete = { viewModel.deletePurchase(tx) }
                    )
                    if (index < sortedTxs.size - 1) {
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ===== Info Row =====
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ===== Gold Detail Chart (with buy + sell markers, Y-axis labels, grid lines, X-axis, tap-to-select) =====
@Composable
private fun GoldDetailChart(
    priceHistory: List<PriceSnapshot>,
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val goldColor = FinancialColors.gold
    val sellColor = FinancialColors.up

    data class TradeMarker(
        val index: Int,
        val tx: Transaction,
        val priceAtDate: Float
    )

    val sortedPrices = remember(priceHistory) { priceHistory.sortedBy { it.date } }
    val tradeMarkers = remember(sortedPrices, transactions) {
        if (sortedPrices.isEmpty()) return@remember emptyList<TradeMarker>()
        transactions.mapNotNull { tx ->
            val idx = sortedPrices.indexOfFirst { it.date >= tx.date }
            if (idx >= 0) TradeMarker(index = idx, tx = tx, priceAtDate = sortedPrices[idx].unitPrice.toFloat())
            else null
        }
    }

    var selectedPointIndex by remember { mutableStateOf(-1) }
    val selectedSnapshot = sortedPrices.getOrNull(selectedPointIndex)
    val selectedTradeMarker = tradeMarkers.firstOrNull { it.index == selectedPointIndex }

    // Computed values
    val values = remember(sortedPrices) { sortedPrices.map { it.unitPrice.toFloat() } }
    val minVal = remember(values) { values.min() }
    val maxVal = remember(values) { values.max() }
    val range = (maxVal - minVal).coerceAtLeast(0.01f)

    val lineColor = if (values.size >= 2) {
        if (values.last() >= values.first()) FinancialColors.up else FinancialColors.down
    } else goldColor

    // Y-axis labels (4 evenly spaced)
    val yLabels = remember(minVal, maxVal, range) {
        (0..3).map { i ->
            val price = maxVal - range * i / 3f
            formatGoldPrice(price)
        }
    }

    // X-axis date labels (smart sampling)
    val xLabels = remember(sortedPrices) {
        val pts = sortedPrices
        if (pts.isEmpty()) return@remember emptyList<Pair<Int, String>>()
        if (pts.size <= 7) {
            pts.mapIndexed { i, p -> i to "${p.date.monthNumber}/${p.date.dayOfMonth}" }
        } else {
            val step = (pts.size - 1) / 5.0
            (0..5).map { i ->
                val idx = (i * step).toInt().coerceAtMost(pts.size - 1)
                idx to "${pts[idx].date.monthNumber}/${pts[idx].date.dayOfMonth}"
            }
        }
    }

    Column(modifier = modifier) {
        // Chart area: Y-axis labels + Canvas
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Y-axis price labels
            Column(
                modifier = Modifier.width(52.dp).height(200.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                yLabels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(sortedPrices) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat()
                            if (sortedPrices.size < 2 || w <= 0f) return@detectTapGestures
                            val stepX = w / (sortedPrices.size - 1)
                            // Find nearest data point by X distance
                            var nearestIdx = 0
                            var nearestDist = Float.MAX_VALUE
                            for (i in sortedPrices.indices) {
                                val px = i * stepX
                                val dist = abs(px - tapOffset.x)
                                if (dist < nearestDist) {
                                    nearestDist = dist
                                    nearestIdx = i
                                }
                            }
                            // Accept tap if within reasonable distance (half the step)
                            val tapRadius = (stepX * 0.6f).coerceAtLeast(20f)
                            selectedPointIndex = if (nearestDist < tapRadius) nearestIdx else -1
                        }
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val paddingTop = 4.dp.toPx()
                    val paddingBottom = 4.dp.toPx()
                    val chartH = h - paddingTop - paddingBottom

                    if (values.size < 2) return@Canvas

                    val stepX = w / (values.size - 1).coerceAtLeast(1)

                    // Grid lines (horizontal, aligned with Y labels)
                    val gridColor = Color.Gray.copy(alpha = 0.12f)
                    for (i in 0..3) {
                        val y = paddingTop + chartH * i / 3f
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    }

                    // Selected point — vertical indicator line
                    if (selectedPointIndex in values.indices) {
                        val sx = selectedPointIndex * stepX
                        val indicatorColor = lineColor.copy(alpha = 0.3f)
                        drawLine(indicatorColor, Offset(sx, paddingTop), Offset(sx, h - paddingBottom), strokeWidth = 2.dp.toPx())
                    }

                    // Fill area
                    val fillPath = Path().apply {
                        moveTo(0f, h - paddingBottom)
                        values.forEachIndexed { i, v ->
                            val x = i * stepX
                            val y = paddingTop + chartH - ((v - minVal) / range * chartH)
                            lineTo(x, y)
                        }
                        lineTo((values.size - 1) * stepX, h - paddingBottom)
                        close()
                    }
                    drawPath(fillPath, lineColor.copy(alpha = 0.08f))

                    // Price line
                    val linePath = Path()
                    values.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = paddingTop + chartH - ((v - minVal) / range * chartH)
                        if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                    }
                    drawPath(linePath, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                    // Selected point — highlighted dot
                    if (selectedPointIndex in values.indices) {
                        val sx = selectedPointIndex * stepX
                        val sy = paddingTop + chartH - ((values[selectedPointIndex] - minVal) / range * chartH)
                        drawCircle(color = Color.White, radius = 8.dp.toPx(), center = Offset(sx, sy))
                        drawCircle(color = lineColor, radius = 5.dp.toPx(), center = Offset(sx, sy))
                    }

                    // Trade markers (smaller dots, not interfering with selection dot)
                    val markerRadius = 5.dp.toPx()
                    tradeMarkers.forEach { marker ->
                        val x = marker.index * stepX
                        val priceVal = values.getOrElse(marker.index) { values.last() }
                        val y = paddingTop + chartH - ((priceVal - minVal) / range * chartH)

                        val isBuy = marker.tx.type == "BUY"
                        val markerColor = if (isBuy) goldColor else sellColor
                        // Dim if a different point is selected
                        val alpha = if (selectedPointIndex == -1 || selectedPointIndex == marker.index) 0.8f else 0.3f

                        drawCircle(color = Color.White.copy(alpha = alpha), radius = markerRadius + 1.dp.toPx(), center = Offset(x, y))
                        drawCircle(color = markerColor.copy(alpha = alpha), radius = markerRadius, center = Offset(x, y))
                    }
                }
            }
        }

        // X-axis date labels
        if (xLabels.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 52.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                xLabels.forEach { (_, label) ->
                    Text(
                        text = label,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }

        // Tooltip for selected price point
        if (selectedSnapshot != null) {
            Spacer(Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ElevatedCard(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = FinancialColors.goldLight.copy(alpha = 0.95f)),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            selectedSnapshot.date.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "¥${"%.2f".format(selectedSnapshot.unitPrice)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinancialColors.gold
                        )
                        if (selectedTradeMarker != null) {
                            Spacer(Modifier.width(8.dp))
                            val actionLabel = if (selectedTradeMarker.tx.type == "BUY") "买入" else "卖出"
                            Text(
                                "$actionLabel ${selectedTradeMarker.tx.quantity} 克",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Format gold price: whole number for ≥ 100, 2 decimals for smaller */
internal fun formatGoldPrice(price: Float): String {
    return if (price >= 100f) "¥${price.toInt()}" else "¥${"%.2f".format(price)}"
}

// ===== Single Trade Record (buy or sell) =====
@Composable
private fun GoldTradeRecord(
    transaction: Transaction,
    index: Int,
    totalRecords: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val totalCost = transaction.quantity.multiply(transaction.price).setScale(2, RoundingMode.HALF_UP)
    val isBuy = transaction.type == "BUY"
    val typeColor = if (isBuy) FinancialColors.gold else FinancialColors.up
    val typeLabel = if (isBuy) "买入" else "卖出"

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(typeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "#${totalRecords - index}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        transaction.date.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(typeColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(typeLabel, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = typeColor)
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "更多",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("编辑")
                                }
                            },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = FinancialColors.up)
                                    Spacer(Modifier.width(8.dp))
                                    Text("删除", color = FinancialColors.up)
                                }
                            },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "单价 ¥${transaction.price} / 克",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "数量 ${transaction.quantity} 克",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Spacer(Modifier.height(4.dp))

            Text(
                "合计 ¥${com.financial.freedom.ui.common.FormatUtils.formatMoney(totalCost)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ===== Reduce Position Dialog =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoldReduceDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, BigDecimal, BigDecimal, Boolean) -> Unit
) {
    var date by remember { mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()) }
    var unitPrice by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("") }
    var addToCash by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("减仓黄金", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                if (error != null) {
                    Text(error!!, color = FinancialColors.up, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("卖出日期") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    enabled = false,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = grams,
                    onValueChange = { grams = it },
                    label = { Text("卖出克数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("卖出单价 (元/克)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                val g = grams.toBigDecimalOrNull()
                val p = unitPrice.toBigDecimalOrNull()
                if (g != null && p != null && g > BigDecimal.ZERO && p > BigDecimal.ZERO) {
                    val total = g.multiply(p).setScale(2, RoundingMode.HALF_UP)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "卖出总额：¥${com.financial.freedom.ui.common.FormatUtils.formatMoney(total)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = FinancialColors.up
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { addToCash = !addToCash }
                ) {
                    Checkbox(checked = addToCash, onCheckedChange = { addToCash = it })
                    Text("加回现金账户", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val gVal = grams.toBigDecimalOrNull()
                val pVal = unitPrice.toBigDecimalOrNull()
                val dVal = runCatching { LocalDate.parse(date) }.getOrNull()
                when {
                    dVal == null -> error = "请输入有效日期"
                    gVal == null || gVal <= BigDecimal.ZERO -> error = "请输入有效克数"
                    pVal == null || pVal <= BigDecimal.ZERO -> error = "请输入有效单价"
                    else -> {
                        error = null
                        onConfirm(dVal, pVal, gVal, addToCash)
                    }
                }
            }) {
                Text("确认减仓")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

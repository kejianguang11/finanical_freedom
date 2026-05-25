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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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

// ===== Gold Page — v23 aggregated gold card =====
@Composable
fun GoldPage(
    onHoldingClick: (Long) -> Unit = {},
    onAddHolding: () -> Unit = {},
    viewModel: GoldViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.holding == null) {
        // Empty state
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "暂无黄金持仓",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "点击右下角 + 添加第一笔买入记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                containerColor = FinancialColors.gold
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加买入", tint = Color.White)
            }
        }
    } else {
        GoldContent(state = state, viewModel = viewModel, onHoldingClick = onHoldingClick)
    }

    // Add / Edit dialog
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
}

@Composable
private fun GoldContent(
    state: GoldUiState,
    viewModel: GoldViewModel,
    onHoldingClick: (Long) -> Unit = {}
) {
    val holding = state.holding ?: return

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                GoldAggregatedCard(
                    state = state,
                    onCardClick = { onHoldingClick(holding.id) }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        FloatingActionButton(
            onClick = { viewModel.showAddDialog() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            containerColor = FinancialColors.gold
        ) {
            Icon(Icons.Filled.Add, contentDescription = "买入黄金", tint = Color.White)
        }
    }
}

// ===== Compact Gold Card (v24 — click to detail) =====
@Composable
private fun GoldAggregatedCard(
    state: GoldUiState,
    onCardClick: () -> Unit
) {
    val holding = state.holding ?: return

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // ── Row 1: Au badge + name/grams | market value ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FinancialColors.gold),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Au",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "黄金",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "XAU · ${state.totalGrams} 克",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        state.marketValue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "市值",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Row 2: today change | P&L ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("今日", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${state.todayChange} (${state.todayChangePct})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.isTodayUp) FinancialColors.up else FinancialColors.down
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("持仓盈亏", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${state.totalPnL} (${state.totalPnLPct})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.isUp) FinancialColors.up else FinancialColors.down
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Row 3: avg cost | total cost ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("持有均价", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        state.avgCostPrice,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("持有总成本", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        state.totalCost,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Row 4: mini sparkline + entry hint ──
            if (state.priceHistory.size >= 2) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniGoldSparkline(
                        priceHistory = state.priceHistory,
                        modifier = Modifier.weight(1f).height(40.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "点击查看详情 >",
                        fontSize = 12.sp,
                        color = FinancialColors.gold,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ===== Mini Gold Sparkline (compact, no interaction) =====
@Composable
private fun MiniGoldSparkline(
    priceHistory: List<PriceSnapshot>,
    modifier: Modifier = Modifier
) {
    val lineColor = if (priceHistory.size >= 2) {
        if (priceHistory.last().unitPrice >= priceHistory.first().unitPrice) FinancialColors.up
        else FinancialColors.down
    } else FinancialColors.gold

    Canvas(modifier = modifier) {
        if (priceHistory.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val values = priceHistory.map { it.unitPrice.toFloat() }
        val minVal = values.min()
        val maxVal = values.max()
        val range = (maxVal - minVal).coerceAtLeast(0.01f)
        val stepX = w / (values.size - 1).coerceAtLeast(1)

        // Fill area
        val fillPath = Path().apply {
            moveTo(0f, h)
            values.forEachIndexed { i, v ->
                val x = i * stepX
                val y = h - ((v - minVal) / range * h)
                lineTo(x, y)
            }
            lineTo((values.size - 1) * stepX, h)
            close()
        }
        drawPath(fillPath, lineColor.copy(alpha = 0.1f))

        // Line
        val linePath = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = h - ((v - minVal) / range * h)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        drawPath(linePath, lineColor, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
    }
}


// ===== Gold Chart with Buy Markers (Y-axis labels, grid lines, X-axis) =====
@Composable
private fun GoldChartWithBuyMarkers(
    priceHistory: List<PriceSnapshot>,
    transactions: List<Transaction>,
    selectedBuyIndex: Int,
    onBuyMarkerTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val goldColor = FinancialColors.gold

    // Computed values
    val values = remember(priceHistory) { priceHistory.map { it.unitPrice.toFloat() } }
    val minVal = remember(values) { values.min() }
    val maxVal = remember(values) { values.max() }
    val range = (maxVal - minVal).coerceAtLeast(0.01f)

    val lineColor = if (values.size >= 2) {
        if (values.last() >= values.first()) FinancialColors.up else FinancialColors.down
    } else goldColor

    // Buy markers: map each transaction to nearest price data point
    data class BuyMarker(val index: Int, val tx: Transaction, val x: Float, val priceAtDate: Float)

    val buyMarkers = remember(priceHistory, transactions) {
        if (priceHistory.isEmpty()) return@remember emptyList<BuyMarker>()
        val sorted = priceHistory.sortedBy { it.date }
        transactions.mapNotNull { tx ->
            val idx = sorted.indexOfFirst { it.date >= tx.date }
            if (idx >= 0) BuyMarker(index = idx, tx = tx, x = 0f, priceAtDate = sorted[idx].unitPrice.toFloat())
            else null
        }
    }

    val selectedMarker = buyMarkers.getOrNull(selectedBuyIndex)

    // Y-axis labels (4 evenly spaced)
    val yLabels = remember(minVal, maxVal, range) {
        (0..3).map { i ->
            val price = maxVal - range * i / 3f
            formatGoldPrice(price)
        }
    }

    // X-axis date labels (smart sampling)
    val sortedPrices = remember(priceHistory) { priceHistory.sortedBy { it.date } }
    val xLabels = remember(sortedPrices) {
        if (sortedPrices.isEmpty()) return@remember emptyList<Pair<Int, String>>()
        if (sortedPrices.size <= 7) {
            sortedPrices.mapIndexed { i, p -> i to "${p.date.monthNumber}/${p.date.dayOfMonth}" }
        } else {
            val step = (sortedPrices.size - 1) / 5.0
            (0..5).map { i ->
                val idx = (i * step).toInt().coerceAtMost(sortedPrices.size - 1)
                idx to "${sortedPrices[idx].date.monthNumber}/${sortedPrices[idx].date.dayOfMonth}"
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
                    Text(text = label, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }

            // Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(buyMarkers) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat()
                            val hitRadius = 30f
                            val found = buyMarkers.indexOfFirst {
                                val markerX = (it.index.toFloat() / (priceHistory.size - 1).coerceAtLeast(1)) * w
                                abs(markerX - tapOffset.x) < hitRadius
                            }
                            if (found >= 0) onBuyMarkerTap(found) else onBuyMarkerTap(-1)
                        }
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val paddingTop = 4.dp.toPx()
                    val paddingBottom = 4.dp.toPx()
                    val chartH = h - paddingTop - paddingBottom

                    if (priceHistory.size < 2) return@Canvas

                    val stepX = w / (priceHistory.size - 1).coerceAtLeast(1)

                    // Grid lines (horizontal, aligned with Y labels)
                    val gridColor = Color.Gray.copy(alpha = 0.12f)
                    for (i in 0..3) {
                        val y = paddingTop + chartH * i / 3f
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
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

                    // Buy markers
                    val markerRadius = 6.dp.toPx()
                    val markerOuterRadius = 10.dp.toPx()
                    buyMarkers.forEachIndexed { markerIdx, marker ->
                        val x = marker.index * stepX
                        val priceVal = values.getOrElse(marker.index) { values.last() }
                        val y = paddingTop + chartH - ((priceVal - minVal) / range * chartH)

                        val isSelected = markerIdx == selectedBuyIndex
                        val outerR = markerOuterRadius + if (isSelected) 2.dp.toPx() else 0f

                        drawCircle(color = goldColor.copy(alpha = if (isSelected) 1f else 0.6f), radius = outerR, center = Offset(x, y))
                        drawCircle(color = Color.White, radius = markerRadius * if (isSelected) 1.2f else 0.8f, center = Offset(x, y))
                        drawCircle(color = goldColor, radius = 3.dp.toPx(), center = Offset(x, y))
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
                    Text(text = label, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }

        // Tooltip below chart
        if (selectedMarker != null) {
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
                        Text(selectedMarker.tx.date.toString(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(8.dp))
                        Text("¥${selectedMarker.tx.price}", fontSize = 12.sp, color = FinancialColors.gold)
                        Spacer(Modifier.width(8.dp))
                        Text("买入 ${selectedMarker.tx.quantity} 克", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// ===== Single Purchase Record =====
@Composable
private fun GoldPurchaseRecord(
    transaction: Transaction,
    index: Int,
    totalRecords: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val totalCost = transaction.quantity.multiply(transaction.price).setScale(2, RoundingMode.HALF_UP)

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
                    // 序号标记
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(FinancialColors.gold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "#${totalRecords - index}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FinancialColors.gold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        transaction.date.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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

// ===== Add / Edit Purchase Dialog =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoldPurchaseDialog(
    editing: Transaction?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, BigDecimal, BigDecimal, Boolean) -> Unit
) {
    val isEdit = editing != null
    var date by remember(editing) { mutableStateOf(editing?.date?.toString() ?: "") }
    var unitPrice by remember(editing) { mutableStateOf(editing?.price?.toPlainString() ?: "") }
    var grams by remember(editing) { mutableStateOf(editing?.quantity?.toPlainString() ?: "") }
    var deductFromCash by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Default date to today if empty
    LaunchedEffect(Unit) {
        if (date.isEmpty()) {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            date = today.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) "编辑买入记录" else "买入黄金",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                if (error != null) {
                    Text(
                        error!!,
                        color = FinancialColors.up,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // 日期选择
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("买入日期") },
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

                // 克数
                OutlinedTextField(
                    value = grams,
                    onValueChange = { grams = it },
                    label = { Text("克数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))

                // 单价
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("单价 (元/克)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 预估总价
                val g = grams.toBigDecimalOrNull()
                val p = unitPrice.toBigDecimalOrNull()
                if (g != null && p != null && g > BigDecimal.ZERO && p > BigDecimal.ZERO) {
                    val total = g.multiply(p).setScale(2, RoundingMode.HALF_UP)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "买入总价：¥${com.financial.freedom.ui.common.FormatUtils.formatMoney(total)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = FinancialColors.gold
                    )
                }

                // 从现金扣除（仅新增时可用）
                if (!isEdit) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { deductFromCash = !deductFromCash }
                    ) {
                        Checkbox(
                            checked = deductFromCash,
                            onCheckedChange = { deductFromCash = it }
                        )
                        Text(
                            "从现金账户扣除",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                        onConfirm(dVal, pVal, gVal, deductFromCash)
                    }
                }
            }) {
                Text(if (isEdit) "保存修改" else "确认买入")
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

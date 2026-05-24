package com.financial.freedom.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.EnergySavingsLeaf
import androidx.compose.material3.Icon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.data.local.entity.DailySummary
import com.financial.freedom.ui.components.TooltipType
import com.financial.freedom.ui.components.TrendChart
import com.financial.freedom.ui.theme.FinancialColors
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onScrollToPage: (Int) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    onAddDeposit: () -> Unit = {},
    onAddHolding: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var showPrecise by remember { mutableStateOf(false) }

    // 从新增/编辑页面返回时自动刷新
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeCount by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (resumeCount > 0) {
                    viewModel.refresh()
                }
                resumeCount++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.isEmpty) {
            EmptyHomeState(
                onAddDeposit = onAddDeposit,
                onAddHolding = onAddHolding
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))

                // ===== L0: 总资产 =====
                Text(
                    "总 资 产",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 8.sp
                )
                Spacer(Modifier.height(8.dp))

                MilestoneGlowText(
                    targetValue = state.netWorthRaw,
                    isCelebrating = state.crossedMilestone != null,
                    isUp = state.isUp,
                    onCelebrationComplete = { viewModel.dismissMilestone() }
                )

                // All-time high banner
                if (state.isAllTimeHigh) {
                    Spacer(Modifier.height(6.dp))
                    AllTimeHighBanner(
                        onDismiss = { viewModel.dismissAllTimeHigh() }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ===== L1: 今日收益 Pill 徽章 =====
                val gainBg = if (state.isUp) FinancialColors.upBg else FinancialColors.downBg
                val gainText = if (state.isUp) FinancialColors.up else FinancialColors.down

                // Gold pulse glow on positive earnings — subtle breathing border
                val goldPulseAlpha = if (state.isUp) {
                    val pulseTransition = rememberInfiniteTransition(label = "goldPulse")
                    val pulse by pulseTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "pulse"
                    )
                    (0.06f * sin(pulse * PI).toFloat()).coerceIn(0f, 0.08f)
                } else 0f

                Box(
                    modifier = Modifier
                        .border(
                            width = if (goldPulseAlpha > 0.001f) 1.5.dp else 0.dp,
                            color = FinancialColors.gold.copy(alpha = goldPulseAlpha),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(gainBg)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "今日 ${state.todayChange}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = gainText
                        )
                        Text(
                            text = state.todayChangePct,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = gainText.copy(alpha = 0.8f)
                        )
                    }
                }

                // Streak badge: consecutive positive days
                if (state.consecutiveUpDays >= 2) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = FinancialColors.gold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "连涨 ${state.consecutiveUpDays} 天",
                            fontSize = 11.sp,
                            color = FinancialColors.gold
                        )
                    }
                }

                // Month-over-month comparison
                if (state.monthOverMonthChange.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    val momIsUp = !state.monthOverMonthChange.startsWith("-")
                    Text(
                        "较上月 ${state.monthOverMonthChange} (${state.monthOverMonthPct})",
                        fontSize = 12.sp,
                        color = if (momIsUp) FinancialColors.up else FinancialColors.down
                    )
                }

                if (state.lastUpdateTime != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "更新于 ${state.lastUpdateTime}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ===== 资产配置占比条 =====
                val investmentValue = computeTotalValueRaw(state.stockValue, state.fundValue, state.goldValue)
                val depositValue = parseMoneyValue(state.depositValue)
                val cashValue = parseMoneyValue(state.cashBalance)
                val receivableValue = parseMoneyValue(state.receivablesTotal)
                val totalAllocation = listOfNotNull(investmentValue, depositValue, cashValue, receivableValue)
                    .fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }

                if (totalAllocation > BigDecimal.ZERO) {
                    AllocationBar(
                        items = listOfNotNull(
                            allocationItem("投资", investmentValue, totalAllocation, FinancialColors.gold),
                            allocationItem("存款", depositValue, totalAllocation, FinancialColors.deposit),
                            allocationItem("现金", cashValue, totalAllocation, FinancialColors.cash),
                            allocationItem("借出", receivableValue, totalAllocation, FinancialColors.receivable)
                        ).sortedByDescending { it.value }
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // ===== L3: 2×2 资产网格 =====
                val investmentChange = computeTotalChange(state.stockChange, state.fundChange, state.goldChange)
                val creditNet = computeCreditNet(state.receivablesTotal, state.debtsTotal)

                // Row 1: 投资 + 存款
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactAssetCard(
                        label = "投资",
                        totalValue = formatAllocationValue(investmentValue),
                        todayChange = investmentChange,
                        accentColor = FinancialColors.gold,
                        modifier = Modifier.weight(1f),
                        onClick = { onScrollToPage(1) }
                    )
                    CompactAssetCard(
                        label = "存款",
                        totalValue = formatAllocationValue(depositValue),
                        todayChange = state.depositChange,
                        accentColor = FinancialColors.deposit,
                        modifier = Modifier.weight(1f),
                        onClick = { onScrollToPage(4) }
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Row 2: 现金 + 信用
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactAssetCard(
                        label = "现金",
                        totalValue = formatAllocationValue(cashValue),
                        todayChange = null,
                        accentColor = FinancialColors.cash,
                        modifier = Modifier.weight(1f),
                        onClick = { onScrollToPage(5) }
                    )
                    CompactAssetCard(
                        label = "信用",
                        totalValue = creditNet,
                        todayChange = null,
                        accentColor = FinancialColors.receivable,
                        modifier = Modifier.weight(1f),
                        onClick = { onScrollToPage(6) }
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ===== L4: 资产走势图（放最下面） =====
                NetWorthTrendCard(
                    trendData = state.trendData,
                    selectedRange = state.selectedTrendRange,
                    onSelectRange = { viewModel.selectTrendRange(it) },
                    multiplier = state.displayMultiplier
                )
            }
        }
    }
}

// ===== Allocation Bar =====

private data class AllocItem(
    val label: String,
    val value: BigDecimal,
    val fraction: Float,
    val color: Color
)

private fun allocationItem(
    label: String, value: BigDecimal?, total: BigDecimal, color: Color
): AllocItem? {
    if (value == null || value <= BigDecimal.ZERO) return null
    val fraction = value.divide(total, 3, RoundingMode.HALF_UP).toFloat()
    return AllocItem(label, value, fraction, color)
}

@Composable
private fun AllocationBar(items: List<AllocItem>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "资产配置",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            // Stacked bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                items.forEach { item ->
                    val fraction = item.fraction.coerceIn(0f, 1f)
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(fraction)
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(item.color)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items.forEach { item ->
                    val pct = (item.fraction * 100).toInt()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(item.color)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${item.label} $pct%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// ===== Compact Asset Card (2×2 Grid) =====
@Composable
private fun CompactAssetCard(
    label: String,
    totalValue: String,
    todayChange: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Top color bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(accentColor)
            )
            // Content — min height ensures cash/credit cards match investment/deposit
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .heightIn(min = 48.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        totalValue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (todayChange != null) {
                    Spacer(Modifier.height(2.dp))
                    val changeVal = parseMoneyValue(todayChange) ?: BigDecimal.ZERO
                    val isZero = changeVal == BigDecimal.ZERO
                    val displayAmount = if (changeVal > BigDecimal.ZERO) "+${formatMoney(changeVal)}"
                        else formatMoney(changeVal)
                    Text(
                        if (isZero) "" else "今日 $displayAmount",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isZero -> MaterialTheme.colorScheme.onSurfaceVariant
                            changeVal > BigDecimal.ZERO -> FinancialColors.up
                            else -> FinancialColors.down
                        }
                    )
                }
            }
        }
    }
}

// ===== Helpers =====

// ===== Helpers =====

private fun computeTotalValueRaw(vararg values: String): BigDecimal? {
    val sum = values.mapNotNull { parseMoneyValue(it) }
        .fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
    return if (sum > BigDecimal.ZERO) sum else null
}

private fun computeTotalValue(vararg values: String): String {
    val sum = values.mapNotNull { parseMoneyValue(it) }
        .fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
    return formatMoneyShort(sum)
}

private fun computeTotalChange(vararg changes: String): String {
    val sum = changes.mapNotNull { parseMoneyValue(it) }
        .fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
    return formatMoney(sum)
}

private fun computeCreditNet(receivables: String, debts: String): String {
    val r = parseMoneyValue(receivables) ?: BigDecimal.ZERO
    val d = parseMoneyValue(debts) ?: BigDecimal.ZERO
    val net = r.subtract(d)
    return if (net >= BigDecimal.ZERO) formatMoneyShort(net) else formatMoneyShort(net)
}

private fun formatAllocationValue(value: BigDecimal?): String {
    return com.financial.freedom.ui.common.FormatUtils.formatAllocationValue(value)
}

private fun parseMoneyValue(s: String): BigDecimal? {
    return com.financial.freedom.ui.common.FormatUtils.parseMoneyValue(s)
}

private fun formatMoney(value: BigDecimal): String {
    return com.financial.freedom.ui.common.FormatUtils.formatMoney(value)
}

private fun formatMoneyShort(value: BigDecimal): String {
    return com.financial.freedom.ui.common.FormatUtils.formatMoneyShort(value)
}

private fun formatNetWorthShort(raw: String): String {
    val value = parseMoneyValue(raw) ?: return raw
    val abs = value.abs().setScale(0, RoundingMode.HALF_UP)
    val intPart = abs.toBigInteger().toString()
    val formattedInt = intPart.reversed().chunked(3).joinToString(",").reversed()
    return if (value < BigDecimal.ZERO) "-¥$formattedInt" else "¥$formattedInt"
}

// ===== Dopamine-hit Composables =====

@Composable
private fun MilestoneGlowText(
    targetValue: BigDecimal,
    isCelebrating: Boolean,
    isUp: Boolean,
    onCelebrationComplete: () -> Unit
) {
    val targetFloat = targetValue.toFloat()
    val animatedValue by animateFloatAsState(
        targetValue = targetFloat,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "netWorthCountUp"
    )

    // Spring settle scale effect when animation completes
    val animationFinished = abs(animatedValue - targetFloat) < 0.5f
    val settleScale by animateFloatAsState(
        targetValue = if (animationFinished) 1.0f else 1.02f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "settleScale"
    )

    // Count-up display: format the animated value as integer with commas
    val displayText = "¥" + String.format("%,d", animatedValue.toLong())

    // Gold shimmer on positive day change (not just milestone)
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    AutoDismiss(isActive = isCelebrating, onDismiss = onCelebrationComplete)

    val baseColor = if (isCelebrating) FinancialColors.gold else MaterialTheme.colorScheme.onSurface
    val glowAlpha = if (isCelebrating) {
        (0.15f + 0.25f * sin(shimmerProgress * 2 * PI).toFloat()).coerceIn(0f, 0.4f)
    } else if (isUp) {
        // Subtle positive-day shimmer: gentle pulse on first load
        (0.03f + 0.05f * sin(shimmerProgress * 1.5f * PI).toFloat()).coerceIn(0f, 0.08f)
    } else 0f

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .scale(settleScale)
            .background(
                brush = if (isCelebrating || (isUp && !animationFinished)) Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        FinancialColors.gold.copy(alpha = glowAlpha),
                        Color.Transparent
                    )
                ) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            fontSize = 36.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = baseColor
        )
    }
}

@Composable
private fun AllTimeHighBanner(onDismiss: () -> Unit) {
    AutoDismiss(isActive = true, onDismiss = onDismiss)
    Row(
        modifier = Modifier
            .background(
                FinancialColors.goldLight,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔥", fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            "历史最佳单日",
            fontWeight = FontWeight.SemiBold,
            color = FinancialColors.gold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun PulseNumber(
    isUp: Boolean,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = if (isUp) scale else 1f
            scaleY = if (isUp) scale else 1f
            transformOrigin = TransformOrigin.Center
        }
    ) {
        content()
    }
}

@Composable
private fun AutoDismiss(
    isActive: Boolean,
    onDismiss: () -> Unit,
    delayMs: Long = 5000L
) {
    if (isActive) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(delayMs)
            onDismiss()
        }
    }
}

// ===== Net Worth Trend Card =====

@Composable
private fun NetWorthTrendCard(
    trendData: List<DailySummary>,
    selectedRange: TrendRange,
    onSelectRange: (TrendRange) -> Unit,
    multiplier: BigDecimal = BigDecimal.ONE
) {
    if (trendData.isEmpty()) return

    var showTotalAssets by remember { mutableStateOf(false) }

    // 每日收益折线：用 dayChange 而非净值，避免买入资产时成本基数造成跳变
    val dailyEarningsData = remember(trendData) {
        trendData.map { it.copy(totalValueCNY = it.dayChange) }
    }

    // 总资产折线：用 netWorth（含现金+借出-负债），而非 totalValueCNY（仅投资+存款）
    val totalAssetsData = remember(trendData) {
        trendData.map { it.copy(totalValueCNY = it.netWorth) }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with title and time range chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "资产走势",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TrendRange.entries.forEach { range ->
                        val isSelected = range == selectedRange
                        val label = when (range) {
                            TrendRange.WEEK -> "7天"
                            TrendRange.MONTH -> "30天"
                            TrendRange.YEAR -> "1年"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) FinancialColors.gold.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .clickable { onSelectRange(range) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) FinancialColors.gold
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // View toggle: 收益 / 总资产
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (!showTotalAssets) MaterialTheme.colorScheme.surface
                                else Color.Transparent
                            )
                            .clickable { showTotalAssets = false }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "收益",
                            fontSize = 12.sp,
                            fontWeight = if (!showTotalAssets) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (!showTotalAssets) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (showTotalAssets) MaterialTheme.colorScheme.surface
                                else Color.Transparent
                            )
                            .clickable { showTotalAssets = true }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "总资产",
                            fontSize = 12.sp,
                            fontWeight = if (showTotalAssets) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (showTotalAssets) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            TrendChart(
                data = if (showTotalAssets) totalAssetsData else dailyEarningsData,
                modifier = Modifier.fillMaxWidth().height(220.dp),
                multiplier = multiplier,
                tooltipType = if (showTotalAssets) TooltipType.NET_WORTH else TooltipType.EARNINGS
            )
        }
    }
}

// ===== Empty Home State =====

@Composable
private fun EmptyHomeState(
    onAddDeposit: () -> Unit,
    onAddHolding: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 80.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        // Seed icon — representing growth
        Icon(
            imageVector = Icons.Outlined.EnergySavingsLeaf,
            contentDescription = null,
            tint = FinancialColors.gold,
            modifier = Modifier.size(72.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "从这里开始积累",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "添加你的第一笔资产",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(40.dp))

        // Action card: Add Deposit
        ElevatedCard(
            onClick = onAddDeposit,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FinancialColors.deposit.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinancialColors.deposit
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "添加存款",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "开始储蓄",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Action card: Add Holding
        ElevatedCard(
            onClick = { onAddHolding("STOCK") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FinancialColors.gold.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinancialColors.gold
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "添加持仓",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "开始投资",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "复利的魔力，从第一笔开始",
            fontSize = 12.sp,
            color = FinancialColors.gold.copy(alpha = 0.7f),
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}

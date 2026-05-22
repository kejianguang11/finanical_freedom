package com.financial.freedom.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.ui.components.TrendChart
import com.financial.freedom.ui.theme.FinancialColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToHoldings: (Int) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 32.dp)
        ) {
            // ===== 总资产 Hero =====
            Text(
                "总资产",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "¥ ${state.totalValueCNY}",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            // 金色分割线
            HorizontalDivider(
                modifier = Modifier.width(48.dp),
                thickness = 2.dp,
                color = FinancialColors.gold
            )

            Spacer(Modifier.height(8.dp))

            // 今日收益
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.todayChange,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isUp) FinancialColors.up else FinancialColors.down
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = state.todayChangePct,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.isUp) FinancialColors.up else FinancialColors.down
                )
            }

            // 更新时间
            if (state.lastUpdateTime != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "价格更新于 ${state.lastUpdateTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(24.dp))

            // ===== 资产走势图 =====
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "资产走势",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TrendRangeSelector(
                            selected = state.selectedTrendRange,
                            onSelect = { viewModel.selectTrendRange(it) }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    if (state.trendData.isEmpty()) {
                        Box(
                            Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "暂无走势数据",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "下拉刷新或添加资产",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        TrendChart(
                            data = state.trendData,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ===== 资产配置 =====
            Text(
                "资产配置",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            // 三列等高：存款 / 股票 / 基金
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AssetCard(
                    icon = Icons.Outlined.AccountBalance,
                    label = "存款",
                    value = state.depositValue,
                    change = state.depositChange,
                    accentColor = FinancialColors.deposit,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToHoldings(0) }
                )
                AssetCard(
                    icon = Icons.AutoMirrored.Outlined.TrendingUp,
                    label = "股票",
                    value = state.stockValue,
                    change = state.stockChange,
                    accentColor = FinancialColors.stock,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToHoldings(1) }
                )
                AssetCard(
                    icon = Icons.Outlined.WaterDrop,
                    label = "基金",
                    value = state.fundValue,
                    change = state.fundChange,
                    accentColor = FinancialColors.fund,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToHoldings(2) }
                )
            }

            Spacer(Modifier.height(10.dp))

            // 黄金独占一行
            AssetCard(
                icon = Icons.Outlined.Diamond,
                label = "黄金",
                value = state.goldValue,
                change = state.goldChange,
                accentColor = FinancialColors.gold,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNavigateToHoldings(3) }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TrendRangeSelector(
    selected: TrendRange,
    onSelect: (TrendRange) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TrendRange.entries.forEach { range ->
            val label = when (range) {
                TrendRange.WEEK -> "7天"
                TrendRange.MONTH -> "30天"
                TrendRange.YEAR -> "1年"
            }
            val isSelected = selected == range
            val textColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(range) }
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
                            .width(24.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(FinancialColors.gold)
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetCard(
    icon: ImageVector,
    label: String,
    value: String,
    change: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(18.dp),
                    tint = accentColor
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            val isUp = change.startsWith("+")
            Text(
                text = "$change 今日",
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isUp -> FinancialColors.up
                    change.startsWith("-") -> FinancialColors.down
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

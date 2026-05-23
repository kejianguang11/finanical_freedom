package com.financial.freedom.ui.holdings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPositionDialog(
    currentQty: String,
    currentAvgCost: String,
    holdingName: String,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal, BigDecimal, LocalDate, Boolean) -> Unit
) {
    var qtyText by rememberSaveable { mutableStateOf("") }
    var priceText by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf("") }
    var deductFromCash by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val dpState = rememberDatePickerState()

    val context = LocalContext.current
    val qty = qtyText.toBigDecimalOrNull()
    val price = priceText.toBigDecimalOrNull()

    val previewTotalCost = if (qty != null && price != null && qty > BigDecimal.ZERO) {
        qty.multiply(price).setScale(2, RoundingMode.HALF_UP)
    } else null

    val oldQty = currentQty.replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO
    val oldCost = currentAvgCost.replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO
    val previewNewQty = qty?.let { oldQty.add(it) }
    val previewNewCost = if (qty != null && price != null && qty > BigDecimal.ZERO && previewNewQty != null && previewNewQty > BigDecimal.ZERO) {
        oldQty.multiply(oldCost).add(qty.multiply(price)).divide(previewNewQty, 4, RoundingMode.HALF_UP)
    } else null
    val previewTotalCostAfter = if (previewNewCost != null && previewNewQty != null) {
        previewNewCost.multiply(previewNewQty).setScale(2, RoundingMode.HALF_UP)
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加仓 — $holdingName") },
        text = {
            Column {
                Text("当前：$currentQty 股 · 成本均价 ¥$currentAvgCost",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                OutlinedTextField(qtyText, { qtyText = it },
                    label = { Text("买入数量 (股)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(priceText, { priceText = it },
                    label = { Text("买入价格 (元)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    readOnly = true, label = { Text("买入日期") },
                    modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { showDatePicker = true }) { Text("选择日期") }
                if (previewNewQty != null && previewNewCost != null && previewTotalCostAfter != null) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("加仓后预览：", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text("持有 ${previewNewQty} 股 · 成本均价 ¥${previewNewCost.setScale(2, RoundingMode.HALF_UP)}",
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("总成本 ¥${previewTotalCostAfter}",
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("从现金中扣除", modifier = Modifier.weight(1f))
                    Switch(checked = deductFromCash, onCheckedChange = { deductFromCash = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val q = qtyText.toBigDecimalOrNull()
                val p = priceText.toBigDecimalOrNull()
                if (q == null || p == null || q <= BigDecimal.ZERO) {
                    Toast.makeText(context, "买入数量必须大于 0", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                val date = runCatching { LocalDate.parse(dateText) }.getOrElse {
                    Toast.makeText(context, "请选择买入日期", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                onConfirm(q, p, date, deductFromCash)
            }) { Text("确认加仓") }
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
                    dpState.selectedDateMillis?.let { millis ->
                        dateText = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = dpState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReducePositionDialog(
    currentQty: String,
    currentAvgCost: String,
    holdingName: String,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal, BigDecimal, LocalDate, Boolean) -> Unit
) {
    var qtyText by rememberSaveable { mutableStateOf("") }
    var priceText by rememberSaveable { mutableStateOf("") }
    var dateText by rememberSaveable { mutableStateOf("") }
    var addToCash by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showCloseConfirm by rememberSaveable { mutableStateOf(false) }
    val dpState = rememberDatePickerState()

    val context = LocalContext.current
    val qty = qtyText.toBigDecimalOrNull()
    val price = priceText.toBigDecimalOrNull()
    val oldQty = currentQty.replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO
    val oldCost = currentAvgCost.replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO

    val previewRemaining = qty?.let { oldQty.subtract(it) }
    val previewPnL = if (qty != null && price != null && qty > BigDecimal.ZERO) {
        price.subtract(oldCost).multiply(qty).setScale(2, RoundingMode.HALF_UP)
    } else null
    val previewPnLPct = if (qty != null && price != null && oldCost > BigDecimal.ZERO && qty > BigDecimal.ZERO) {
        price.subtract(oldCost).divide(oldCost, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100)).setScale(1, RoundingMode.HALF_UP)
    } else null

    if (showCloseConfirm) {
        AlertDialog(
            onDismissRequest = { showCloseConfirm = false },
            title = { Text("全部清仓确认") },
            text = { Text("将卖出全部 ${oldQty} 股「$holdingName」，确定要清仓吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showCloseConfirm = false
                    val date = runCatching { LocalDate.parse(dateText) }.getOrElse {
                        Toast.makeText(context, "请选择卖出日期", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    onConfirm(oldQty, price!!, date, addToCash)
                }) { Text("确认清仓") }
            },
            dismissButton = {
                TextButton(onClick = { showCloseConfirm = false }) { Text("取消") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("减仓 — $holdingName") },
        text = {
            Column {
                Text("当前：$currentQty 股 · 成本均价 ¥$currentAvgCost",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                OutlinedTextField(qtyText, { qtyText = it },
                    label = { Text("卖出数量 (股)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(priceText, { priceText = it },
                    label = { Text("卖出价格 (元)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    readOnly = true, label = { Text("卖出日期") },
                    modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { showDatePicker = true }) { Text("选择日期") }
                if (previewRemaining != null) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("减仓后预览：", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text("剩余 ${previewRemaining} 股",
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (previewPnL != null && previewPnLPct != null && previewPnL != BigDecimal.ZERO) {
                        val pnlSign = if (previewPnL >= BigDecimal.ZERO) "+" else ""
                        val isProfit = previewPnL >= BigDecimal.ZERO
                        Text("实现盈亏 ${pnlSign}¥${previewPnL} (${pnlSign}${previewPnLPct}%)",
                            fontSize = 14.sp,
                            color = if (isProfit) com.financial.freedom.ui.theme.FinancialColors.up
                                    else com.financial.freedom.ui.theme.FinancialColors.down)
                    }
                    if (previewRemaining <= BigDecimal.ZERO) {
                        Text("将全部清仓", fontSize = 12.sp,
                            color = com.financial.freedom.ui.theme.FinancialColors.up)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("收入计入现金", modifier = Modifier.weight(1f))
                    Switch(checked = addToCash, onCheckedChange = { addToCash = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val q = qtyText.toBigDecimalOrNull()
                val p = priceText.toBigDecimalOrNull()
                if (q == null || p == null || q <= BigDecimal.ZERO) {
                    Toast.makeText(context, "卖出数量必须大于 0", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                if (q > oldQty) {
                    Toast.makeText(context, "卖出数量不能超过当前持仓", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                val date = runCatching { LocalDate.parse(dateText) }.getOrElse {
                    Toast.makeText(context, "请选择卖出日期", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                if (q >= oldQty) {
                    showCloseConfirm = true
                } else {
                    onConfirm(q, p, date, addToCash)
                }
            }) { Text("确认减仓") }
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
                    dpState.selectedDateMillis?.let { millis ->
                        dateText = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = dpState) }
    }
}

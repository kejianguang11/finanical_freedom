package com.financial.freedom.ui.credit

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.data.local.entity.Debt
import com.financial.freedom.data.local.entity.Receivable
import com.financial.freedom.ui.theme.FinancialColors
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import java.math.BigDecimal

@Composable
fun CreditScreen(viewModel: CreditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 16.dp)
    ) {
        // 应收款区域
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("应收款", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { viewModel.showAddReceivable() }) {
                    Icon(Icons.Filled.Add, contentDescription = "新增应收款")
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (state.receivables.isEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Text("暂无应收款", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(state.receivables, key = { "rec_${it.id}" }) { r ->
                ReceivableCard(r, state.displayMultiplier, onEdit = { viewModel.showEditReceivable(r) },
                    onDelete = { viewModel.deleteReceivable(r) })
                Spacer(Modifier.height(8.dp))
            }
        }

        // 应收合计
        item {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("应收合计", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("¥${state.receivablesTotal}", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = FinancialColors.up)
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))
        }

        // 负债区域
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("负债", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { viewModel.showAddDebt() }) {
                    Icon(Icons.Filled.Add, contentDescription = "新增负债")
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (state.debts.isEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Text("暂无负债", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(state.debts, key = { "debt_${it.id}" }) { d ->
                DebtCard(d, state.displayMultiplier, onEdit = { viewModel.showEditDebt(d) },
                    onDelete = { viewModel.deleteDebt(d) })
                Spacer(Modifier.height(8.dp))
            }
        }

        // 负债合计 + 净额
        item {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("负债合计", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("¥${state.debtsTotal}", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = FinancialColors.down)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("应收净额", style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold)
                val netColor = if (state.netAmount.startsWith("-")) FinancialColors.down else FinancialColors.up
                Text("¥${state.netAmount}", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = netColor)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    // 新增应收款弹窗
    if (state.showAddReceivable) {
        ReceivableDialog(
            title = "新增应收款",
            onConfirm = { name, amount, expectedDate, note ->
                viewModel.addReceivable(name, amount, expectedDate, note)
            },
            onDismiss = { viewModel.hideAddReceivable() }
        )
    }

    // 编辑应收款弹窗
    if (state.showEditReceivable && state.editingReceivable != null) {
        val r = state.editingReceivable!!
        ReceivableDialog(
            title = "编辑应收款",
            initial = r,
            onConfirm = { name, amount, expectedDate, note ->
                viewModel.updateReceivable(r.id, name, amount, expectedDate, note)
            },
            onDismiss = { viewModel.hideEditReceivable() }
        )
    }

    // 新增负债弹窗
    if (state.showAddDebt) {
        DebtDialog(
            title = "新增负债",
            onConfirm = { name, amount, interestRate, date, note ->
                viewModel.addDebt(name, amount, interestRate, date, note)
            },
            onDismiss = { viewModel.hideAddDebt() }
        )
    }

    // 编辑负债弹窗
    if (state.showEditDebt && state.editingDebt != null) {
        val d = state.editingDebt!!
        DebtDialog(
            title = "编辑负债",
            initial = d,
            onConfirm = { name, amount, interestRate, date, note ->
                viewModel.updateDebt(d.id, name, amount, interestRate, date, note)
            },
            onDismiss = { viewModel.hideEditDebt() }
        )
    }
}

@Composable
private fun ReceivableCard(r: Receivable, multiplier: BigDecimal, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(r.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text("${r.date} 借出", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (r.expectedDate != null) {
                    Text("预计 ${r.expectedDate} 归还", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (r.note.isNotBlank()) {
                    Text(r.note, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(com.financial.freedom.ui.common.FormatUtils.formatAllocationValue(r.amount, multiplier),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = FinancialColors.up)
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDelete) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun DebtCard(d: Debt, multiplier: BigDecimal, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(d.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text("${d.date}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (d.interestRate != null) {
                    Text("利率 ${(d.interestRate * BigDecimal(100)).setScale(2)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (d.note.isNotBlank()) {
                    Text(d.note, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(com.financial.freedom.ui.common.FormatUtils.formatAllocationValue(d.amount, multiplier),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = FinancialColors.down)
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDelete) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ReceivableDialog(
    title: String,
    initial: Receivable? = null,
    onConfirm: (String, BigDecimal, kotlinx.datetime.LocalDate?, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.toPlainString() ?: "") }
    var expectedDate by remember { mutableStateOf(initial?.expectedDate?.toString() ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("对方姓名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it; error = null },
                    label = { Text("金额") }, isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = expectedDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("预计归还日（选填）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showDatePicker = true }
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it },
                    label = { Text("备注（选填）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = amount.toBigDecimalOrNull()
                if (name.isBlank()) error = "请输入姓名"
                else if (value == null || value <= BigDecimal.ZERO) error = "请输入有效金额"
                else {
                    val expDate = runCatching {
                        kotlinx.datetime.LocalDate.parse(expectedDate)
                    }.getOrNull()
                    onConfirm(name, value, expDate, note)
                }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        expectedDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DebtDialog(
    title: String,
    initial: Debt? = null,
    onConfirm: (String, BigDecimal, BigDecimal?, LocalDate, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.toPlainString() ?: "") }
    var date by remember { mutableStateOf(initial?.date?.toString() ?: "") }
    var interestRate by remember { mutableStateOf(initial?.interestRate?.toPlainString() ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("来源（银行/个人）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it; error = null },
                    label = { Text("金额") }, isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("日期") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showDatePicker = true }
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = interestRate, onValueChange = { interestRate = it },
                    label = { Text("年利率（选填，如 0.0395 = 3.95%）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it },
                    label = { Text("备注（选填）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = amount.toBigDecimalOrNull()
                val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
                if (name.isBlank()) error = "请输入来源"
                else if (value == null || value <= BigDecimal.ZERO) error = "请输入有效金额"
                else if (parsedDate == null) error = "请选择日期"
                else {
                    val rate = interestRate.toBigDecimalOrNull()
                    onConfirm(name, value, rate, parsedDate, note)
                }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
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

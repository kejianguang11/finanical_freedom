package com.financial.freedom.ui.credit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.draw.clip
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

    var receivablesExpanded by remember { mutableStateOf(false) }
    var debtsExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 16.dp)
    ) {
        // 借出折叠区
        item {
            SectionHeader(
                title = "借出",
                total = state.receivablesTotal,
                count = state.receivables.count { it.status == "未还" },
                accentColor = FinancialColors.receivable,
                expanded = receivablesExpanded,
                onToggle = { receivablesExpanded = !receivablesExpanded },
                onAdd = { viewModel.showAddReceivable() }
            )
            AnimatedVisibility(
                visible = receivablesExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(4.dp))
                    if (state.receivables.isEmpty()) {
                        EmptyHint("暂无借出款")
                    } else {
                        state.receivables.forEach { r ->
                            ReceivableItem(
                                r = r,
                                multiplier = state.displayMultiplier,
                                onEdit = { viewModel.showEditReceivable(r) },
                                onMarkRepaid = { viewModel.markReceivableRepaid(r) },
                                onDelete = { viewModel.deleteReceivable(r) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // 负债折叠区
        item {
            SectionHeader(
                title = "负债",
                total = state.debtsTotal,
                count = state.debts.count { it.status == "未还" },
                accentColor = FinancialColors.debt,
                expanded = debtsExpanded,
                onToggle = { debtsExpanded = !debtsExpanded },
                onAdd = { viewModel.showAddDebt() }
            )
            AnimatedVisibility(
                visible = debtsExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(4.dp))
                    if (state.debts.isEmpty()) {
                        EmptyHint("暂无负债")
                    } else {
                        state.debts.forEach { d ->
                            DebtItem(
                                d = d,
                                multiplier = state.displayMultiplier,
                                onEdit = { viewModel.showEditDebt(d) },
                                onMarkPaid = { viewModel.markDebtPaid(d) },
                                onDelete = { viewModel.deleteDebt(d) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // 净资产底部常驻
        item {
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val label = when {
                    state.netSign > 0 -> "净借出"
                    state.netSign < 0 -> "净负债"
                    else -> "净头寸"
                }
                val color = when {
                    state.netSign > 0 -> FinancialColors.up
                    state.netSign < 0 -> FinancialColors.down
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                val prefix = when {
                    state.netSign > 0 -> "+¥"
                    state.netSign < 0 -> "-¥"
                    else -> "¥"
                }
                Text(
                    "$prefix${state.netAmount}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    // 新增借出弹窗
    if (state.showAddReceivable) {
        ReceivableDialog(
            title = "新增借出",
            onConfirm = { name, amount, date, note, deductFromCash ->
                viewModel.addReceivable(name, amount, date, note, deductFromCash)
            },
            onDismiss = { viewModel.hideAddReceivable() }
        )
    }

    // 编辑借出弹窗
    if (state.showEditReceivable && state.editingReceivable != null) {
        val r = state.editingReceivable!!
        ReceivableDialog(
            title = "编辑借出",
            initial = r,
            onConfirm = { name, amount, date, note, _ ->
                viewModel.updateReceivable(r.id, name, amount, date, note)
            },
            onDismiss = { viewModel.hideEditReceivable() }
        )
    }

    // 新增负债弹窗
    if (state.showAddDebt) {
        DebtDialog(
            title = "新增负债",
            onConfirm = { name, amount, date, note, addToCash ->
                viewModel.addDebt(name, amount, date, note, addToCash)
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
            onConfirm = { name, amount, date, note, _ ->
                viewModel.updateDebt(d.id, name, amount, date, note)
            },
            onDismiss = { viewModel.hideEditDebt() }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    total: String,
    count: Int,
    accentColor: androidx.compose.ui.graphics.Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${count}笔 · 未还",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "¥$total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onAdd, modifier = Modifier.height(24.dp).width(24.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "新增", tint = accentColor)
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReceivableItem(
    r: Receivable,
    multiplier: BigDecimal,
    onEdit: () -> Unit,
    onMarkRepaid: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val statusText = if (r.status == "已还") "已还" else "未还"
    val statusColor = if (r.status == "已还") MaterialTheme.colorScheme.onSurfaceVariant else FinancialColors.receivable

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(r.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Row {
                        Text("${r.date}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(" · $statusText", style = MaterialTheme.typography.labelSmall, color = statusColor)
                    }
                    if (r.note.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(r.note, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    com.financial.freedom.ui.common.FormatUtils.formatAllocationValue(r.amount, multiplier),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinancialColors.receivable
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (r.status != "已还") {
                    TextButton(onClick = onMarkRepaid) {
                        Text("对方已还款", style = MaterialTheme.typography.labelLarge, color = FinancialColors.receivable)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                Box {
                    TextButton(onClick = { menuExpanded = true }) {
                        Text("···", style = MaterialTheme.typography.labelLarge)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("编辑") }, onClick = { menuExpanded = false; onEdit() })
                        DropdownMenuItem(text = { Text("删除") }, onClick = { menuExpanded = false; onDelete() })
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtItem(
    d: Debt,
    multiplier: BigDecimal,
    onEdit: () -> Unit,
    onMarkPaid: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val statusText = if (d.status == "已还") "已还" else "未还"
    val statusColor = if (d.status == "已还") MaterialTheme.colorScheme.onSurfaceVariant else FinancialColors.debt

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(d.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Row {
                        Text("${d.date}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(" · $statusText", style = MaterialTheme.typography.labelSmall, color = statusColor)
                    }
                    if (d.note.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(d.note, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    com.financial.freedom.ui.common.FormatUtils.formatAllocationValue(d.amount, multiplier),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinancialColors.debt
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (d.status != "已还") {
                    TextButton(onClick = onMarkPaid) {
                        Text("我已还清", style = MaterialTheme.typography.labelLarge, color = FinancialColors.debt)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                Box {
                    TextButton(onClick = { menuExpanded = true }) {
                        Text("···", style = MaterialTheme.typography.labelLarge)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("编辑") }, onClick = { menuExpanded = false; onEdit() })
                        DropdownMenuItem(text = { Text("删除") }, onClick = { menuExpanded = false; onDelete() })
                    }
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
    onConfirm: (String, BigDecimal, LocalDate, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.toPlainString() ?: "") }
    var date by remember { mutableStateOf(initial?.date?.toString() ?: Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var deductFromCash by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val isEdit = initial != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("借款人") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
                OutlinedTextField(value = note, onValueChange = { note = it },
                    label = { Text("备注（选填）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (!isEdit) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { deductFromCash = !deductFromCash }) {
                        Checkbox(checked = deductFromCash, onCheckedChange = { deductFromCash = it })
                        Text("现金同步扣减（钱借出去了，现金减少）", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = amount.toBigDecimalOrNull()
                val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
                if (name.isBlank()) error = "请输入借款人"
                else if (value == null || value <= BigDecimal.ZERO) error = "请输入有效金额"
                else if (parsedDate == null) error = "请选择日期"
                else onConfirm(name, value, parsedDate, note, deductFromCash)
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DebtDialog(
    title: String,
    initial: Debt? = null,
    onConfirm: (String, BigDecimal, LocalDate, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.toPlainString() ?: "") }
    var date by remember { mutableStateOf(initial?.date?.toString() ?: Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var addToCash by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val isEdit = initial != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("债权人") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
                OutlinedTextField(value = note, onValueChange = { note = it },
                    label = { Text("备注（选填）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (!isEdit) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { addToCash = !addToCash }) {
                        Checkbox(checked = addToCash, onCheckedChange = { addToCash = it })
                        Text("现金同步到账（借到钱了，现金增加）", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = amount.toBigDecimalOrNull()
                val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
                if (name.isBlank()) error = "请输入债权人"
                else if (value == null || value <= BigDecimal.ZERO) error = "请输入有效金额"
                else if (parsedDate == null) error = "请选择日期"
                else onConfirm(name, value, parsedDate, note, addToCash)
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

package com.financial.freedom.ui.holdings.deposit

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.data.local.entity.Deposit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

val currencies = listOf("CNY", "USD", "JPY", "HKD", "EUR", "GBP")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDepositScreen(
    onSaved: () -> Unit = {},
    viewModel: AddDepositViewModel = hiltViewModel()
) {
    var bank by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("CNY") }
    var principal by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf("") }
    var maturityDate by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var deductFromCash by rememberSaveable { mutableStateOf(false) }
    var currencyExpanded by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新增存款") },
                navigationIcon = {
                    IconButton(onClick = onSaved) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            OutlinedTextField(bank, { bank = it }, label = { Text("银行/平台") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = it }) {
                OutlinedTextField(
                    value = currency, onValueChange = {}, readOnly = true,
                    label = { Text("币种") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                    currencies.forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = { currency = c; currencyExpanded = false })
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(principal, { principal = it }, label = { Text("本金") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(rate, { rate = it }, label = { Text("年化利率 (如 2.75)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            // 存入日期 - 日期选择器
            var showStartDatePicker by rememberSaveable { mutableStateOf(false) }
            val startDatePickerState = rememberDatePickerState()
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("存入日期") },
                    modifier = Modifier.fillMaxWidth()
                )
                // 透明 overlay 确保点击事件不被 OutlinedTextField 内部拦截
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showStartDatePicker = true }
                )
            }
            if (showStartDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            startDatePickerState.selectedDateMillis?.let { millis ->
                                val instant = Instant.fromEpochMilliseconds(millis)
                                startDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                            }
                            showStartDatePicker = false
                        }) { Text("确定") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartDatePicker = false }) { Text("取消") }
                    }
                ) {
                    DatePicker(state = startDatePickerState)
                }
            }

            Spacer(Modifier.height(12.dp))
            // 到期日期 - 日期选择器
            var showMaturityDatePicker by rememberSaveable { mutableStateOf(false) }
            val maturityDatePickerState = rememberDatePickerState()
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = maturityDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("到期日期") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showMaturityDatePicker = true }
                )
            }
            if (showMaturityDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showMaturityDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            maturityDatePickerState.selectedDateMillis?.let { millis ->
                                val instant = Instant.fromEpochMilliseconds(millis)
                                maturityDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                            }
                            showMaturityDatePicker = false
                        }) { Text("确定") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMaturityDatePicker = false }) { Text("取消") }
                    }
                ) {
                    DatePicker(state = maturityDatePickerState)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(note, { note = it }, label = { Text("备注 (选填)") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("从现金中扣除", modifier = Modifier.weight(1f))
                Switch(checked = deductFromCash, onCheckedChange = { deductFromCash = it })
            }

            Spacer(Modifier.height(24.dp))
            val isSaving by viewModel.isSaving.collectAsState()

            Button(
                onClick = {
                    if (bank.isBlank() || principal.isBlank() || rate.isBlank() || startDate.isBlank()) {
                        Toast.makeText(context, "请填写必填字段", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.save(
                        Deposit(
                            name = bank, bank = bank, currency = currency,
                            principal = BigDecimal(principal),
                            interestRate = BigDecimal(rate).divide(BigDecimal(100), 6, java.math.RoundingMode.HALF_UP),
                            startDate = LocalDate.parse(startDate),
                            maturityDate = LocalDate.parse(maturityDate),
                            note = note
                        ),
                        deductFromCash = deductFromCash,
                        onSuccess = { savedAmount, totalAssets ->
                            Toast.makeText(context, "已存入 ¥$savedAmount · 总资产 ¥$totalAssets", Toast.LENGTH_SHORT).show()
                            onSaved()
                        },
                        onError = { msg ->
                            Toast.makeText(context, "保存失败: $msg", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isSaving) "保存中..." else "保存") }
        }
    }
}

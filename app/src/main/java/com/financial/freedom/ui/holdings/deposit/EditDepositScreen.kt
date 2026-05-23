package com.financial.freedom.ui.holdings.deposit

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.data.local.dao.DepositDao
import com.financial.freedom.data.local.entity.Deposit
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditDepositViewModel @Inject constructor(
    private val depositDao: DepositDao,
    private val backfillEngine: BackfillEngine,
    private val accountManager: AccountManager
) : ViewModel() {
    var deposit by mutableStateOf<Deposit?>(null)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            deposit = depositDao.getById(id, accountId)
        }
    }

    fun update(deposit: Deposit, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                val oldDeposit = this@EditDepositViewModel.deposit
                depositDao.update(deposit)
                val fromDate = if (oldDeposit != null && oldDeposit.startDate < deposit.startDate) {
                    oldDeposit.startDate
                } else {
                    deposit.startDate
                }
                onSuccess()
                viewModelScope.launch(Dispatchers.IO) {
                    backfillEngine.markDirtyAndBackfill(fromDate, deposit.accountId)
                }
            } catch (e: Exception) {
                onError(e.message ?: "未知错误")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun delete(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                val d = deposit ?: return@launch
                depositDao.delete(d)
                onSuccess()
                viewModelScope.launch(Dispatchers.IO) {
                    backfillEngine.markDirtyAndBackfill(d.startDate, d.accountId)
                }
            } catch (e: Exception) {
                onError(e.message ?: "未知错误")
            } finally {
                _isSaving.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDepositScreen(
    depositId: Long,
    onSaved: () -> Unit = {},
    viewModel: EditDepositViewModel = hiltViewModel()
) {
    val d = viewModel.deposit

    var bank by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("CNY") }
    var principal by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf("") }
    var maturityDate by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var currencyExpanded by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(depositId) { viewModel.load(depositId) }

    // 首次加载时预填表单
    LaunchedEffect(d) {
        if (d != null && !loaded) {
            bank = d!!.bank
            currency = d!!.currency
            principal = d!!.principal.toPlainString()
            rate = d!!.interestRate.multiply(BigDecimal(100)).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            startDate = d!!.startDate.toString()
            maturityDate = d!!.maturityDate.toString()
            note = d!!.note
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑存款") },
                navigationIcon = {
                    IconButton(onClick = onSaved) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (d == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("加载中...")
            }
            return@Scaffold
        }

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

            Spacer(Modifier.height(24.dp))
            val isSaving by viewModel.isSaving.collectAsState()

            Button(
                onClick = {
                    val dep = d ?: return@Button
                    viewModel.update(
                        dep.copy(
                            name = bank, bank = bank, currency = currency,
                            principal = BigDecimal(principal),
                            interestRate = BigDecimal(rate).divide(BigDecimal(100), 6, java.math.RoundingMode.HALF_UP),
                            startDate = LocalDate.parse(startDate),
                            maturityDate = LocalDate.parse(maturityDate),
                            note = note
                        ),
                        onSuccess = {
                            Toast.makeText(context, "存款已更新", Toast.LENGTH_SHORT).show()
                            onSaved()
                        },
                        onError = { msg ->
                            Toast.makeText(context, "更新失败: $msg", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isSaving) "保存中..." else "保存修改") }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = com.financial.freedom.ui.theme.FinancialColors.up
                )
            ) { Text("删除存款") }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("删除「${bank.ifBlank { "存款" }}」后将无法恢复，确定删除吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(
                        onSuccess = {
                            Toast.makeText(context, "存款已删除", Toast.LENGTH_SHORT).show()
                            showDeleteConfirm = false
                            onSaved()
                        },
                        onError = { msg ->
                            Toast.makeText(context, "删除失败: $msg", Toast.LENGTH_SHORT).show()
                            showDeleteConfirm = false
                        }
                    )
                }) { Text("删除", color = com.financial.freedom.ui.theme.FinancialColors.up) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

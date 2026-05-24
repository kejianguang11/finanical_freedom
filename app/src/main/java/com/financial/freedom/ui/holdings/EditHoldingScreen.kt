package com.financial.freedom.ui.holdings

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financial.freedom.data.local.dao.HoldingDao
import com.financial.freedom.data.local.entity.Holding
import com.financial.freedom.domain.account.AccountManager
import com.financial.freedom.domain.calculator.BackfillEngine
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

@HiltViewModel
class EditHoldingViewModel @Inject constructor(
    private val holdingDao: HoldingDao,
    private val backfillEngine: BackfillEngine,
    private val accountManager: AccountManager
) : ViewModel() {
    var holding by mutableStateOf<Holding?>(null)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            val accountId = accountManager.currentAccountId.value ?: return@launch
            holding = holdingDao.getById(id, accountId)
        }
    }

    fun update(holding: Holding, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                val oldHolding = this@EditHoldingViewModel.holding
                holdingDao.update(holding)
                val fromDate = if (oldHolding != null && oldHolding.costDate < holding.costDate) {
                    oldHolding.costDate
                } else {
                    holding.costDate
                }
                onSuccess()
                viewModelScope.launch(Dispatchers.IO) {
                    backfillEngine.markDirtyAndBackfill(fromDate, holding.accountId)
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
fun EditHoldingScreen(
    holdingId: Long,
    onSaved: () -> Unit = {},
    viewModel: EditHoldingViewModel = hiltViewModel()
) {
    val h = viewModel.holding

    var symbol by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var market by rememberSaveable { mutableStateOf("CN") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var costPrice by rememberSaveable { mutableStateOf("") }
    var goldUnitPrice by rememberSaveable { mutableStateOf("") }
    var costDate by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val type = h?.type ?: "STOCK"
    val typeLabel = when (type) { "STOCK" -> "股票"; "FUND" -> "基金"; "GOLD" -> "黄金"; else -> "资产" }

    LaunchedEffect(holdingId) { viewModel.load(holdingId) }

    LaunchedEffect(h) {
        if (h != null && !loaded) {
            symbol = h!!.symbol
            name = h!!.name
            market = h!!.market
            quantity = h!!.quantity.toPlainString()
            if (h!!.type == "GOLD") {
                goldUnitPrice = h!!.costPrice.toPlainString()
            } else {
                costPrice = h!!.costPrice.toPlainString()
            }
            costDate = h!!.costDate.toString()
            note = h!!.note
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑$typeLabel") },
                navigationIcon = {
                    IconButton(onClick = onSaved) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (h == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("加载中...")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            if (type != "GOLD") {
                OutlinedTextField(symbol, { symbol = it }, label = { Text("代码") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(name, { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            if (type == "STOCK") {
                OutlinedTextField(market, { market = it }, label = { Text("市场 (CN/US/HK)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            if (type == "GOLD") {
                // 黄金：克数 + 单价
                OutlinedTextField(quantity, { quantity = it }, label = { Text("克数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(goldUnitPrice, { goldUnitPrice = it }, label = { Text("单价 (元/克)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                // 自动计算买入总价
                if (quantity.isNotBlank() && goldUnitPrice.isNotBlank()) {
                    val grams = quantity.toBigDecimalOrNull()
                    val unitPrice = goldUnitPrice.toBigDecimalOrNull()
                    if (grams != null && unitPrice != null && grams > BigDecimal.ZERO && unitPrice > BigDecimal.ZERO) {
                        val totalPrice = grams.multiply(unitPrice).setScale(2, java.math.RoundingMode.HALF_UP)
                        Spacer(Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                "买入总价：¥${totalPrice.toPlainString()}",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                val qtyLabel = when (type) { "FUND" -> "持有份额"; else -> "持有数量 (股)" }
                OutlinedTextField(quantity, { quantity = it }, label = { Text(qtyLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))

                val priceLabel = when (type) { "FUND" -> "成本净值"; else -> "成本价" }
                OutlinedTextField(costPrice, { costPrice = it }, label = { Text(priceLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(12.dp))
            // 买入日期 - 日期选择器
            var showDatePicker by rememberSaveable { mutableStateOf(false) }
            val datePickerState = rememberDatePickerState()
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = costDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("买入日期") },
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
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val instant = Instant.fromEpochMilliseconds(millis)
                                costDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
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
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(note, { note = it }, label = { Text("备注 (选填)") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(24.dp))
            val isSaving by viewModel.isSaving.collectAsState()

            Button(
                onClick = {
                    val holding = h ?: return@Button
                    val updated = if (holding.type == "GOLD") {
                        val grams = BigDecimal(quantity)
                        val unitPrice = BigDecimal(goldUnitPrice)
                        holding.copy(
                            symbol = "XAU", name = "黄金", market = "",
                            quantity = grams,
                            costPrice = unitPrice,
                            costDate = LocalDate.parse(costDate),
                            note = note
                        )
                    } else {
                        holding.copy(
                            symbol = symbol, name = name, market = market,
                            quantity = BigDecimal(quantity),
                            costPrice = BigDecimal(costPrice),
                            costDate = LocalDate.parse(costDate),
                            note = note
                        )
                    }
                    viewModel.update(
                        updated,
                        onSuccess = {
                            Toast.makeText(context, "$typeLabel 已更新", Toast.LENGTH_SHORT).show()
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
        }
    }
}

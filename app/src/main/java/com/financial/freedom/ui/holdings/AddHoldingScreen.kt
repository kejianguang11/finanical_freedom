package com.financial.freedom.ui.holdings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.data.local.entity.Holding
import com.financial.freedom.data.remote.SearchResult
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHoldingScreen(
    type: String,
    onSaved: () -> Unit = {},
    viewModel: AddHoldingViewModel = hiltViewModel()
) {
    val typeLabel = when (type) { "STOCK" -> "股票"; "FUND" -> "基金"; "GOLD" -> "黄金"; else -> "资产" }

    var symbol by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var market by rememberSaveable { mutableStateOf("CN") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var costPrice by rememberSaveable { mutableStateOf("") }
    var goldUnitPrice by rememberSaveable { mutableStateOf("") }  // 黄金单价（元/克）
    var costDate by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var deductFromCash by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新增$typeLabel") },
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
            // 代码输入框（股票/基金支持自动搜索，黄金隐藏）
            if (type != "GOLD") {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { value ->
                            symbol = value
                            viewModel.onSymbolChanged(value)
                        },
                        label = { Text("代码") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.width(20.dp).height(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    )
                    // 搜索结果下拉
                    if (searchResults.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Column {
                                searchResults.take(8).forEachIndexed { index, result ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                symbol = result.symbol
                                                name = result.name
                                                if (result.market.isNotBlank()) market = result.market
                                                viewModel.clearSearchResults()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(result.symbol, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                result.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            if (result.market == "CN") "A股" else if (result.market == "US") "美股" else if (result.market == "HK") "港股" else result.market,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (index < searchResults.take(8).lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(name, { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            when (type) {
                "GOLD" -> {
                    // 黄金：只需克数 + 单价 + 日期，自动算买入总价
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
                }
                "FUND" -> {
                    OutlinedTextField(quantity, { quantity = it }, label = { Text("持有份额") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(costPrice, { costPrice = it }, label = { Text("成本净值") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                }
                else -> {
                    OutlinedTextField(market, { market = it }, label = { Text("市场 (CN/US/HK)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(quantity, { quantity = it }, label = { Text("持有数量 (股)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(costPrice, { costPrice = it }, label = { Text("成本价") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                }
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
                                val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
                                costDate = localDate.toString()
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
                    val isValid = when (type) {
                        "GOLD" -> quantity.isNotBlank() && goldUnitPrice.isNotBlank() && costDate.isNotBlank()
                        else -> symbol.isNotBlank() && name.isNotBlank() && quantity.isNotBlank() && costPrice.isNotBlank()
                    }
                    if (!isValid) {
                        Toast.makeText(context, "请填写必填字段", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val holding = when (type) {
                        "GOLD" -> {
                            val grams = BigDecimal(quantity)
                            val unitPrice = BigDecimal(goldUnitPrice)
                            Holding(
                                type = "GOLD", symbol = "XAU", name = "黄金", market = "",
                                currency = "CNY",
                                quantity = grams,
                                costPrice = grams.multiply(unitPrice),
                                costDate = LocalDate.parse(costDate),
                                note = note
                            )
                        }
                        else -> Holding(
                            type = type, symbol = symbol, name = name, market = market,
                            currency = if (market == "US") "USD" else if (market == "HK") "HKD" else "CNY",
                            quantity = BigDecimal(quantity),
                            costPrice = BigDecimal(costPrice),
                            costDate = LocalDate.parse(costDate),
                            note = note
                        )
                    }
                    viewModel.save(
                        holding,
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

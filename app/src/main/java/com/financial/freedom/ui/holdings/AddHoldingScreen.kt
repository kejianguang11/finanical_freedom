package com.financial.freedom.ui.holdings

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.financial.freedom.data.local.entity.Holding
import com.financial.freedom.ui.components.SearchSheet
import kotlinx.datetime.LocalDate
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
    var costDate by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current
    var showSearch by rememberSaveable { mutableStateOf(false) }

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
            OutlinedTextField(symbol, { symbol = it }, label = { Text("代码") },
                modifier = Modifier.fillMaxWidth(), trailingIcon = {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                })
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(name, { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            when (type) {
                "GOLD" -> {
                    OutlinedTextField(quantity, { quantity = it }, label = { Text("重量 (克)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(costPrice, { costPrice = it }, label = { Text("买入总价 (¥)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
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
            OutlinedTextField(costDate, { costDate = it }, label = { Text("买入日期 (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(note, { note = it }, label = { Text("备注 (选填)") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (symbol.isBlank() || name.isBlank() || quantity.isBlank() || costPrice.isBlank()) {
                        Toast.makeText(context, "请填写必填字段", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.save(
                        Holding(
                            type = type, symbol = symbol, name = name, market = market,
                            currency = if (market == "US") "USD" else if (market == "HK") "HKD" else "CNY",
                            quantity = BigDecimal(quantity),
                            costPrice = when (type) {
                                "GOLD" -> BigDecimal(costPrice).divide(BigDecimal(quantity), 4, java.math.RoundingMode.HALF_UP)
                                else -> BigDecimal(costPrice)
                            },
                            costDate = LocalDate.parse(costDate),
                            note = note
                        ),
                        onSuccess = {
                            Toast.makeText(context, "$typeLabel 已保存", Toast.LENGTH_SHORT).show()
                            onSaved()
                        },
                        onError = { msg ->
                            Toast.makeText(context, "保存失败: $msg", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存") }
        }
    }

    if (showSearch) {
        ModalBottomSheet(
            onDismissRequest = { showSearch = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            SearchSheet(
                title = "搜索$typeLabel",
                placeholder = "输入代码或名称",
                onSearch = { query -> viewModel.search(query) },
                onSelect = { result ->
                    symbol = result.symbol
                    name = result.name
                    market = result.market
                    showSearch = false
                }
            )
        }
    }
}
